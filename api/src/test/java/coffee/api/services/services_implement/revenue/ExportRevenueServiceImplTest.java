package coffee.api.services.services_implement.revenue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.result.RevenueExportResult;
import coffee.api.dto.result.RevenueResult;
import coffee.api.enums.SortDirection;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.ExportRevenueMapper;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_implement.revenue.common.RevenueExcelWriter;
import coffee.api.services.services_implement.revenue.common.RevenueFilterResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExportRevenueServiceImplTest {

  // Summary sheet layout written by RevenueExcelWriter; period table columns: STT, period, count,
  // revenue, share
  private static final int SUMMARY_SHOP_ROW = RevenueExcelWriter.SUMMARY_SHOP_ROW;
  private static final int SUMMARY_PERIOD_ROW = RevenueExcelWriter.SUMMARY_PERIOD_ROW;
  private static final int SUMMARY_SEARCH_ROW = RevenueExcelWriter.SUMMARY_SEARCH_ROW;
  private static final int SUMMARY_COUNT_ROW = RevenueExcelWriter.SUMMARY_COUNT_ROW;
  private static final int SUMMARY_TOTAL_ROW = RevenueExcelWriter.SUMMARY_TOTAL_ROW;
  private static final int SUMMARY_HEADER_ROW = RevenueExcelWriter.SUMMARY_HEADER_ROW;
  private static final int SUMMARY_FIRST_PERIOD_ROW = SUMMARY_HEADER_ROW + 1;
  private static final int VALUE = RevenueExcelWriter.SUMMARY_VALUE_COLUMN;

  @Mock private ExportRevenueMapper exportRevenueMapper;
  @Mock private GetRevenueMapper getRevenueMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;

  private ExportRevenueServiceImpl service;
  private SearchRevenueRequest request;
  private UUID shopId;
  private UUID otherShopId;
  private UUID userId;

  @BeforeEach
  void setUp() {
    service =
        new ExportRevenueServiceImpl(
            exportRevenueMapper,
            getRevenueMapper,
            new RevenueFilterResolver(new InvoiceValidator(createInvoiceMapper, commonMapper)),
            new RevenueExcelWriter());
    shopId = UUID.fromString("00000000-0000-0000-0000-000000000501");
    otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000502");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000503");
    request = new SearchRevenueRequest();
  }

  private RevenueResult invoice(String code, LocalDateTime createdAt, String amount) {
    RevenueResult invoice = new RevenueResult();
    invoice.setInvoiceId(UUID.randomUUID());
    invoice.setInvoiceCode(code);
    invoice.setShopId(shopId);
    invoice.setShopName("Chi nhánh 1");
    invoice.setStaffName("Nhân viên A");
    invoice.setTableNumber(3);
    invoice.setCreatedAt(createdAt);
    invoice.setTotalAmount(new BigDecimal(amount));
    var item = new RevenueResult.RevenueItemResult();
    item.setInvoiceDetailId(UUID.randomUUID());
    item.setDrinkName("Cà Phê Sữa");
    item.setSize("M");
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("29000"));
    item.setItemTotal(new BigDecimal("58000"));
    item.setNote("Ít đá");
    invoice.setItems(new ArrayList<>(List.of(item)));
    return invoice;
  }

  private void stubShopPermission(UUID shop, String role) {
    when(commonMapper.checkShopExisted(shop)).thenReturn(true);
    if (!"OWNER".equals(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shop)).thenReturn(true);
    }
  }

  private void stubRows(
      String role, UUID target, LocalDateTime start, LocalDateTime end, List<RevenueResult> rows) {
    when(getRevenueMapper.countRevenueFiltered(null, target, null, start, end, role, shopId))
        .thenReturn(rows == null ? 1L : (long) rows.size());
    when(exportRevenueMapper.getRevenueForExport(
            null, target, null, start, end, "createdAt", "DESC", role, shopId))
        .thenReturn(rows);
  }

  private Workbook open(RevenueExportResult result) throws IOException {
    return new XSSFWorkbook(new ByteArrayInputStream(result.getContent()));
  }

  private String text(Sheet sheet, int row, int column) {
    return sheet.getRow(row).getCell(column).getStringCellValue();
  }

  /** Numeric value; for formula cells this is the value cached when the file was written. */
  private double number(Sheet sheet, int row, int column) {
    return sheet.getRow(row).getCell(column).getNumericCellValue();
  }

  /** Value as Excel displays it (date cells in their dd/mm/yyyy or mm/yyyy format). */
  private String shown(Sheet sheet, int row, int column) {
    return new DataFormatter().formatCellValue(sheet.getRow(row).getCell(column));
  }

  private void setRange(LocalDate start, LocalDate end) {
    request.setStartDate(start);
    request.setEndDate(end);
  }

  // =========================================================================
  // SUCCESS
  // =========================================================================

  @Test
  void process_ManagerSuccess_WritesSummaryInvoicesAndItems_TC001() throws IOException {
    LocalDate start = LocalDate.of(2026, 11, 2);
    LocalDate end = LocalDate.of(2026, 11, 8);
    setRange(start, end);
    stubShopPermission(shopId, "MANAGER");
    when(exportRevenueMapper.getShopName(shopId)).thenReturn("Chi nhánh 1");
    when(exportRevenueMapper.getProfileName(userId)).thenReturn("Nguyễn Quản Lý");
    stubRows(
        "MANAGER",
        shopId,
        start.atStartOfDay(),
        end.atTime(LocalTime.MAX),
        List.of(
            invoice("IV-0001", LocalDateTime.of(2026, 11, 2, 9, 30), "58000"),
            invoice("IV-0002", LocalDateTime.of(2026, 11, 2, 15, 0), "42000"),
            invoice("IV-0003", LocalDateTime.of(2026, 11, 5, 10, 0), "100000")));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    assertEquals("doanh-thu_20261102_20261108.xlsx", result.getFileName());
    try (Workbook workbook = open(result)) {
      assertEquals(3, workbook.getNumberOfSheets());
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Chi nhánh 1", text(summary, SUMMARY_SHOP_ROW, VALUE));
      assertEquals("Kỳ báo cáo: 02/11/2026 - 08/11/2026", text(summary, SUMMARY_PERIOD_ROW, 0));
      // Administrative header, title and signature block; nothing frozen on the summary
      assertEquals(RevenueExcelWriter.ORGANIZATION, text(summary, 0, 0));
      // No national motto; gridlines stay visible
      assertNull(summary.getRow(0).getCell(2));
      assertNull(summary.getRow(1).getCell(2));
      assertTrue(summary.isDisplayGridlines());
      assertEquals("CHI NHÁNH 1", text(summary, 1, 0));
      assertEquals("BÁO CÁO DOANH THU", text(summary, RevenueExcelWriter.SUMMARY_TITLE_ROW, 0));
      assertNull(summary.getPaneInformation());
      int signatureRow = SUMMARY_FIRST_PERIOD_ROW + 7 + 2;
      assertTrue(text(summary, signatureRow, 0).startsWith("Ghi chú:"));
      assertTrue(text(summary, signatureRow + 2, VALUE).startsWith("Ngày "));
      assertEquals("Người lập biểu", text(summary, signatureRow + 3, VALUE));
      assertEquals("Nguyễn Quản Lý", text(summary, signatureRow + 8, VALUE));
      assertEquals("—", text(summary, SUMMARY_SEARCH_ROW, VALUE));
      assertEquals(3, number(summary, SUMMARY_COUNT_ROW, VALUE));
      assertEquals(200000, number(summary, SUMMARY_TOTAL_ROW, VALUE));
      assertEquals("Ngày", text(summary, SUMMARY_HEADER_ROW, 1));
      // Every day of the week is listed, days without revenue show 0
      assertEquals("02/11/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW, 1));
      assertEquals(2, number(summary, SUMMARY_FIRST_PERIOD_ROW, 2));
      assertEquals(100000, number(summary, SUMMARY_FIRST_PERIOD_ROW, 3));
      assertEquals(0.5, number(summary, SUMMARY_FIRST_PERIOD_ROW, 4), 1e-9);
      assertEquals(0, number(summary, SUMMARY_FIRST_PERIOD_ROW + 1, 3));
      assertEquals(100000, number(summary, SUMMARY_FIRST_PERIOD_ROW + 3, 3));
      assertEquals("08/11/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW + 6, 1));
      assertEquals("Tổng cộng", text(summary, SUMMARY_FIRST_PERIOD_ROW + 7, 0));
      assertEquals(200000, number(summary, SUMMARY_FIRST_PERIOD_ROW + 7, 3));
      // Figures are live formulas over the invoice sheet, not fixed numbers
      Cell dayRevenue = summary.getRow(SUMMARY_FIRST_PERIOD_ROW).getCell(3);
      assertEquals(CellType.FORMULA, dayRevenue.getCellType());
      assertTrue(dayRevenue.getCellFormula().startsWith("SUMIFS('Hóa đơn'!$G$2:$G$4"));
      assertEquals(
          "C" + (SUMMARY_FIRST_PERIOD_ROW + 8),
          summary.getRow(SUMMARY_COUNT_ROW).getCell(VALUE).getCellFormula());

      Sheet invoices = workbook.getSheet(RevenueExcelWriter.SHEET_INVOICES);
      assertEquals("IV-0001", text(invoices, 1, 1));
      assertEquals("Nhân viên A", text(invoices, 1, 3));
      assertEquals("Bàn 3", text(invoices, 1, 4));
      assertEquals(58000, number(invoices, 1, 6));
      assertEquals(200000, number(invoices, 4, 6));

      Sheet items = workbook.getSheet(RevenueExcelWriter.SHEET_ITEMS);
      // 3 drinks + total row; one drink per invoice, so only the total label is merged
      assertEquals(4, items.getLastRowNum());
      assertEquals(1, items.getNumMergedRegions());
      // Header filter like the invoice sheet: header + 3 drink rows, total row excluded
      assertEquals("A1:I4", ((XSSFSheet) items).getCTWorksheet().getAutoFilter().getRef());
      assertEquals("A1:G4", ((XSSFSheet) invoices).getCTWorksheet().getAutoFilter().getRef());
      assertEquals("Cà Phê Sữa", text(items, 1, 3));
      assertEquals(2, number(items, 1, 5));
      assertEquals(58000, number(items, 1, 7));
      assertEquals("Ít đá", text(items, 1, 8));
      assertEquals("Tổng cộng", text(items, 4, 0));
      assertEquals(6, number(items, 4, 5));
      assertEquals(174000, number(items, 4, 7));
    }
  }

  @Test
  void process_OwnerWithoutShopOrDates_ExportsAllShopsPerMonth_TC002() throws IOException {
    stubRows(
        "OWNER",
        null,
        null,
        null,
        List.of(
            invoice("IV-0001", LocalDateTime.of(2026, 10, 2, 9, 0), "50000"),
            invoice("IV-0002", LocalDateTime.of(2026, 11, 3, 9, 0), "70000")));

    RevenueExportResult result = service.process(request, "OWNER", shopId, userId);

    assertEquals("doanh-thu_tat-ca.xlsx", result.getFileName());
    verify(exportRevenueMapper, never()).getShopName(any());
    verifyNoInteractions(commonMapper);
    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Tất cả", text(summary, SUMMARY_SHOP_ROW, VALUE));
      assertEquals("TẤT CẢ CHI NHÁNH", text(summary, 1, 0));
      assertEquals("Kỳ báo cáo: toàn bộ thời gian", text(summary, SUMMARY_PERIOD_ROW, 0));
      assertEquals("Tháng", text(summary, SUMMARY_HEADER_ROW, 1));
      assertEquals("10/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW, 1));
      assertEquals("11/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW + 1, 1));
      assertEquals(70000, number(summary, SUMMARY_FIRST_PERIOD_ROW + 1, 3));
    }
  }

  @Test
  void process_OwnerSelectsShop_UsesShopNameInHeader_TC003() throws IOException {
    request.setShopId(otherShopId);
    stubShopPermission(otherShopId, "OWNER");
    when(exportRevenueMapper.getShopName(otherShopId)).thenReturn("Chi nhánh 2");
    when(getRevenueMapper.countRevenueFiltered(
            null, otherShopId, null, null, null, "OWNER", shopId))
        .thenReturn(0L);

    RevenueExportResult result = service.process(request, "OWNER", shopId, userId);

    try (Workbook workbook = open(result)) {
      assertEquals(
          "Chi nhánh 2",
          text(workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY), SUMMARY_SHOP_ROW, VALUE));
    }
  }

  @Test
  void process_NoInvoices_SkipsListQuery_AndWritesZeroReport_TC004() throws IOException {
    LocalDate day = LocalDate.of(2026, 11, 7);
    setRange(day, day);
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(
            null, shopId, null, day.atStartOfDay(), day.atTime(LocalTime.MAX), "MANAGER", shopId))
        .thenReturn(0L);

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    verify(exportRevenueMapper, never())
        .getRevenueForExport(any(), any(), any(), any(), any(), any(), any(), any(), any());
    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals(0, number(summary, SUMMARY_COUNT_ROW, VALUE));
      assertEquals("07/11/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW, 1));
      assertEquals(0, number(summary, SUMMARY_FIRST_PERIOD_ROW, 3));
      assertEquals(0, workbook.getSheet(RevenueExcelWriter.SHEET_INVOICES).getLastRowNum() - 1);
      assertEquals(0, workbook.getSheet(RevenueExcelWriter.SHEET_ITEMS).getLastRowNum());
    }
  }

  @Test
  void process_YearRange_SummarisesTwelveMonths_TC005() throws IOException {
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 12, 31);
    setRange(start, end);
    stubShopPermission(shopId, "MANAGER");
    stubRows(
        "MANAGER",
        shopId,
        start.atStartOfDay(),
        end.atTime(LocalTime.MAX),
        List.of(invoice("IV-0001", LocalDateTime.of(2026, 3, 15, 9, 0), "80000")));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Tháng", text(summary, SUMMARY_HEADER_ROW, 1));
      assertEquals("01/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW, 1));
      assertEquals(80000, number(summary, SUMMARY_FIRST_PERIOD_ROW + 2, 3));
      assertEquals("12/2026", shown(summary, SUMMARY_FIRST_PERIOD_ROW + 11, 1));
      assertEquals("Tổng cộng", text(summary, SUMMARY_FIRST_PERIOD_ROW + 12, 0));
    }
  }

  @Test
  void process_PassesFiltersAndSortToMapper_TC006() {
    UUID invoiceId = UUID.randomUUID();
    request.setSearch("  Cà Phê ");
    request.setInvoiceId(invoiceId);
    request.setSortBy("totalAmount");
    request.setSortDirection(SortDirection.ASC);
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(
            "Cà Phê", shopId, invoiceId, null, null, "MANAGER", shopId))
        .thenReturn(1L);
    when(exportRevenueMapper.getRevenueForExport(
            "Cà Phê", shopId, invoiceId, null, null, "totalAmount", "ASC", "MANAGER", shopId))
        .thenReturn(List.of(invoice("IV-0001", LocalDateTime.of(2026, 11, 1, 8, 0), "10000")));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    assertTrue(result.getContent().length > 0);
    verify(exportRevenueMapper)
        .getRevenueForExport(
            "Cà Phê", shopId, invoiceId, null, null, "totalAmount", "ASC", "MANAGER", shopId);
  }

  @Test
  void process_MapperReturnsNull_TreatedAsEmpty_TC007() throws IOException {
    stubShopPermission(shopId, "MANAGER");
    stubRows("MANAGER", shopId, null, null, null);

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    try (Workbook workbook = open(result)) {
      assertEquals(
          0, number(workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY), SUMMARY_COUNT_ROW, VALUE));
    }
  }

  @Test
  void process_OnlyStartDate_MonthlySummaryAndOpenEndFileName_TC008() throws IOException {
    request.setStartDate(LocalDate.of(2026, 11, 1));
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(
            null, shopId, null, LocalDateTime.of(2026, 11, 1, 0, 0), null, "MANAGER", shopId))
        .thenReturn(0L);

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    assertEquals("doanh-thu_20261101_hientai.xlsx", result.getFileName());
    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Kỳ báo cáo: từ 01/11/2026", text(summary, SUMMARY_PERIOD_ROW, 0));
      assertEquals("Tháng", text(summary, SUMMARY_HEADER_ROW, 1));
    }
  }

  @Test
  void process_OnlyEndDate_OpenStartFileName_TC009() throws IOException {
    LocalDate end = LocalDate.of(2026, 11, 30);
    request.setEndDate(end);
    request.setSearch("Bàn");
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(
            "Bàn", shopId, null, null, end.atTime(LocalTime.MAX), "MANAGER", shopId))
        .thenReturn(0L);

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    assertEquals("doanh-thu_batdau_20261130.xlsx", result.getFileName());
    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Kỳ báo cáo: đến 30/11/2026", text(summary, SUMMARY_PERIOD_ROW, 0));
      assertEquals("Bàn", text(summary, SUMMARY_SEARCH_ROW, VALUE));
    }
  }

  @Test
  void process_InvoiceWithMissingFields_WritesPlaceholders_TC010() throws IOException {
    RevenueResult sparse = new RevenueResult();
    sparse.setInvoiceId(UUID.randomUUID());
    var emptyDetail = new RevenueResult.RevenueItemResult();
    var item = new RevenueResult.RevenueItemResult();
    item.setInvoiceDetailId(UUID.randomUUID());
    List<RevenueResult.RevenueItemResult> details = new ArrayList<>();
    details.add(null);
    details.add(emptyDetail);
    details.add(item);
    sparse.setItems(details);
    RevenueResult noItems = invoice("IV-0002", null, "1000");
    noItems.setItems(null);
    stubShopPermission(shopId, "MANAGER");
    stubRows("MANAGER", shopId, null, null, List.of(sparse, noItems));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    try (Workbook workbook = open(result)) {
      Sheet invoices = workbook.getSheet(RevenueExcelWriter.SHEET_INVOICES);
      assertEquals("—", text(invoices, 1, 1));
      assertEquals("—", text(invoices, 1, 3));
      assertEquals("—", text(invoices, 1, 4));
      assertEquals("—", text(invoices, 1, 5));
      assertEquals(0, number(invoices, 1, 6));
      assertEquals(1000, number(invoices, 3, 6));
      // Null and empty detail rows are skipped; the invoice without items adds no row
      Sheet items = workbook.getSheet(RevenueExcelWriter.SHEET_ITEMS);
      assertEquals(2, items.getLastRowNum()); // 1 drink + total row
      Row row = items.getRow(1);
      assertEquals("—", row.getCell(3).getStringCellValue());
      assertEquals(0, row.getCell(5).getNumericCellValue());
      assertEquals("", row.getCell(8).getStringCellValue());
      // Invoices without a date are counted in the total but not in any period
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals(2, number(summary, SUMMARY_COUNT_ROW, VALUE));
      assertEquals("Tổng cộng", text(summary, SUMMARY_FIRST_PERIOD_ROW, 0));
    }
  }

  @Test
  void process_AcceptsExactlyMaxInvoices_TC011() {
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn((long) ExportRevenueServiceImpl.MAX_EXPORT_INVOICES);
    when(exportRevenueMapper.getRevenueForExport(
            null, shopId, null, null, null, "createdAt", "DESC", "MANAGER", shopId))
        .thenReturn(List.of());

    assertNotNull(service.process(request, "MANAGER", shopId, userId).getContent());
  }

  @Test
  void process_InvoiceWithSeveralDrinks_MergesInvoiceCells_TC012() throws IOException {
    RevenueResult multi = invoice("IV-0001", LocalDateTime.of(2026, 11, 3, 9, 0), "87000");
    var tea = new RevenueResult.RevenueItemResult();
    tea.setInvoiceDetailId(UUID.randomUUID());
    tea.setDrinkName("Trà Đào");
    tea.setSize("L");
    tea.setQuantity(1);
    tea.setUnitPrice(new BigDecimal("29000"));
    multi.getItems().add(tea);
    RevenueResult single = invoice("IV-0002", LocalDateTime.of(2026, 11, 3, 10, 0), "58000");
    stubShopPermission(shopId, "MANAGER");
    stubRows("MANAGER", shopId, null, null, List.of(multi, single));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    try (Workbook workbook = open(result)) {
      Sheet items = workbook.getSheet(RevenueExcelWriter.SHEET_ITEMS);
      // Invoice code, time and shop of IV-0001 span its 2 drink rows (rows 2-3 in Excel)
      List<CellRangeAddress> merged = items.getMergedRegions();
      for (int column = 0; column <= 2; column++) {
        assertTrue(merged.contains(new CellRangeAddress(1, 2, column, column)), "column " + column);
      }
      assertEquals("IV-0001", text(items, 1, 0));
      assertEquals("", text(items, 2, 0));
      assertEquals("03/11/2026 09:00", shown(items, 1, 1));
      assertEquals("Trà Đào", text(items, 2, 3));
      assertEquals(29000, number(items, 2, 7));
      // IV-0002 has one drink: not merged
      assertEquals("IV-0002", text(items, 3, 0));
      assertFalse(merged.stream().anyMatch(range -> range.isInRange(3, 0)));
      assertEquals(145000, number(items, 4, 7));
    }
  }

  @Test
  void process_ManagerRequestingOtherShopOrAllShops_ExportsOnlyAssignedShop_TC013()
      throws IOException {
    // A manager tampering with the payload: another shop's id, then no shop at all ("all shops")
    for (UUID requestedShop : new UUID[] {otherShopId, null}) {
      request.setShopId(requestedShop);
      when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
      when(exportRevenueMapper.getShopName(shopId)).thenReturn("Chi nhánh 1");
      when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
          .thenReturn(0L);

      RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

      // Filter forced to the manager's own shop; the requested shop is never touched
      verify(getRevenueMapper, atLeastOnce())
          .countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId);
      verify(commonMapper, never()).checkShopExisted(otherShopId);
      verify(exportRevenueMapper, never()).getShopName(otherShopId);
      verify(getRevenueMapper, never())
          .countRevenueFiltered(any(), isNull(), any(), any(), any(), any(), any());
      try (Workbook workbook = open(result)) {
        assertEquals(
            "Chi nhánh 1",
            text(workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY), SUMMARY_SHOP_ROW, VALUE));
      }
    }
  }

  @Test
  void process_BlankNames_WritesPlaceholdersAndSkipsSignatureName_TC014() throws IOException {
    RevenueResult blank = invoice("IV-0001", LocalDateTime.of(2026, 11, 2, 9, 30), "58000");
    blank.setShopName("  ");
    blank.setStaffName("");
    stubShopPermission(shopId, "MANAGER");
    when(exportRevenueMapper.getShopName(shopId)).thenReturn("  ");
    when(exportRevenueMapper.getProfileName(userId)).thenReturn("   ");
    stubRows("MANAGER", shopId, null, null, List.of(blank));

    RevenueExportResult result = service.process(request, "MANAGER", shopId, userId);

    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals("Tất cả", text(summary, SUMMARY_SHOP_ROW, VALUE));
      Sheet invoices = workbook.getSheet(RevenueExcelWriter.SHEET_INVOICES);
      assertEquals("—", text(invoices, 1, 2));
      assertEquals("—", text(invoices, 1, 3));
      // Period row, total row, then note + date + title + hint: no row for the signer's name
      assertEquals(SUMMARY_FIRST_PERIOD_ROW + 7, summary.getLastRowNum());
    }
  }

  @Test
  void process_ThrowsUncheckedIOException_WhenWorkbookCannotBeWritten_TC015() {
    RevenueExcelWriter failingWriter =
        new RevenueExcelWriter() {
          @Override
          protected byte[] serialize(XSSFWorkbook workbook) throws IOException {
            throw new IOException("disk full");
          }
        };
    ExportRevenueServiceImpl failingService =
        new ExportRevenueServiceImpl(
            exportRevenueMapper,
            getRevenueMapper,
            new RevenueFilterResolver(new InvoiceValidator(createInvoiceMapper, commonMapper)),
            failingWriter);
    stubShopPermission(shopId, "MANAGER");
    stubRows(
        "MANAGER",
        shopId,
        null,
        null,
        List.of(invoice("IV-0001", LocalDateTime.of(2026, 11, 2, 9, 30), "58000")));

    var ex =
        assertThrows(
            UncheckedIOException.class,
            () -> failingService.process(request, "MANAGER", shopId, userId));

    assertEquals("Cannot create the revenue report file", ex.getMessage());
  }

  @Test
  void process_OwnerWithoutUserId_SkipsSignatureNameLookup_TC016() throws IOException {
    stubRows("OWNER", null, null, null, List.of(invoice("IV-0001", null, "58000")));

    RevenueExportResult result = service.process(request, "OWNER", shopId, null);

    verify(exportRevenueMapper, never()).getProfileName(any());
    try (Workbook workbook = open(result)) {
      Sheet summary = workbook.getSheet(RevenueExcelWriter.SHEET_SUMMARY);
      assertEquals(1, number(summary, SUMMARY_COUNT_ROW, VALUE));
    }
  }

  // =========================================================================
  // FAILURES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequest_WhenTooManyInvoices_TC017() {
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(ExportRevenueServiceImpl.MAX_EXPORT_INVOICES + 1L);

    var ex =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, "MANAGER", shopId, userId));

    assertTrue(ex.getMessage().startsWith("Too many invoices to export"));
    verify(exportRevenueMapper, never())
        .getRevenueForExport(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"STAFF", "CUSTOMER", ""})
  void process_RejectsRolesOtherThanOwnerAndManager_TC018(String role) {
    var ex =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, role, shopId, userId));

    assertEquals("You do not have permission to access revenue", ex.getMessage());
    verifyNoInteractions(exportRevenueMapper, commonMapper);
  }

  @Test
  void process_RejectsManagerWithoutAssignedShop_TC019() {
    var ex =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, "MANAGER", null, userId));

    assertEquals(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP, ex.getMessage());
    verifyNoInteractions(exportRevenueMapper);
  }

  @Test
  void process_RejectsManagerWithoutActiveMembership_TC020() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    var ex =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, "MANAGER", shopId, userId));

    assertEquals(ErrorMessage.SHOP_ACCESS_DENIED, ex.getMessage());
    verifyNoInteractions(exportRevenueMapper);
  }

  @Test
  void process_RejectsMissingOrDeletedShop_TC021() {
    request.setShopId(otherShopId);
    when(commonMapper.checkShopExisted(otherShopId)).thenReturn(false);

    assertThrows(
        DataNotFoundException.class, () -> service.process(request, "OWNER", shopId, userId));
    verifyNoInteractions(exportRevenueMapper);
  }

  @Test
  void process_RejectsStartDateAfterEndDate_TC022() {
    setRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1));
    stubShopPermission(shopId, "MANAGER");

    var ex =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, "MANAGER", shopId, userId));

    assertEquals("startDate must not be after endDate", ex.getMessage());
    verifyNoInteractions(exportRevenueMapper);
  }
}
