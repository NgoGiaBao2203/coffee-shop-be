package coffee.api.services.services_implement.revenue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.revenue.GetRevenueResponse;
import coffee.api.dto.result.RevenueResult;
import coffee.api.enums.SortDirection;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.GetRevenueMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import coffee.api.services.services_implement.revenue.common.RevenueFilterResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetRevenueServiceImplTest {

  @Mock private GetRevenueMapper getRevenueMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;

  private GetRevenueServiceImpl service;
  private static ValidatorFactory validatorFactory;
  private static Validator validator;
  private SearchRevenueRequest request;
  private RevenueResult sample;
  private UUID shopId;
  private UUID otherShopId;
  private UUID userId;

  @BeforeAll
  static void initValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @BeforeEach
  void setUp() {
    service =
        new GetRevenueServiceImpl(
            getRevenueMapper,
            new RevenueFilterResolver(new InvoiceValidator(createInvoiceMapper, commonMapper)));
    shopId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000402");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000403");
    request = new SearchRevenueRequest();

    sample = new RevenueResult();
    sample.setInvoiceId(UUID.fromString("00000000-0000-0000-0000-000000000404"));
    sample.setInvoiceCode("IV-ABCD1234");
    sample.setShopId(shopId);
    sample.setShopName("Coffee Shop - Chi nhánh 1");
    sample.setStaffName("Employee One");
    sample.setTotalAmount(new BigDecimal("99000.00"));
    var item = new RevenueResult.RevenueItemResult();
    item.setDrinkName("Cà Phê Sữa");
    item.setQuantity(1);
    item.setUnitPrice(new BigDecimal("29000.00"));
    sample.setItems(List.of(item));
  }

  private void stubShopPermission(UUID shop, String role) {
    when(commonMapper.checkShopExisted(shop)).thenReturn(true);
    if (!"OWNER".equalsIgnoreCase(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shop)).thenReturn(true);
    }
  }

  private void stubResults(String role, UUID target, long count, List<RevenueResult> rows) {
    when(getRevenueMapper.countRevenueFiltered(null, target, null, null, null, role, shopId))
        .thenReturn(count);
    when(getRevenueMapper.getRevenueFiltered(
            null, target, null, null, null, "createdAt", "DESC", 10, 0, role, shopId))
        .thenReturn(rows);
    when(getRevenueMapper.getRevenueTotal(null, target, null, null, null, role, shopId))
        .thenReturn(new BigDecimal("99000.00"));
  }

  private GetRevenueResponse process(String role) {
    return service.process(request, role, shopId, userId);
  }

  private void assertSingleViolation(String field, String message) {
    var violations = validator.validate(request);
    assertEquals(1, violations.size(), violations.toString());
    var violation = violations.iterator().next();
    assertEquals(field, violation.getPropertyPath().toString());
    assertEquals(message, violation.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS AND EMPTY RESULTS
  // =========================================================================

  @Test
  void process_ManagerSuccess_UsesAssignedShop_TC001() {
    stubShopPermission(shopId, "MANAGER");
    stubResults("MANAGER", shopId, 1, List.of(sample));

    var response = process("MANAGER");

    assertEquals("Get revenue statistics successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals("Cà Phê Sữa", response.getItems().getFirst().getItems().getFirst().getDrinkName());
    assertEquals("IV-ABCD1234", response.getItems().getFirst().getInvoiceCode());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());
    assertEquals(new BigDecimal("99000.00"), response.getTotalRevenue());
  }

  @Test
  void process_ManagerIgnoresRequestedShop_TC002() {
    request.setShopId(otherShopId);
    stubShopPermission(shopId, "MANAGER");
    stubResults("MANAGER", shopId, 1, List.of(sample));

    process("MANAGER");

    verify(commonMapper, never()).checkShopExisted(otherShopId);
    verify(getRevenueMapper)
        .countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId);
  }

  @Test
  void process_OwnerWithoutShopSeesAllShops_TC003() {
    stubResults("OWNER", null, 1, List.of(sample));

    var response = process("OWNER");

    assertEquals(1, response.getItems().size());
    verifyNoInteractions(commonMapper);
  }

  @Test
  void process_OwnerCanSelectAnyShop_TC004() {
    request.setShopId(otherShopId);
    stubShopPermission(otherShopId, "OWNER");
    stubResults("OWNER", otherShopId, 1, List.of(sample));

    process("OWNER");

    verify(commonMapper).checkShopExisted(otherShopId);
  }

  @Test
  void process_ReturnsEmptyPage_AndSkipsListQuery_WhenCountIsZero_TC005() {
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(0L);

    var response = process("MANAGER");

    assertEquals("No revenue records found", response.getMessage());
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
    assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotalRevenue()));
    verify(getRevenueMapper, never())
        .getRevenueTotal(any(), any(), any(), any(), any(), any(), any());
    verify(getRevenueMapper, never())
        .getRevenueFiltered(
            any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_ReturnsEmptyList_WhenMapperReturnsNull_TC006() {
    stubShopPermission(shopId, "MANAGER");
    stubResults("MANAGER", shopId, 3, null);

    var response = process("MANAGER");

    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals("No revenue records found", response.getMessage());
  }

  @Test
  void process_PassesValidatedFilters_AndCalculatesPagination_TC007() {
    UUID invoiceId = UUID.randomUUID();
    request.setSearch("  Cà Phê ");
    request.setInvoiceId(invoiceId);
    request.setStartDate(LocalDate.of(2026, 8, 1));
    request.setEndDate(LocalDate.of(2026, 8, 31));
    request.setPage(3);
    request.setSize(20);
    request.setSortBy("totalAmount");
    request.setSortDirection(SortDirection.ASC);
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
    LocalDateTime end = LocalDate.of(2026, 8, 31).atTime(LocalTime.MAX);
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(
            "Cà Phê", shopId, invoiceId, start, end, "MANAGER", shopId))
        .thenReturn(45L);
    when(getRevenueMapper.getRevenueFiltered(
            "Cà Phê",
            shopId,
            invoiceId,
            start,
            end,
            "totalAmount",
            "ASC",
            20,
            40,
            "MANAGER",
            shopId))
        .thenReturn(List.of(sample));
    when(getRevenueMapper.getRevenueTotal(
            "Cà Phê", shopId, invoiceId, start, end, "MANAGER", shopId))
        .thenReturn(new BigDecimal("580000.00"));

    var response = process("MANAGER");

    assertEquals(new BigDecimal("580000.00"), response.getTotalRevenue());
    assertEquals(3, response.getPagination().getPage());
    assertEquals(45L, response.getPagination().getTotalElements());
    assertEquals(3, response.getPagination().getTotalPages());
  }

  @Test
  void process_NullTotalResultFallsBackToZero_TC008() {
    stubShopPermission(shopId, "MANAGER");
    stubResults("MANAGER", shopId, 1, List.of(sample));
    when(getRevenueMapper.getRevenueTotal(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(null);

    var response = process("MANAGER");

    assertEquals(BigDecimal.ZERO, response.getTotalRevenue());
    assertEquals(1, response.getItems().size());
  }

  @Test
  void process_TotalCoversWholePeriod_NotOnlyCurrentPage_TC009() {
    request.setPage(2);
    request.setSize(1);
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(5L);
    when(getRevenueMapper.getRevenueFiltered(
            null, shopId, null, null, null, "createdAt", "DESC", 1, 1, "MANAGER", shopId))
        .thenReturn(List.of(sample));
    when(getRevenueMapper.getRevenueTotal(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(new BigDecimal("145000.00"));

    var response = process("MANAGER");

    assertEquals(1, response.getItems().size());
    assertEquals(new BigDecimal("145000.00"), response.getTotalRevenue());
    assertEquals(5L, response.getPagination().getTotalElements());
  }

  // =========================================================================
  // SERVICE PROCESS - PERMISSION AND VALIDATION FAILURES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"STAFF", "CUSTOMER", ""})
  void process_RejectsRolesOtherThanOwnerAndManager_TC010(String role) {
    var ex = assertThrows(InvalidRequestException.class, () -> process(role));

    assertEquals("You do not have permission to access revenue", ex.getMessage());
    verifyNoInteractions(getRevenueMapper, commonMapper);
  }

  @Test
  void process_RejectsManagerWithoutAssignedShop_TC011() {
    var ex =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, "MANAGER", null, userId));

    assertEquals(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP, ex.getMessage());
    verifyNoInteractions(getRevenueMapper);
  }

  @Test
  void process_RejectsMissingOrDeletedShop_TC012() {
    request.setShopId(otherShopId);
    when(commonMapper.checkShopExisted(otherShopId)).thenReturn(false);

    assertThrows(DataNotFoundException.class, () -> process("OWNER"));
    verifyNoInteractions(getRevenueMapper);
  }

  @Test
  void process_RejectsManagerWithoutActiveMembership_TC013() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    var ex = assertThrows(InvalidRequestException.class, () -> process("MANAGER"));

    assertEquals(ErrorMessage.SHOP_ACCESS_DENIED, ex.getMessage());
    verifyNoInteractions(getRevenueMapper);
  }

  @Test
  void process_RejectsMissingUserId_TC014() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    var ex =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, "MANAGER", shopId, null));

    assertEquals(ErrorMessage.SHOP_ACCESS_DENIED, ex.getMessage());
  }

  @Test
  void process_RejectsStartDateAfterEndDate_TC015() {
    request.setStartDate(LocalDate.of(2026, 9, 1));
    request.setEndDate(LocalDate.of(2026, 8, 1));
    stubShopPermission(shopId, "MANAGER");

    var ex = assertThrows(InvalidRequestException.class, () -> process("MANAGER"));

    assertEquals("startDate must not be after endDate", ex.getMessage());
    verifyNoInteractions(getRevenueMapper);
  }

  @Test
  void process_AcceptsSameStartAndEndDate_TC016() {
    request.setStartDate(LocalDate.of(2026, 8, 1));
    request.setEndDate(LocalDate.of(2026, 8, 1));
    stubShopPermission(shopId, "MANAGER");
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
    LocalDateTime end = LocalDate.of(2026, 8, 1).atTime(LocalTime.MAX);
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, start, end, "MANAGER", shopId))
        .thenReturn(0L);

    assertEquals("No revenue records found", process("MANAGER").getMessage());
  }

  @Test
  void process_PropagatesCountQueryFailure_TC017() {
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenThrow(new IllegalStateException("db down"));

    assertThrows(IllegalStateException.class, () -> process("MANAGER"));
  }

  @Test
  void process_PropagatesListQueryFailure_TC018() {
    stubShopPermission(shopId, "MANAGER");
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, null, "MANAGER", shopId))
        .thenReturn(1L);
    when(getRevenueMapper.getRevenueFiltered(
            null, shopId, null, null, null, "createdAt", "DESC", 10, 0, "MANAGER", shopId))
        .thenThrow(new IllegalStateException("db down"));

    assertThrows(IllegalStateException.class, () -> process("MANAGER"));
  }

  @Test
  void process_AcceptsOnlyStartDate_TC019() {
    request.setStartDate(LocalDate.of(2026, 8, 1));
    stubShopPermission(shopId, "MANAGER");
    LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, start, null, "MANAGER", shopId))
        .thenReturn(0L);

    assertEquals("No revenue records found", process("MANAGER").getMessage());
    verify(getRevenueMapper)
        .countRevenueFiltered(null, shopId, null, start, null, "MANAGER", shopId);
  }

  @Test
  void process_AcceptsOnlyEndDate_TC020() {
    request.setEndDate(LocalDate.of(2026, 8, 31));
    stubShopPermission(shopId, "MANAGER");
    LocalDateTime end = LocalDate.of(2026, 8, 31).atTime(LocalTime.MAX);
    when(getRevenueMapper.countRevenueFiltered(null, shopId, null, null, end, "MANAGER", shopId))
        .thenReturn(0L);

    assertEquals("No revenue records found", process("MANAGER").getMessage());
    verify(getRevenueMapper).countRevenueFiltered(null, shopId, null, null, end, "MANAGER", shopId);
  }

  // =========================================================================
  // REQUEST VALIDATION
  // =========================================================================

  @Test
  void requestValidation_AcceptsDefaults_TC021() {
    assertTrue(validator.validate(request).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"invoiceId", "shopName", "staffName", "createdAt", "totalAmount", "tableNumber"})
  void requestValidation_AcceptsSupportedSortField_TC022(String field) {
    request.setSortBy(field);
    assertTrue(validator.validate(request).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"status", "fullName", "drinkName", "price", "createdAt; DROP TABLE invoices", ""})
  void requestValidation_RejectsUnsupportedSortField_TC023(String sortBy) {
    request.setSortBy(sortBy);
    assertSingleViolation("sortBy", ValidationMessage.SORT_BY_INVALID);
  }

  @Test
  void requestValidation_RejectsNullSortFieldAndDirection_TC024() {
    request.setSortBy(null);
    request.setSortDirection(null);

    var violations = validator.validate(request);

    assertEquals(2, violations.size());
    violations.forEach(v -> assertEquals(ValidationMessage.FIELD_REQUIRED, v.getMessage()));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void requestValidation_RejectsInvalidPage_TC025(int page) {
    request.setPage(page);
    assertSingleViolation("page", ValidationMessage.PAGE_MIN);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 100})
  void requestValidation_AcceptsSizeBoundary_TC026(int size) {
    request.setSize(size);
    assertTrue(validator.validate(request).isEmpty());
  }

  @Test
  void requestValidation_RejectsSmallAndLargeSize_TC027() {
    request.setSize(0);
    assertSingleViolation("size", ValidationMessage.SIZE_MIN);
    request.setSize(101);
    assertSingleViolation("size", ValidationMessage.SIZE_MAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cà phê", "Nguyễn Văn A", "7c9e6679-7425-40de-944b-e07fc1f90ae7", "  "})
  void requestValidation_AcceptsSearch_TC028(String search) {
    request.setSearch(search);
    assertTrue(validator.validate(request).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"100%", "a_b", "<script>", "x; DROP TABLE"})
  void requestValidation_RejectsUnsupportedSearch_TC029(String search) {
    request.setSearch(search);
    assertSingleViolation("search", ValidationMessage.SPECIAL_CHARACTERS);
  }

  @Test
  void requestValidation_SearchLengthBoundary_TC030() {
    request.setSearch("a".repeat(100));
    assertTrue(validator.validate(request).isEmpty());
    request.setSearch("a".repeat(101));
    assertSingleViolation("search", ValidationMessage.SIZE_MAX);
  }

  @Test
  void requestValidation_BlankDatesAreTreatedAsNoFilter_TC031() {
    request.setStartDate("");
    request.setEndDate("  ");
    assertNull(request.getStartDate());
    assertNull(request.getEndDate());

    request.setStartDate("2026-11-01");
    assertEquals(LocalDate.of(2026, 11, 1), request.getStartDate());
  }

  @Test
  void requestValidation_RejectsMalformedDate_TC032() {
    assertThrows(
        java.time.format.DateTimeParseException.class, () -> request.setStartDate("02/11/2026"));
  }

  // =========================================================================
  // REQUEST HELPERS
  // =========================================================================

  @Test
  void totalPages_HandlesEmptyExactAndPartialPages_TC033() {
    request.setSize(10);
    assertEquals(0, request.totalPages(0));
    assertEquals(1, request.totalPages(10));
    assertEquals(2, request.totalPages(11));
  }

  @Test
  void calcOffset_HandlesFirstAndLaterPages_TC034() {
    request.setSize(10);
    request.setPage(1);
    assertEquals(0, request.calcOffset());
    request.setPage(4);
    assertEquals(30, request.calcOffset());
  }
}
