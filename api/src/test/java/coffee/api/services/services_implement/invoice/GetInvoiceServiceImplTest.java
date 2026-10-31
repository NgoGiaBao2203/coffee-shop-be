package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.GetInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class GetInvoiceServiceImplTest {
  @Mock private GetInvoiceMapper getInvoiceMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;
  private GetInvoiceServiceImpl service;
  private static ValidatorFactory validatorFactory;
  private static Validator validator;
  private SearchInvoiceRequest request;
  private InvoiceResult sampleInvoice;
  private UUID shopId;
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
        new GetInvoiceServiceImpl(
            getInvoiceMapper, new InvoiceValidator(createInvoiceMapper, commonMapper));
    shopId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000302");
    request = new SearchInvoiceRequest();
    request.setShopId(shopId);
    sampleInvoice = new InvoiceResult();
    sampleInvoice.setInvoiceId(UUID.fromString("00000000-0000-0000-0000-000000000303"));
    sampleInvoice.setShopId(shopId);
    sampleInvoice.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000304"));
    sampleInvoice.setStatus("0");
    sampleInvoice.setTotalAmount(new BigDecimal("50000.00"));
    var item = new InvoiceResult.InvoiceItemResult();
    item.setDrinkDetailId(UUID.fromString("00000000-0000-0000-0000-000000000305"));
    item.setDrinkName("Cà phê muối");
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("25000.00"));
    sampleInvoice.setItems(List.of(item));
  }

  private void stubShopPermission(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (!"OWNER".equalsIgnoreCase(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    }
  }

  private void stubResults(String role, long count, List<InvoiceResult> invoices) {
    String roleName = role.toUpperCase(Locale.ROOT);
    when(getInvoiceMapper.countInvoicesFiltered(null, roleName, shopId, shopId, null))
        .thenReturn(count);
    when(getInvoiceMapper.getInvoicesFiltered(
            null, "createdAt", "DESC", 10, 0L, roleName, shopId, shopId, null))
        .thenReturn(invoices);
  }

  private coffee.api.dto.response.base_response.PageResponse<InvoiceResult> process(String role) {
    return service.process(request, role, shopId, userId);
  }

  private void verifyNoInvoiceQuery() {
    verifyNoInteractions(getInvoiceMapper, createInvoiceMapper);
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

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF", "owner", "manager", "staff"})
  void process_Success_ForSupportedRoles_TC001(String role) {
    // 1. Prepare valid permission and invoice data
    stubShopPermission(role);
    stubResults(role, 1L, List.of(sampleInvoice));

    // 2. Fetch invoices in the requested shop
    var response = process(role);

    // 3. Verify status, pagination and nested invoice data
    assertEquals("S0001", response.getCode());
    assertEquals("Get invoices successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals("PENDING", response.getItems().get(0).getStatus());
    assertEquals(sampleInvoice.getTableId(), response.getItems().get(0).getTableId());
    assertEquals("Cà phê muối", response.getItems().get(0).getItems().get(0).getDrinkName());
    assertEquals(new BigDecimal("50000.00"), response.getItems().get(0).getTotalAmount());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());
    verifyNoInteractions(createInvoiceMapper);
  }

  @Test
  void process_ReturnsEmptyPage_WhenCountIsZero_TC002() {
    stubShopPermission("STAFF");

    var response = process("STAFF");

    assertEquals(0, response.getItems().size());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
    verify(getInvoiceMapper, never())
        .getInvoicesFiltered(any(), any(), any(), anyInt(), anyLong(), any(), any(), any(), any());
  }

  @Test
  void process_ReturnsEmptyList_WhenMapperReturnsNull_TC003() {
    stubShopPermission("OWNER");
    stubResults("OWNER", 1L, null);

    var response = process("OWNER");

    assertEquals(0, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());
  }

  @Test
  void process_AcceptsEmptyMapperList_WithPositiveCount_TC004() {
    stubShopPermission("OWNER");
    stubResults("OWNER", 1L, List.of());

    assertEquals(0, process("OWNER").getItems().size());
  }

  @Test
  void process_PassesValidatedFilters_AndCalculatesPagination_TC005() {
    stubShopPermission("MANAGER");
    request.setSearch("  Cà phê muối  ");
    request.setStatus(1);
    request.setPage(3);
    request.setSize(5);
    request.setSortBy("tableNumber");
    request.setSortDirection(SortDirection.ASC);
    when(getInvoiceMapper.countInvoicesFiltered("Cà phê muối", "MANAGER", shopId, shopId, 1))
        .thenReturn(12L);
    when(getInvoiceMapper.getInvoicesFiltered(
            "Cà phê muối", "tableNumber", "ASC", 5, 10L, "MANAGER", shopId, shopId, 1))
        .thenReturn(List.of(sampleInvoice));

    var response = process("MANAGER");

    assertEquals(12L, response.getPagination().getTotalElements());
    assertEquals(3, response.getPagination().getTotalPages());
    assertEquals(3, response.getPagination().getPage());
    assertEquals(5, response.getPagination().getSize());
    assertEquals("  Cà phê muối  ", request.getSearch());
  }

  @Test
  void process_OwnerCanSelectShopWithoutAssignment_TC006() {
    stubShopPermission("OWNER");

    var response = service.process(request, "OWNER", null, userId);

    assertEquals(0, response.getItems().size());
    verify(getInvoiceMapper).countInvoicesFiltered(null, "OWNER", null, shopId, null);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_OwnerCanSelectAnotherShop_TC007() {
    stubShopPermission("OWNER");
    UUID assignedShop = UUID.fromString("00000000-0000-0000-0000-000000000306");

    service.process(request, "OWNER", assignedShop, userId);

    verify(getInvoiceMapper).countInvoicesFiltered(null, "OWNER", assignedShop, shopId, null);
  }

  @Test
  void process_NormalizesAllInvoiceStatuses_TC008() {
    stubShopPermission("OWNER");
    var invoices =
        java.util.Arrays.asList("0", "1", "2", null, "99").stream()
            .map(
                status -> {
                  var invoice = new InvoiceResult();
                  invoice.setStatus(status);
                  return invoice;
                })
            .toList();
    stubResults("OWNER", 5L, invoices);

    var response = process("OWNER");

    assertEquals(
        List.of("PENDING", "COMPLETED", "CANCELLED", "Không xác định", "UNDEFINED"),
        response.getItems().stream().map(InvoiceResult::getStatus).toList());
  }

  // =========================================================================
  // SERVICE PROCESS - ROLE, SHOP AND MEMBERSHIP ERRORS
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_AcceptsMissingShopId_TC009(String role) {
    request.setShopId(null);
    if (!"OWNER".equals(role)) {
      stubShopPermission(role);
    }

    var response = process(role);

    assertEquals(0, response.getItems().size());
    verify(getInvoiceMapper)
        .countInvoicesFiltered(null, role, shopId, "OWNER".equals(role) ? null : shopId, null);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ADMIN", "CASHIER", ""})
  @NullSource
  void process_RejectsUnsupportedRole_TC010(String role) {
    var error = assertThrows(InvalidRequestException.class, () -> process(role));
    assertEquals("You do not have permission to access invoices", error.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInvoiceQuery();
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_RejectsMissingAssignedShop_TC011(String role) {
    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, role, null, userId));
    assertEquals("User is not assigned to any shop", error.getMessage());
    verifyNoInvoiceQuery();
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_UsesAssignedShopInsteadOfRequestedShop_TC012(String role) {
    request.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000307"));
    stubShopPermission(role);
    stubResults(role, 1L, List.of(sampleInvoice));

    var response = process(role);

    assertEquals(shopId, response.getItems().get(0).getShopId());
    verify(getInvoiceMapper).countInvoicesFiltered(null, role, shopId, shopId, null);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void process_RejectsMissingOrDeletedShop_TC013(boolean nullResult) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(nullResult ? null : false);

    var error = assertThrows(DataNotFoundException.class, () -> process("OWNER"));

    assertEquals(shopId, error.getId());
    verifyNoInvoiceQuery();
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_RejectsMissingUserId_TC014(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, role, shopId, null));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInvoiceQuery();
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_RejectsMissingMembership_TC015(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    var error = assertThrows(InvalidRequestException.class, () -> process(role));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInvoiceQuery();
  }

  @Test
  void process_RejectsNullMembership_TC016() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(null);

    var error = assertThrows(InvalidRequestException.class, () -> process("STAFF"));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInvoiceQuery();
  }

  // =========================================================================
  // SERVICE PROCESS - DATABASE ERRORS
  // =========================================================================

  @Test
  void process_PropagatesCountQueryFailure_TC017() {
    stubShopPermission("OWNER");
    when(getInvoiceMapper.countInvoicesFiltered(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Count query failed") {});

    var error = assertThrows(DataAccessException.class, () -> process("OWNER"));

    assertEquals("Count query failed", error.getMessage());
    verify(getInvoiceMapper, never())
        .getInvoicesFiltered(any(), any(), any(), anyInt(), anyLong(), any(), any(), any(), any());
  }

  @Test
  void process_PropagatesListQueryFailure_TC018() {
    stubShopPermission("OWNER");
    when(getInvoiceMapper.countInvoicesFiltered(null, "OWNER", shopId, shopId, null))
        .thenReturn(1L);
    when(getInvoiceMapper.getInvoicesFiltered(
            null, "createdAt", "DESC", 10, 0L, "OWNER", shopId, shopId, null))
        .thenThrow(new DataAccessException("List query failed") {});

    var error = assertThrows(DataAccessException.class, () -> process("OWNER"));
    assertEquals("List query failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - SUCCESS CASES
  // =========================================================================

  @Test
  void requestValidation_AcceptsDefaultsWithShopId_TC019() {
    assertEquals(0, validator.validate(request).size());
    assertEquals(1, request.getPage());
    assertEquals(10, request.getSize());
    assertEquals("createdAt", request.getSortBy());
    assertEquals(SortDirection.DESC, request.getSortDirection());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  @NullSource
  void requestValidation_AcceptsSupportedStatus_TC020(Integer status) {
    request.setStatus(status);
    assertEquals(0, validator.validate(request).size());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invoiceId",
        "totalAmount",
        "status",
        "tableNumber",
        "createdAt",
        "updateAt",
        "isDeleted",
        "shopName",
        "staffName"
      })
  void requestValidation_AcceptsSupportedSortField_TC021(String field) {
    request.setSortBy(field);
    assertEquals(0, validator.validate(request).size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ASC", "DESC"})
  void requestValidation_AcceptsSortDirection_TC022(String direction) {
    request.setSortDirection(SortDirection.valueOf(direction));
    assertEquals(0, validator.validate(request).size());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "  ",
        "Cà phê muối",
        "Bàn 1",
        "11111111-1111-1111-1111-111111111111",
        "Shop A - B & C / (D), E.F 'G'"
      })
  @NullSource
  void requestValidation_AcceptsSearch_TC023(String search) {
    request.setSearch(search);
    assertEquals(0, validator.validate(request).size());
  }

  @Test
  void requestValidation_AcceptsSearchAt100Characters_TC024() {
    request.setSearch("A".repeat(100));
    assertEquals(0, validator.validate(request).size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 100})
  void requestValidation_AcceptsSizeBoundary_TC025(int size) {
    request.setSize(size);
    assertEquals(0, validator.validate(request).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - FAILED CASES
  // =========================================================================

  @Test
  void requestValidation_AcceptsMissingShopId_TC026() {
    request.setShopId(null);
    assertEquals(0, validator.validate(request).size());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 3, 99})
  void requestValidation_RejectsInvalidStatus_TC027(int status) {
    request.setStatus(status);
    assertSingleViolation("status", ValidationMessage.Msg.STATUS_INVALID);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void requestValidation_RejectsInvalidPage_TC028(int page) {
    request.setPage(page);
    assertSingleViolation("page", ValidationMessage.Msg.PAGE_MIN);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void requestValidation_RejectsSmallPageSize_TC029(int size) {
    request.setSize(size);
    assertSingleViolation("size", ValidationMessage.Msg.SIZE_MIN);
  }

  @Test
  void requestValidation_RejectsLargePageSize_TC030() {
    request.setSize(101);
    assertSingleViolation("size", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_RejectsLongSearch_TC031() {
    request.setSearch("A".repeat(101));
    assertSingleViolation("search", ValidationMessage.Msg.SIZE_MAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"%", "_", "<script>", "A; DROP TABLE invoices", "☕"})
  void requestValidation_RejectsUnsupportedSearch_TC032(String search) {
    request.setSearch(search);
    assertSingleViolation("search", ValidationMessage.Msg.SPECIAL_CHARACTERS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "created_at", "totalAmount DESC", "invoiceId;DROP", "tableId"})
  void requestValidation_RejectsUnsupportedSortField_TC033(String sortBy) {
    request.setSortBy(sortBy);
    assertSingleViolation("sortBy", ValidationMessage.Msg.SORT_BY_INVALID);
  }

  @Test
  void requestValidation_RejectsNullSortField_TC034() {
    request.setSortBy(null);
    assertSingleViolation("sortBy", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_RejectsNullSortDirection_TC035() {
    request.setSortDirection(null);
    assertSingleViolation("sortDirection", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  // =========================================================================
  // REQUEST HELPERS - TRIMMING AND PAGINATION
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "\t\n"})
  @NullSource
  void trimmedSearch_ReturnsNullForBlankInput_TC036(String search) {
    request.setSearch(search);
    assertNull(request.trimmedSearch());
  }

  @Test
  void trimmedSearch_RemovesSurroundingWhitespace_TC037() {
    request.setSearch("  Bàn 12  ");
    assertEquals("Bàn 12", request.trimmedSearch());
  }

  @Test
  void calcOffset_HandlesFirstPageAndLargePageNumbers_TC038() {
    assertEquals(0L, request.calcOffset());
    request.setPage(3);
    request.setSize(5);
    assertEquals(10L, request.calcOffset());
    request.setPage(Integer.MAX_VALUE);
    request.setSize(100);
    assertEquals(214748364600L, request.calcOffset());
    assertEquals(0, validator.validate(request).size());
  }

  @Test
  void totalPages_HandlesEmptyExactAndPartialPages_TC039() {
    assertEquals(0, request.totalPages(0));
    assertEquals(1, request.totalPages(1));
    assertEquals(1, request.totalPages(10));
    assertEquals(2, request.totalPages(11));
  }
}
