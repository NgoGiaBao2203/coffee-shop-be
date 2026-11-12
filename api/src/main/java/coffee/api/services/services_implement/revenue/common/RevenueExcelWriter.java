package coffee.api.services.services_implement.revenue.common;

import coffee.api.dto.result.RevenueResult;
import coffee.api.dto.result.RevenueResult.RevenueItemResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Build the revenue report workbook in a report layout (unit and branch name, centred title,
 * information block, bordered table, note and signature):
 *
 * <ul>
 *   <li>Tổng hợp: filters, totals and revenue per day or per month;
 *   <li>Hóa đơn: one row per paid invoice;
 *   <li>Chi tiết món: one row per drink, the invoice cells merged over its drinks.
 * </ul>
 *
 * Totals and period figures are Excel formulas over the invoice sheet, so the file recalculates
 * when it is edited; cached values are computed before saving.
 */
@Component
public class RevenueExcelWriter {

  public static final String SHEET_SUMMARY = "Tổng hợp";
  public static final String SHEET_INVOICES = "Hóa đơn";
  public static final String SHEET_ITEMS = "Chi tiết món";

  public static final String ORGANIZATION = "HOA YÊN COFFEE";

  // Summary sheet layout (0-based rows), shared with the tests
  public static final int SUMMARY_TITLE_ROW = 3;
  public static final int SUMMARY_PERIOD_ROW = 4;
  public static final int SUMMARY_SHOP_ROW = 6;
  public static final int SUMMARY_SEARCH_ROW = 7;
  public static final int SUMMARY_COUNT_ROW = 8;
  public static final int SUMMARY_TOTAL_ROW = 9;
  public static final int SUMMARY_EXPORTED_AT_ROW = 10;
  public static final int SUMMARY_HEADER_ROW = 12;

  /** Info values and table figures start in this column (C). */
  public static final int SUMMARY_VALUE_COLUMN = 2;

  private static final int SUMMARY_LAST_COLUMN = 4;

  /** Ranges up to this many days are summarised per day; longer ranges per month. */
  private static final int MAX_DAYS_PER_DAY_SUMMARY = 31;

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private static final String INVOICE_REF = "'" + SHEET_INVOICES + "'!";
  private static final String NO_VALUE = "—";
  private static final String TOTAL_LABEL = "Tổng cộng";
  private static final String INVOICE_CODE = "Mã hóa đơn";
  private static final String SHOP = "Chi nhánh";
  private static final String CREATED_AT = "Thời gian";
  private static final String REVENUE_VND = "Doanh thu (VNĐ)";

  /**
   * Report filters and the person who exported it.
   *
   * @param shopName null means all shops
   * @param preparedBy full name for the signature block, may be null
   */
  public record ReportInfo(
      LocalDate startDate, LocalDate endDate, String shopName, String search, String preparedBy) {}

  /** Create the .xlsx file. */
  public byte[] write(List<RevenueResult> invoices, ReportInfo info) {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Styles styles = new Styles(workbook);
      // Create the summary first so it is the first tab; its formulas reference the invoice sheet
      Sheet summary = workbook.createSheet(SHEET_SUMMARY);
      writeInvoiceSheet(workbook.createSheet(SHEET_INVOICES), styles, invoices);
      writeItemSheet(workbook.createSheet(SHEET_ITEMS), styles, invoices);
      writeSummarySheet(summary, styles, invoices, info);

      XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
      workbook.setForceFormulaRecalculation(true);
      return serialize(workbook);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create the revenue report file", e);
    }
  }

  /** Workbook as .xlsx bytes; a separate method so tests can simulate a write failure. */
  protected byte[] serialize(XSSFWorkbook workbook) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    workbook.write(out);
    return out.toByteArray();
  }

  // ── Sheet 1: summary (report layout, gridlines shown, nothing frozen) ──

  private void writeSummarySheet(
      Sheet sheet, Styles styles, List<RevenueResult> invoices, ReportInfo info) {
    setColumnWidths(sheet, 7, 24, 16, 22, 14);

    // Unit name and branch (top left)
    mergedText(sheet, sheet.createRow(0), 0, 1, ORGANIZATION, styles.headingBold);
    String branch = textOr(info.shopName(), "Tất cả chi nhánh").toUpperCase(Locale.ROOT);
    mergedText(sheet, sheet.createRow(1), 0, 1, branch, styles.heading);

    Row titleRow = sheet.createRow(SUMMARY_TITLE_ROW);
    titleRow.setHeightInPoints(26);
    mergedText(sheet, titleRow, 0, SUMMARY_LAST_COLUMN, "BÁO CÁO DOANH THU", styles.title);
    mergedText(
        sheet,
        sheet.createRow(SUMMARY_PERIOD_ROW),
        0,
        SUMMARY_LAST_COLUMN,
        "Kỳ báo cáo: " + formatPeriod(info),
        styles.italicCenter);

    boolean perDay = isPerDaySummary(info.startDate(), info.endDate());
    List<LocalDate> periods = listPeriods(invoices, info.startDate(), info.endDate(), perDay);
    int firstPeriodRow = SUMMARY_HEADER_ROW + 1;
    int totalRow = firstPeriodRow + periods.size();
    String totalRevenue = "D" + (totalRow + 1);

    writeInfoText(sheet, styles, SUMMARY_SHOP_ROW, "Chi nhánh:", textOr(info.shopName(), "Tất cả"));
    writeInfoText(
        sheet, styles, SUMMARY_SEARCH_ROW, "Từ khóa tìm kiếm:", textOr(info.search(), NO_VALUE));
    writeInfoFormula(
        sheet, styles, SUMMARY_COUNT_ROW, "Tổng số hóa đơn:", "C" + (totalRow + 1), styles.infoInt);
    writeInfoFormula(
        sheet, styles, SUMMARY_TOTAL_ROW, "Tổng doanh thu (VNĐ):", totalRevenue, styles.infoMoney);
    writeInfoText(
        sheet,
        styles,
        SUMMARY_EXPORTED_AT_ROW,
        "Thời gian xuất:",
        LocalDateTime.now().format(DATE_TIME));

    writeHeader(
        sheet,
        styles,
        SUMMARY_HEADER_ROW,
        "STT",
        perDay ? "Ngày" : "Tháng",
        "Số hóa đơn",
        REVENUE_VND,
        "Tỷ trọng");
    for (int i = 0; i < periods.size(); i++) {
      writePeriodRow(
          sheet, styles, firstPeriodRow + i, periods.get(i), perDay, invoices, totalRevenue);
    }
    writeSummaryTotalRow(sheet, styles, totalRow, invoices, totalRevenue);

    writeSignature(sheet, styles, totalRow + 2, info.preparedBy());
    setupPrint(sheet, false, -1);
  }

  private void writePeriodRow(
      Sheet sheet,
      Styles styles,
      int rowIndex,
      LocalDate period,
      boolean perDay,
      List<RevenueResult> invoices,
      String totalRevenue) {
    Row row = sheet.createRow(rowIndex);
    setNumber(row, 0, rowIndex - SUMMARY_HEADER_ROW, styles.cell(Kind.CENTER_INT, false));
    Cell periodCell = row.createCell(1);
    periodCell.setCellValue(period);
    periodCell.setCellStyle(styles.cell(perDay ? Kind.DAY : Kind.MONTH, false));

    String ref = "B" + (rowIndex + 1);
    String next = perDay ? "(" + ref + "+1)" : "DATE(YEAR(" + ref + "),MONTH(" + ref + ")+1,1)";
    int lastInvoiceRow = invoices.size() + 1;
    String dates = INVOICE_REF + "$F$2:$F$" + lastInvoiceRow;
    String when = dates + ",\">=\"&" + ref + "," + dates + ",\"<\"&" + next;
    boolean empty = invoices.isEmpty();
    setFormulaOrZero(row, 2, empty ? null : "COUNTIFS(" + when + ")", styles.cell(Kind.INT, false));
    setFormulaOrZero(
        row,
        3,
        empty ? null : "SUMIFS(" + INVOICE_REF + "$G$2:$G$" + lastInvoiceRow + "," + when + ")",
        styles.cell(Kind.MONEY, false));
    setFormula(
        row,
        4,
        "IF($" + totalRevenue + "=0,0,D" + (rowIndex + 1) + "/$" + totalRevenue + ")",
        styles.cell(Kind.PERCENT, false));
  }

  /** Totals come from the invoice sheet so invoices without a date are still counted. */
  private void writeSummaryTotalRow(
      Sheet sheet, Styles styles, int rowIndex, List<RevenueResult> invoices, String totalRevenue) {
    Row row = sheet.createRow(rowIndex);
    writeTotalLabel(sheet, styles, row, 1);
    String amounts = INVOICE_REF + "$G$2:$G$" + (invoices.size() + 1);
    boolean empty = invoices.isEmpty();
    setFormulaOrZero(row, 2, empty ? null : "COUNT(" + amounts + ")", styles.cell(Kind.INT, true));
    setFormulaOrZero(row, 3, empty ? null : "SUM(" + amounts + ")", styles.cell(Kind.MONEY, true));
    setFormula(row, 4, "IF(" + totalRevenue + "=0,0,1)", styles.cell(Kind.PERCENT, true));
  }

  /** Note, date line and the preparer's signature block on the right. */
  private void writeSignature(Sheet sheet, Styles styles, int firstRow, String preparedBy) {
    setText(
        sheet.createRow(firstRow),
        0,
        "Ghi chú: Doanh thu chỉ tính các hóa đơn đã thanh toán.",
        styles.note);
    LocalDate today = LocalDate.now();
    String dateLine =
        String.format(
            "Ngày %02d tháng %02d năm %d",
            today.getDayOfMonth(), today.getMonthValue(), today.getYear());
    int column = SUMMARY_VALUE_COLUMN;
    mergedText(
        sheet,
        sheet.createRow(firstRow + 2),
        column,
        SUMMARY_LAST_COLUMN,
        dateLine,
        styles.italicCenter);
    mergedText(
        sheet,
        sheet.createRow(firstRow + 3),
        column,
        SUMMARY_LAST_COLUMN,
        "Người lập biểu",
        styles.boldCenter);
    mergedText(
        sheet,
        sheet.createRow(firstRow + 4),
        column,
        SUMMARY_LAST_COLUMN,
        "(Ký, ghi rõ họ tên)",
        styles.italicCenter);
    if (preparedBy != null && !preparedBy.isBlank()) {
      mergedText(
          sheet,
          sheet.createRow(firstRow + 8),
          column,
          SUMMARY_LAST_COLUMN,
          preparedBy.trim(),
          styles.boldCenter);
    }
  }

  private void writeInfoText(Sheet sheet, Styles styles, int rowIndex, String label, String value) {
    Row row = infoRow(sheet, styles, rowIndex, label);
    mergedText(sheet, row, SUMMARY_VALUE_COLUMN, SUMMARY_LAST_COLUMN, value, styles.infoText);
  }

  private void writeInfoFormula(
      Sheet sheet, Styles styles, int rowIndex, String label, String formula, CellStyle style) {
    Row row = infoRow(sheet, styles, rowIndex, label);
    setFormula(row, SUMMARY_VALUE_COLUMN, formula, style);
  }

  /** Label merged over columns A:B. */
  private Row infoRow(Sheet sheet, Styles styles, int rowIndex, String label) {
    Row row = sheet.createRow(rowIndex);
    mergedText(sheet, row, 0, 1, label, styles.infoLabel);
    return row;
  }

  private boolean isPerDaySummary(LocalDate startDate, LocalDate endDate) {
    return startDate != null
        && endDate != null
        && ChronoUnit.DAYS.between(startDate, endDate) < MAX_DAYS_PER_DAY_SUMMARY;
  }

  /**
   * Days or months (first day of the month) to list. When both dates are known every period in the
   * range is listed, including periods without revenue; otherwise only periods with invoices.
   */
  private List<LocalDate> listPeriods(
      List<RevenueResult> invoices, LocalDate startDate, LocalDate endDate, boolean perDay) {
    TreeSet<LocalDate> periods = new TreeSet<>();
    if (startDate != null && endDate != null) {
      LocalDate period = toPeriod(startDate, perDay);
      LocalDate last = toPeriod(endDate, perDay);
      while (!period.isAfter(last)) {
        periods.add(period);
        period = perDay ? period.plusDays(1) : period.plusMonths(1);
      }
    }
    for (RevenueResult invoice : invoices) {
      if (invoice.getCreatedAt() != null) {
        periods.add(toPeriod(invoice.getCreatedAt().toLocalDate(), perDay));
      }
    }
    return List.copyOf(periods);
  }

  private LocalDate toPeriod(LocalDate date, boolean perDay) {
    return perDay ? date : date.withDayOfMonth(1);
  }

  private String formatPeriod(ReportInfo info) {
    LocalDate start = info.startDate();
    LocalDate end = info.endDate();
    if (start != null && end != null) {
      return start.format(DATE) + " - " + end.format(DATE);
    }
    if (start != null) {
      return "từ " + start.format(DATE);
    }
    if (end != null) {
      return "đến " + end.format(DATE);
    }
    return "toàn bộ thời gian";
  }

  // ── Sheet 2: invoices ───────────────────────────────────────────────

  private void writeInvoiceSheet(Sheet sheet, Styles styles, List<RevenueResult> invoices) {
    setColumnWidths(sheet, 7, 22, 26, 22, 10, 18, 20);
    writeHeader(
        sheet, styles, 0, "STT", INVOICE_CODE, SHOP, "Nhân viên", "Bàn", CREATED_AT, REVENUE_VND);

    int rowIndex = 1;
    for (RevenueResult invoice : invoices) {
      Row row = sheet.createRow(rowIndex);
      setNumber(row, 0, rowIndex, styles.cell(Kind.CENTER_INT, false));
      setText(row, 1, textOr(invoice.getInvoiceCode(), NO_VALUE), styles.cell(Kind.TEXT, false));
      setText(row, 2, textOr(invoice.getShopName(), NO_VALUE), styles.cell(Kind.TEXT, false));
      setText(row, 3, textOr(invoice.getStaffName(), NO_VALUE), styles.cell(Kind.TEXT, false));
      setText(row, 4, formatTable(invoice.getTableNumber()), styles.cell(Kind.CENTER, false));
      setDateTime(row, 5, invoice.getCreatedAt(), styles);
      setMoney(row, 6, invoice.getTotalAmount(), styles.cell(Kind.MONEY, false));
      rowIndex++;
    }

    Row sumRow = sheet.createRow(rowIndex);
    writeTotalLabel(sheet, styles, sumRow, 5);
    setFormulaOrZero(
        sumRow,
        6,
        invoices.isEmpty() ? null : "SUM(G2:G" + rowIndex + ")",
        styles.cell(Kind.MONEY, true));

    sheet.createFreezePane(0, 1);
    if (!invoices.isEmpty()) {
      sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 6));
    }
    setupPrint(sheet, true, 0);
  }

  private String formatTable(Integer tableNumber) {
    return tableNumber != null ? "Bàn " + tableNumber : NO_VALUE;
  }

  // ── Sheet 3: drinks sold ────────────────────────────────────────────

  private void writeItemSheet(Sheet sheet, Styles styles, List<RevenueResult> invoices) {
    setColumnWidths(sheet, 22, 18, 24, 26, 10, 10, 16, 18, 30);
    writeHeader(
        sheet,
        styles,
        0,
        INVOICE_CODE,
        CREATED_AT,
        SHOP,
        "Món uống",
        "Kích cỡ",
        "Số lượng",
        "Đơn giá (VNĐ)",
        "Thành tiền (VNĐ)",
        "Ghi chú");

    int rowIndex = 1;
    for (RevenueResult invoice : invoices) {
      List<RevenueItemResult> items = itemsOf(invoice);
      if (!items.isEmpty()) {
        writeInvoiceItems(sheet, styles, rowIndex, invoice, items);
        rowIndex += items.size();
      }
    }

    if (rowIndex > 1) {
      Row sumRow = sheet.createRow(rowIndex);
      writeTotalLabel(sheet, styles, sumRow, 4);
      setFormula(sumRow, 5, "SUM(F2:F" + rowIndex + ")", styles.cell(Kind.INT, true));
      sumRow.createCell(6).setCellStyle(styles.cell(Kind.MONEY, true));
      setFormula(sumRow, 7, "SUM(H2:H" + rowIndex + ")", styles.cell(Kind.MONEY, true));
      sumRow.createCell(8).setCellStyle(styles.cell(Kind.TEXT, true));
      // Filter buttons on the header, same as the invoice sheet (total row excluded)
      sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 8));
    }

    sheet.createFreezePane(0, 1);
    setupPrint(sheet, true, 0);
  }

  /**
   * Rows of one invoice: the invoice code, time and shop are written once and merged vertically
   * over all its drinks.
   */
  private void writeInvoiceItems(
      Sheet sheet,
      Styles styles,
      int firstRow,
      RevenueResult invoice,
      List<RevenueItemResult> items) {
    CellStyle text = styles.cell(Kind.TEXT, false);
    for (int i = 0; i < items.size(); i++) {
      RevenueItemResult item = items.get(i);
      int rowIndex = firstRow + i;
      Row row = sheet.createRow(rowIndex);
      if (i == 0) {
        setText(row, 0, textOr(invoice.getInvoiceCode(), NO_VALUE), text);
        setDateTime(row, 1, invoice.getCreatedAt(), styles);
        setText(row, 2, textOr(invoice.getShopName(), NO_VALUE), text);
      } else {
        row.createCell(0).setCellStyle(text);
        row.createCell(1).setCellStyle(styles.cell(Kind.DATE_TIME, false));
        row.createCell(2).setCellStyle(text);
      }
      setText(row, 3, textOr(item.getDrinkName(), NO_VALUE), text);
      setText(row, 4, textOr(item.getSize(), NO_VALUE), styles.cell(Kind.CENTER, false));
      setNumber(
          row,
          5,
          item.getQuantity() != null ? item.getQuantity() : 0,
          styles.cell(Kind.INT, false));
      setMoney(row, 6, item.getUnitPrice(), styles.cell(Kind.MONEY, false));
      int excelRow = rowIndex + 1;
      setFormula(row, 7, "F" + excelRow + "*G" + excelRow, styles.cell(Kind.MONEY, false));
      setText(row, 8, textOr(item.getNote(), ""), styles.cell(Kind.WRAP, false));
    }
    if (items.size() > 1) {
      int lastRow = firstRow + items.size() - 1;
      for (int column = 0; column <= 2; column++) {
        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, column, column));
      }
    }
  }

  /** Detail rows of an invoice; an invoice without details yields an empty list. */
  private List<RevenueItemResult> itemsOf(RevenueResult invoice) {
    if (invoice.getItems() == null) {
      return List.of();
    }
    return invoice.getItems().stream()
        .filter(Objects::nonNull)
        .filter(item -> item.getInvoiceDetailId() != null)
        .toList();
  }

  // ── Layout helpers ──────────────────────────────────────────────────

  /**
   * Text in {@code firstColumn}, merged up to {@code lastColumn} (always after {@code
   * firstColumn}), every cell styled.
   */
  private void mergedText(
      Sheet sheet, Row row, int firstColumn, int lastColumn, String value, CellStyle style) {
    setText(row, firstColumn, value, style);
    for (int column = firstColumn + 1; column <= lastColumn; column++) {
      row.createCell(column).setCellStyle(style);
    }
    sheet.addMergedRegion(
        new CellRangeAddress(row.getRowNum(), row.getRowNum(), firstColumn, lastColumn));
  }

  private void writeHeader(Sheet sheet, Styles styles, int rowIndex, String... titles) {
    Row row = sheet.createRow(rowIndex);
    row.setHeightInPoints(22);
    for (int i = 0; i < titles.length; i++) {
      setText(row, i, titles[i], styles.header);
    }
  }

  /** "Tổng cộng" merged from column A to {@code lastColumn}. */
  private void writeTotalLabel(Sheet sheet, Styles styles, Row row, int lastColumn) {
    mergedText(sheet, row, 0, lastColumn, TOTAL_LABEL, styles.totalLabel);
  }

  private void setColumnWidths(Sheet sheet, int... widthsInChars) {
    for (int i = 0; i < widthsInChars.length; i++) {
      sheet.setColumnWidth(i, widthsInChars[i] * 256);
    }
  }

  /** A4, fit to one page wide; {@code headerRow} >= 0 is repeated on every printed page. */
  private void setupPrint(Sheet sheet, boolean landscape, int headerRow) {
    PrintSetup print = sheet.getPrintSetup();
    print.setPaperSize(PrintSetup.A4_PAPERSIZE);
    print.setLandscape(landscape);
    print.setFitWidth((short) 1);
    print.setFitHeight((short) 0);
    sheet.setFitToPage(true);
    sheet.setHorizontallyCenter(true);
    if (headerRow >= 0) {
      sheet.setRepeatingRows(new CellRangeAddress(headerRow, headerRow, -1, -1));
    }
  }

  // ── Cell helpers ────────────────────────────────────────────────────

  private void setText(Row row, int column, String value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setNumber(Row row, int column, long value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setMoney(Row row, int column, BigDecimal value, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellValue(value != null ? value.doubleValue() : 0D);
    cell.setCellStyle(style);
  }

  private void setFormula(Row row, int column, String formula, CellStyle style) {
    Cell cell = row.createCell(column);
    cell.setCellFormula(formula);
    cell.setCellStyle(style);
  }

  /** Formula cell, or 0 when there is nothing to reference (empty report). */
  private void setFormulaOrZero(Row row, int column, String formula, CellStyle style) {
    if (formula == null) {
      setNumber(row, column, 0, style);
    } else {
      setFormula(row, column, formula, style);
    }
  }

  private void setDateTime(Row row, int column, LocalDateTime value, Styles styles) {
    if (value == null) {
      setText(row, column, NO_VALUE, styles.cell(Kind.CENTER, false));
      return;
    }
    Cell cell = row.createCell(column);
    cell.setCellValue(value);
    cell.setCellStyle(styles.cell(Kind.DATE_TIME, false));
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  // ── Styles ──────────────────────────────────────────────────────────

  /** Table cell kinds: alignment and number format. */
  private enum Kind {
    TEXT,
    WRAP,
    CENTER,
    CENTER_INT,
    INT,
    MONEY,
    PERCENT,
    DATE_TIME,
    DAY,
    MONTH
  }

  /**
   * Formal black-on-white styles (Times New Roman, thin black borders), light grey only on table
   * headers and totals; table cell styles are cached.
   */
  private static final class Styles {
    private static final String FONT = "Times New Roman";
    private static final XSSFColor SHADE =
        new XSSFColor(new byte[] {(byte) 0xF2, (byte) 0xF2, (byte) 0xF2}, null);
    private static final String NUMBER_FORMAT = "#,##0;-#,##0;\"-\"";

    private final XSSFWorkbook workbook;
    private final XSSFFont regular;
    private final XSSFFont bold;
    private final XSSFFont italic;
    private final Map<String, CellStyle> cache = new HashMap<>();

    private final CellStyle headingBold;
    private final CellStyle heading;
    private final CellStyle title;
    private final CellStyle italicCenter;
    private final CellStyle boldCenter;
    private final CellStyle note;
    private final CellStyle infoLabel;
    private final CellStyle infoText;
    private final CellStyle infoInt;
    private final CellStyle infoMoney;
    private final CellStyle header;
    private final CellStyle totalLabel;

    Styles(XSSFWorkbook workbook) {
      this.workbook = workbook;
      workbook.getFontAt(0).setFontName(FONT);
      regular = font(false, false, 11);
      bold = font(true, false, 11);
      italic = font(false, true, 11);

      headingBold = plain(font(true, false, 12), HorizontalAlignment.CENTER);
      heading = plain(font(false, false, 12), HorizontalAlignment.CENTER);
      title = plain(font(true, false, 16), HorizontalAlignment.CENTER);
      italicCenter = plain(italic, HorizontalAlignment.CENTER);
      boldCenter = plain(bold, HorizontalAlignment.CENTER);
      note = plain(italic, HorizontalAlignment.LEFT);
      infoLabel = plain(regular, HorizontalAlignment.LEFT);
      infoText = plain(bold, HorizontalAlignment.LEFT);
      infoInt = plain(bold, HorizontalAlignment.LEFT);
      infoInt.setDataFormat(format(NUMBER_FORMAT));
      infoMoney = plain(bold, HorizontalAlignment.LEFT);
      infoMoney.setDataFormat(format(NUMBER_FORMAT));

      header = bordered(bold, true);
      header.setAlignment(HorizontalAlignment.CENTER);
      header.setWrapText(true);
      totalLabel = bordered(bold, true);
      totalLabel.setAlignment(HorizontalAlignment.CENTER);
    }

    CellStyle cell(Kind kind, boolean total) {
      return cache.computeIfAbsent(kind + "_" + total, key -> createCell(kind, total));
    }

    private CellStyle createCell(Kind kind, boolean total) {
      XSSFCellStyle style = bordered(total ? bold : regular, total);
      switch (kind) {
        case WRAP -> style.setWrapText(true);
        case CENTER -> style.setAlignment(HorizontalAlignment.CENTER);
        case CENTER_INT -> centered(style, "0");
        case INT, MONEY -> style.setDataFormat(format(NUMBER_FORMAT));
        case PERCENT -> style.setDataFormat(format("0.0%;-0.0%;\"-\""));
        case DATE_TIME -> centered(style, "dd/mm/yyyy hh:mm");
        case DAY -> centered(style, "dd/mm/yyyy");
        case MONTH -> centered(style, "mm/yyyy");
        default -> {
          // TEXT: left aligned, general format
        }
      }
      return style;
    }

    private void centered(XSSFCellStyle style, String pattern) {
      style.setAlignment(HorizontalAlignment.CENTER);
      style.setDataFormat(format(pattern));
    }

    private short format(String pattern) {
      return workbook.createDataFormat().getFormat(pattern);
    }

    /** No border, no fill: text blocks of the report. */
    private XSSFCellStyle plain(XSSFFont font, HorizontalAlignment alignment) {
      XSSFCellStyle style = workbook.createCellStyle();
      style.setFont(font);
      style.setAlignment(alignment);
      style.setVerticalAlignment(VerticalAlignment.CENTER);
      return style;
    }

    /** Table cell: thin black border, light grey fill when shaded. */
    private XSSFCellStyle bordered(XSSFFont font, boolean shaded) {
      XSSFCellStyle style = plain(font, HorizontalAlignment.GENERAL);
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
      if (shaded) {
        style.setFillForegroundColor(SHADE);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      }
      return style;
    }

    private XSSFFont font(boolean isBold, boolean isItalic, int size) {
      XSSFFont font = workbook.createFont();
      font.setFontName(FONT);
      font.setBold(isBold);
      font.setItalic(isItalic);
      font.setFontHeightInPoints((short) size);
      return font;
    }
  }
}
