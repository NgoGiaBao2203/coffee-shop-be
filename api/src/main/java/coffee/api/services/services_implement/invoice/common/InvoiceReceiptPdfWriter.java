package coffee.api.services.services_implement.invoice.common;

import coffee.api.dto.result.InvoiceReceiptResult;
import coffee.api.dto.result.InvoiceResult.InvoiceItemResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

/**
 * Renders a paid invoice as a PDF receipt for 80mm thermal paper: one page, 80mm wide, height grown
 * to fit the content. Uses an embedded DejaVu font so Vietnamese text prints correctly.
 */
@Component
public class InvoiceReceiptPdfWriter {

  /** 80mm in PDF points (1 inch = 25.4mm = 72pt). */
  static final float PAGE_WIDTH = 80f / 25.4f * 72f;

  private static final float MARGIN = 10f;
  private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
  private static final float COLUMN_GAP = 6f;
  private static final float INDENT = 8f;
  private static final float LEADING = 1.4f;
  private static final float SEPARATOR_HEIGHT = 7f;
  private static final float BODY_SIZE = 8f;
  private static final float NOTE_SIZE = 7.5f;
  private static final float HEADER_SIZE = 10f;
  private static final float TITLE_SIZE = 11f;
  private static final String REGULAR_FONT = "/fonts/DejaVuSansCondensed.ttf";
  private static final String BOLD_FONT = "/fonts/DejaVuSansCondensed-Bold.ttf";
  private static final String EMPTY = "-";
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private enum Align {
    LEFT,
    CENTER,
    RIGHT
  }

  /** One printed line: a separator, a single text, or a left text with a right-aligned value. */
  private record Row(
      String left, String right, PDFont font, float size, Align align, float indent) {
    static Row separator() {
      return new Row(null, null, null, 0, Align.LEFT, 0);
    }

    boolean isSeparator() {
      return font == null;
    }

    float height() {
      return isSeparator() ? SEPARATOR_HEIGHT : size * LEADING;
    }
  }

  private final String regularFontPath;
  private final String boldFontPath;

  public InvoiceReceiptPdfWriter() {
    this(REGULAR_FONT, BOLD_FONT);
  }

  /** Font paths are classpath resources; overridable for tests. */
  InvoiceReceiptPdfWriter(String regularFontPath, String boldFontPath) {
    this.regularFontPath = regularFontPath;
    this.boldFontPath = boldFontPath;
  }

  public byte[] write(InvoiceReceiptResult receipt) {
    try (PDDocument document = new PDDocument()) {
      PDFont regular = loadFont(document, regularFontPath);
      PDFont bold = loadFont(document, boldFontPath);
      List<Row> rows = buildRows(receipt, regular, bold);

      float pageHeight = 2 * MARGIN;
      for (Row row : rows) {
        pageHeight += row.height();
      }
      PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, pageHeight));
      document.addPage(page);

      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        float top = pageHeight - MARGIN;
        for (Row row : rows) {
          top -= row.height();
          draw(stream, row, top);
        }
      }

      ByteArrayOutputStream output = new ByteArrayOutputStream();
      document.save(output);
      return output.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot render invoice receipt", e);
    }
  }

  private List<Row> buildRows(InvoiceReceiptResult receipt, PDFont regular, PDFont bold)
      throws IOException {
    List<Row> rows = new ArrayList<>();

    // Shop header
    addCentered(rows, receipt.getShopName(), bold, HEADER_SIZE);
    addCentered(rows, receipt.getShopAddress(), regular, BODY_SIZE);
    if (hasText(receipt.getShopPhone())) {
      addCentered(rows, "ĐT: " + receipt.getShopPhone(), regular, BODY_SIZE);
    }
    rows.add(Row.separator());

    // Invoice information
    addCentered(rows, "HÓA ĐƠN THANH TOÁN", bold, TITLE_SIZE);
    addPair(rows, "Mã HĐ:", receipt.getInvoiceCode(), regular, BODY_SIZE, 0);
    addPair(
        rows,
        "Ngày:",
        receipt.getCreatedAt() == null ? EMPTY : receipt.getCreatedAt().format(DATE_TIME),
        regular,
        BODY_SIZE,
        0);
    addPair(
        rows,
        "Bàn:",
        receipt.getTableNumber() == null ? EMPTY : String.valueOf(receipt.getTableNumber()),
        regular,
        BODY_SIZE,
        0);
    addPair(rows, "Nhân viên:", textOrEmpty(receipt.getStaffName()), regular, BODY_SIZE, 0);
    rows.add(Row.separator());

    // Drink lines
    addPair(rows, "Món", "Thành tiền", bold, BODY_SIZE, 0);
    List<InvoiceItemResult> items = receipt.getItems() == null ? List.of() : receipt.getItems();
    for (InvoiceItemResult item : items) {
      String name = textOrEmpty(item.getDrinkName());
      if (hasText(item.getSize())) {
        name += " (" + item.getSize() + ")";
      }
      addLeft(rows, name, bold, BODY_SIZE, 0);
      addPair(
          rows,
          item.getQuantity() + " x " + money(item.getUnitPrice()),
          money(item.getItemTotal()),
          regular,
          BODY_SIZE,
          INDENT);
      if (hasText(item.getNote())) {
        addLeft(rows, "Ghi chú: " + item.getNote(), regular, NOTE_SIZE, INDENT);
      }
    }
    rows.add(Row.separator());

    // Total and payment status
    addPair(rows, "TỔNG CỘNG", money(receipt.getTotalAmount()) + " đ", bold, HEADER_SIZE, 0);
    rows.add(Row.separator());
    addCentered(rows, statusLabel(receipt.getStatus()), bold, BODY_SIZE + 1);
    addCentered(rows, "Cảm ơn quý khách, hẹn gặp lại!", regular, BODY_SIZE);
    return rows;
  }

  private void addCentered(List<Row> rows, String text, PDFont font, float size)
      throws IOException {
    if (!hasText(text)) {
      return;
    }
    for (String line : wrap(clean(text, font), font, size, CONTENT_WIDTH)) {
      rows.add(new Row(line, null, font, size, Align.CENTER, 0));
    }
  }

  private void addLeft(List<Row> rows, String text, PDFont font, float size, float indent)
      throws IOException {
    for (String line : wrap(clean(text, font), font, size, CONTENT_WIDTH - indent)) {
      rows.add(new Row(line, null, font, size, Align.LEFT, indent));
    }
  }

  /**
   * Label and right-aligned value on one line when both fit; otherwise the label on its own line(s)
   * and the value right-aligned on the line(s) below.
   */
  private void addPair(
      List<Row> rows, String left, String right, PDFont font, float size, float indent)
      throws IOException {
    String label = clean(left, font);
    String value = clean(textOrEmpty(right), font);
    float lineWidth = CONTENT_WIDTH - indent;
    if (width(label, font, size) + COLUMN_GAP + width(value, font, size) <= lineWidth) {
      rows.add(new Row(label, value, font, size, Align.LEFT, indent));
      return;
    }
    addLeft(rows, label, font, size, indent);
    for (String line : wrap(value, font, size, lineWidth)) {
      rows.add(new Row(line, null, font, size, Align.RIGHT, 0));
    }
  }

  private void draw(PDPageContentStream stream, Row row, float bottom) throws IOException {
    if (row.isSeparator()) {
      float y = bottom + SEPARATOR_HEIGHT / 2;
      stream.setLineDashPattern(new float[] {2f, 2f}, 0);
      stream.setLineWidth(0.5f);
      stream.moveTo(MARGIN, y);
      stream.lineTo(PAGE_WIDTH - MARGIN, y);
      stream.stroke();
      return;
    }
    float baseline = bottom + row.size() * (LEADING - 1) / 2 + row.size() * 0.2f;
    float textWidth = width(row.left(), row.font(), row.size());
    float leftX =
        switch (row.align()) {
          case CENTER -> (PAGE_WIDTH - textWidth) / 2;
          case RIGHT -> PAGE_WIDTH - MARGIN - textWidth;
          case LEFT -> MARGIN + row.indent();
        };
    showText(stream, row.left(), row.font(), row.size(), leftX, baseline);
    if (row.right() != null) {
      float rightX = PAGE_WIDTH - MARGIN - width(row.right(), row.font(), row.size());
      showText(stream, row.right(), row.font(), row.size(), rightX, baseline);
    }
  }

  private void showText(
      PDPageContentStream stream, String text, PDFont font, float size, float x, float y)
      throws IOException {
    stream.beginText();
    stream.setFont(font, size);
    stream.newLineAtOffset(x, y);
    stream.showText(text);
    stream.endText();
  }

  /** Greedy word wrap; a single word wider than the line is split by characters. */
  private List<String> wrap(String text, PDFont font, float size, float maxWidth)
      throws IOException {
    List<String> lines = new ArrayList<>();
    StringBuilder line = new StringBuilder();
    for (String word : text.split(" ")) {
      String candidate = line.isEmpty() ? word : line + " " + word;
      if (width(candidate, font, size) <= maxWidth) {
        line.setLength(0);
        line.append(candidate);
        continue;
      }
      if (!line.isEmpty()) {
        lines.add(line.toString());
        line.setLength(0);
      }
      for (char c : word.toCharArray()) {
        if (!line.isEmpty() && width(line.toString() + c, font, size) > maxWidth) {
          lines.add(line.toString());
          line.setLength(0);
        }
        line.append(c);
      }
    }
    lines.add(line.toString());
    return lines;
  }

  /** Collapse whitespace and replace characters the font cannot draw (emoji, controls) by '?'. */
  private String clean(String text, PDFont font) {
    StringBuilder result = new StringBuilder();
    text.replaceAll("\\s+", " ")
        .trim()
        .codePoints()
        .forEach(
            cp -> {
              String ch = new String(Character.toChars(cp));
              result.append(canEncode(font, ch) ? ch : "?");
            });
    return result.toString();
  }

  private boolean canEncode(PDFont font, String ch) {
    try {
      font.encode(ch);
      return true;
    } catch (IOException | IllegalArgumentException e) {
      return false;
    }
  }

  private float width(String text, PDFont font, float size) throws IOException {
    return font.getStringWidth(text) / 1000 * size;
  }

  private PDFont loadFont(PDDocument document, String path) throws IOException {
    try (InputStream font = InvoiceReceiptPdfWriter.class.getResourceAsStream(path)) {
      if (font == null) {
        throw new IOException("Font not found: " + path);
      }
      return PDType0Font.load(document, font);
    }
  }

  private String statusLabel(Integer status) {
    if (status == null) {
      return EMPTY;
    }
    return switch (status) {
      case 0 -> "CHƯA THANH TOÁN";
      case 1 -> "ĐÃ THANH TOÁN";
      case 2 -> "ĐÃ HỦY";
      default -> EMPTY;
    };
  }

  /** Vietnamese money format: 1.250.000 (dot grouping, no decimals). */
  private String money(BigDecimal amount) {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    symbols.setGroupingSeparator('.');
    return new DecimalFormat("#,##0", symbols).format(amount == null ? BigDecimal.ZERO : amount);
  }

  private boolean hasText(String text) {
    return text != null && !text.isBlank();
  }

  private String textOrEmpty(String text) {
    return hasText(text) ? text : EMPTY;
  }
}
