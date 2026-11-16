package coffee.api.services.services_implement.invoice.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import coffee.api.dto.result.InvoiceReceiptResult;
import coffee.api.dto.result.InvoiceResult.InvoiceItemResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InvoiceReceiptPdfWriterTest {

  private InvoiceReceiptPdfWriter writer;
  private InvoiceReceiptResult receipt;

  @BeforeEach
  void setUp() {
    writer = new InvoiceReceiptPdfWriter();
    receipt = new InvoiceReceiptResult();
    receipt.setInvoiceId(UUID.fromString("e1000000-0000-0000-0000-000000000001"));
    receipt.setInvoiceCode("INV-20261109-1A2B3C4D");
    receipt.setShopName("Cà Phê Phố Cổ");
    receipt.setShopAddress("12 Hàng Bạc, Hoàn Kiếm, Hà Nội");
    receipt.setShopPhone("0901234567");
    receipt.setStaffName("Nguyễn Văn An");
    receipt.setTableNumber(5);
    receipt.setStatus(1);
    receipt.setCreatedAt(LocalDateTime.of(2026, 11, 9, 14, 5));
    receipt.setTotalAmount(new BigDecimal("1095000.00"));
    receipt.setItems(
        new ArrayList<>(
            List.of(
                item("Cà phê sữa đá", "M", 2, "35000.00", "Ít đá"),
                item("Trà đào cam sả", "L", 1, "1025000.00", null))));
  }

  // =========================================================================
  // HELPERS
  // =========================================================================

  private InvoiceItemResult item(
      String name, String size, int quantity, String unitPrice, String note) {
    InvoiceItemResult result = new InvoiceItemResult();
    result.setDrinkName(name);
    result.setSize(size);
    result.setQuantity(quantity);
    result.setUnitPrice(new BigDecimal(unitPrice));
    result.setItemTotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
    result.setNote(note);
    return result;
  }

  private PDRectangle pageSize(byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertEquals(1, document.getNumberOfPages());
      return document.getPage(0).getMediaBox();
    }
  }

  private String text(byte[] pdf) throws IOException {
    try (PDDocument document = Loader.loadPDF(pdf)) {
      return new PDFTextStripper().getText(document);
    }
  }

  // =========================================================================
  // WRITE - LAYOUT
  // =========================================================================

  @Test
  void write_ProducesSinglePage80mmWide_TC001() throws IOException {
    PDRectangle size = pageSize(writer.write(receipt));

    assertEquals(226.77f, size.getWidth(), 0.01f);
    assertTrue(size.getHeight() > 100f);
  }

  @Test
  void write_PrintsShopInvoiceItemsAndTotalInVietnamese_TC002() throws IOException {
    String text = text(writer.write(receipt));

    for (String expected :
        List.of(
            "Cà Phê Phố Cổ",
            "12 Hàng Bạc, Hoàn Kiếm, Hà Nội",
            "ĐT: 0901234567",
            "HÓA ĐƠN THANH TOÁN",
            "INV-20261109-1A2B3C4D",
            "09/11/2026 14:05",
            "Nguyễn Văn An",
            "Cà phê sữa đá (M)",
            "2 x 35.000",
            "70.000",
            "Ghi chú: Ít đá",
            "Trà đào cam sả (L)",
            "1 x 1.025.000",
            "TỔNG CỘNG",
            "1.095.000 đ",
            "ĐÃ THANH TOÁN",
            "Cảm ơn quý khách, hẹn gặp lại!")) {
      assertTrue(text.contains(expected), () -> "Missing: " + expected + " in\n" + text);
    }
  }

  @Test
  void write_GrowsPageHeight_WhenMoreItemsAreAdded_TC003() throws IOException {
    float shortHeight = pageSize(writer.write(receipt)).getHeight();
    for (int i = 0; i < 10; i++) {
      receipt.getItems().add(item("Bạc xỉu " + i, "S", 1, "29000.00", null));
    }

    float longHeight = pageSize(writer.write(receipt)).getHeight();

    assertTrue(longHeight > shortHeight + 100f);
  }

  @Test
  void write_WrapsLongDrinkNameAndNote_InsteadOfCuttingThem_TC004() throws IOException {
    String longName = "Cà phê muối kem phô mai sữa dừa nướng thơm béo đặc biệt của quán";
    String longNote = "Không đường, ít đá, thêm một shot espresso và mang về cho khách";
    receipt.setItems(new ArrayList<>(List.of(item(longName, "L", 1, "65000.00", longNote))));

    String text = text(writer.write(receipt)).replaceAll("\\s+", " ");

    assertTrue(text.contains(longName + " (L)"), text);
    assertTrue(text.contains("Ghi chú: " + longNote), text);
  }

  @Test
  void write_SplitsWordLongerThanTheLine_TC005() throws IOException {
    String longWord = "A".repeat(120);
    receipt.setItems(new ArrayList<>(List.of(item(longWord, null, 1, "10000.00", null))));

    String text = text(writer.write(receipt)).replaceAll("\\s+", "");

    assertTrue(text.contains(longWord), text);
  }

  @Test
  void write_MovesItemTotalBelow_WhenQuantityAndPriceAreAtTheMaximum_TC006() throws IOException {
    receipt.setItems(
        new ArrayList<>(List.of(item("Cà phê", "S", 100, "9999999999999999.99", null))));
    receipt.setTotalAmount(new BigDecimal("999999999999999999.00"));

    String text = text(writer.write(receipt));

    String lineBreak = System.lineSeparator();
    assertTrue(
        text.contains("100 x 10.000.000.000.000.000" + lineBreak + "999.999.999.999.999.999"),
        text);
    assertTrue(text.contains("TỔNG CỘNG 999.999.999.999.999.999 đ"), text);
  }

  @Test
  void write_MovesWideValueToItsOwnRightAlignedLine_TC007() throws IOException {
    String longStaffName = "Nguyễn Hoàng Thị Phương Thảo Bảo Ngọc Minh Anh Khánh Linh";
    receipt.setStaffName(longStaffName);

    String text = text(writer.write(receipt));

    assertTrue(text.contains("Nhân viên:" + System.lineSeparator()), text);
    assertTrue(text.replaceAll("\\s+", " ").contains(longStaffName), text);
  }

  // =========================================================================
  // WRITE - MISSING OR UNSUPPORTED DATA
  // =========================================================================

  @Test
  void write_PrintsDash_WhenOptionalFieldsAreMissing_TC008() throws IOException {
    receipt.setShopAddress(null);
    receipt.setShopPhone(" ");
    receipt.setStaffName(null);
    receipt.setTableNumber(null);
    receipt.setCreatedAt(null);
    receipt.setItems(null);
    receipt.setTotalAmount(null);

    String text = text(writer.write(receipt));

    assertTrue(text.contains("Bàn: -"), text);
    assertTrue(text.contains("Nhân viên: -"), text);
    assertTrue(text.contains("Ngày: -"), text);
    assertTrue(text.contains("0 đ"), text);
    assertTrue(!text.contains("ĐT:"), text);
  }

  @Test
  void write_ReplacesCharactersTheFontCannotDraw_TC009() {
    receipt.setItems(
        new ArrayList<>(
            List.of(item("Trà sữa \uD83E\uDDCB\tsize\nlớn", "M", 1, "40000.00", null))));

    String text = assertDoesNotThrow(() -> text(writer.write(receipt)));

    assertTrue(text.contains("Trà sữa ? size lớn (M)"), text);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"0, CHƯA THANH TOÁN", "1, ĐÃ THANH TOÁN", "2, ĐÃ HỦY", "9, -", "NULL, -"},
      nullValues = "NULL")
  void write_PrintsStatusLabel_TC010(Integer status, String label) throws IOException {
    receipt.setStatus(status);

    String text = text(writer.write(receipt));

    assertTrue(text.contains(label), text);
  }

  // =========================================================================
  // WRITE - RENDERING FAILURE
  // =========================================================================

  @Test
  void write_ThrowsUncheckedIOException_WhenFontIsMissing_TC011() {
    InvoiceReceiptPdfWriter brokenWriter =
        new InvoiceReceiptPdfWriter("/fonts/missing.ttf", "/fonts/missing-bold.ttf");

    var error = assertThrows(UncheckedIOException.class, () -> brokenWriter.write(receipt));

    assertEquals("Cannot render invoice receipt", error.getMessage());
    assertEquals("Font not found: /fonts/missing.ttf", error.getCause().getMessage());
  }
}
