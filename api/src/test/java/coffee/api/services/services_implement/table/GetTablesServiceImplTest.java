package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.TableResult;
import coffee.api.enums.Roles;
import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetTablesMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class GetTablesServiceImplTest {

  @Mock private GetTablesMapper getTablesMapper;

  @InjectMocks private GetTablesServiceImpl getTablesService;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;
  private SearchTablesRequest validRequest;
  private TableResult sampleTableResult;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;

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
    currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000801");
    currentUserShopId = UUID.fromString("00000000-0000-0000-0000-000000000802");
    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();

    validRequest = new SearchTablesRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000803"));
    validRequest.setSearch("Table 1");
    validRequest.setStatus(1); // Available
    validRequest.setSortBy("tableNumber");
    validRequest.setSortDirection(SortDirection.ASC);

    sampleTableResult = new TableResult();
    sampleTableResult.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000804"));
    sampleTableResult.setShopId(currentUserShopId);
    sampleTableResult.setShopName("Hoa Yên Coffee");
    sampleTableResult.setTableNumber(1);
    sampleTableResult.setDescription("Table near window");
    sampleTableResult.setStatus(1);
    sampleTableResult.setCreatedAt("2026-10-08T10:15:30");
    sampleTableResult.setUpdatedAt("2026-10-08T10:15:30");
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<TableResult> expectedItems = Collections.singletonList(sampleTableResult);

    when(getTablesMapper.countTablesFiltered(
            search, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals("Get tables successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals(1, response.getItems().getFirst().getTableNumber());
    assertEquals("Available", response.getItems().getFirst().getStatusName());
    assertEquals("Hoa Yên Coffee", response.getItems().getFirst().getShopName());
    assertEquals("2026-10-08T10:15:30", response.getItems().getFirst().getCreatedAt());

    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getTablesMapper)
        .countTablesFiltered(search, status, managerRole, currentUserShopId, targetShopId);
    verify(getTablesMapper)
        .getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNull_TC002() {
    // Arrange: Owner does not belong to a specific shop and does not filter by target shop
    validRequest.setShopId(null);
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getTablesMapper.countTablesFiltered(search, status, ownerRole, null, null))
        .thenReturn(totalElements);
    when(getTablesMapper.getTablesFiltered(
            search, status, sortBy, sortDirection, size, offset, ownerRole, null, null))
        .thenReturn(Collections.singletonList(sampleTableResult));

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, ownerRole, null, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(1, response.getItems().size());
    assertEquals("2026-10-08T10:15:30", response.getItems().getFirst().getCreatedAt());

    verify(getTablesMapper).countTablesFiltered(search, status, ownerRole, null, null);
    verify(getTablesMapper)
        .getTablesFiltered(
            search, status, sortBy, sortDirection, size, offset, ownerRole, null, null);
  }

  @Test
  void process_Success_WhenUserIsStaff_MasksAuditTimestamps_TC003() {
    // Arrange: When STAFF views tables, audit timestamps must be nullified
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getTablesMapper.countTablesFiltered(
            search, status, staffRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);
    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            staffRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(Collections.singletonList(sampleTableResult));

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, staffRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(1, response.getItems().size());
    TableResult result = response.getItems().getFirst();
    assertEquals("Available", result.getStatusName());
    assertNull(result.getCreatedAt(), "Staff must not see createdAt timestamp");
    assertNull(result.getUpdatedAt(), "Staff must not see updatedAt timestamp");

    verify(getTablesMapper)
        .countTablesFiltered(search, status, staffRole, currentUserShopId, targetShopId);
    verify(getTablesMapper)
        .getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            staffRole,
            currentUserShopId,
            targetShopId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC004() {
    // Arrange
    validRequest.setSearch("NonExistentTable");
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getTablesMapper.countTablesFiltered(
            search, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(0, response.getItems().size());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC005() {
    // Arrange
    validRequest.setSearch("   ");
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getTablesMapper.countTablesFiltered(
            null, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            null,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(Collections.singletonList(sampleTableResult));

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(5L, response.getPagination().getTotalElements());

    verify(getTablesMapper)
        .countTablesFiltered(null, status, managerRole, currentUserShopId, targetShopId);
    verify(getTablesMapper)
        .getTablesFiltered(
            null,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId);
  }

  @Test
  void process_SuccessWhenMapperReturnsNullList_TC006() {
    // Arrange
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getTablesMapper.countTablesFiltered(
            search, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(null);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(0, response.getItems().size());
    assertEquals(0L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWhenSortDirectionIsNull_DefaultsToAsc_TC007() {
    // Arrange
    validRequest.setSortDirection(null);
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getTablesMapper.countTablesFiltered(
            search, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            "ASC",
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(Collections.singletonList(sampleTableResult));

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    verify(getTablesMapper)
        .getTablesFiltered(
            search,
            status,
            sortBy,
            "ASC",
            size,
            offset,
            managerRole,
            currentUserShopId,
            targetShopId);
  }

  @Test
  void process_SuccessForDifferentStatusMappings_TC008() {
    // Arrange
    TableResult occupiedTable = createTableResult(2, "Occupied Table", 2);
    TableResult reservedTable = createTableResult(3, "Reserved Table", 3);
    TableResult nullStatusTable = createTableResult(4, "Unknown Table", null);
    TableResult unrecognizedTable = createTableResult(5, "Invalid Status Table", 99);

    List<TableResult> items =
        List.of(occupiedTable, reservedTable, nullStatusTable, unrecognizedTable);

    when(getTablesMapper.countTablesFiltered(any(), any(), any(), any(), any())).thenReturn(4L);
    when(getTablesMapper.getTablesFiltered(
            any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
        .thenReturn(items);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals("S0001", response.getCode());
    assertEquals(4, response.getItems().size());
    assertEquals("Occupied", response.getItems().get(0).getStatusName());
    assertEquals("Reserved", response.getItems().get(1).getStatusName());
    assertEquals("Unknown", response.getItems().get(2).getStatusName());
    assertEquals("Unknown", response.getItems().get(3).getStatusName());
  }

  @Test
  void process_SuccessForLaterPageWithDescendingSort_TC009() {
    // Arrange
    validRequest.setPage(3);
    validRequest.setSize(25);
    validRequest.setSortDirection(SortDirection.DESC);
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    UUID targetShopId = validRequest.getShopId();

    when(getTablesMapper.countTablesFiltered(
            search, status, managerRole, currentUserShopId, targetShopId))
        .thenReturn(60L);
    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            validRequest.getSortBy(),
            "DESC",
            25,
            50,
            managerRole,
            currentUserShopId,
            targetShopId))
        .thenReturn(Collections.singletonList(sampleTableResult));

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, managerRole, currentUserShopId, currentUserId);

    // Assert
    assertEquals(3, response.getPagination().getPage());
    assertEquals(25, response.getPagination().getSize());
    assertEquals(60L, response.getPagination().getTotalElements());
    assertEquals(3, response.getPagination().getTotalPages());
    verify(getTablesMapper)
        .getTablesFiltered(
            search,
            status,
            validRequest.getSortBy(),
            "DESC",
            25,
            50,
            managerRole,
            currentUserShopId,
            targetShopId);
  }

  @Test
  void process_MasksAuditFieldsOnEveryStaffRow_TC010() {
    var first = createTableResult(1, "First table", 1);
    var second = createTableResult(2, "Second table", 2);
    when(getTablesMapper.countTablesFiltered(any(), any(), any(), any(), any())).thenReturn(2L);
    when(getTablesMapper.getTablesFiltered(
            any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
        .thenReturn(List.of(first, second));

    var response =
        getTablesService.process(validRequest, staffRole, currentUserShopId, currentUserId);

    assertEquals(
        List.of("Available", "Occupied"),
        response.getItems().stream().map(TableResult::getStatusName).toList());
    assertEquals(
        Collections.nCopies(2, null),
        response.getItems().stream().map(TableResult::getCreatedAt).toList());
    assertEquals(
        Collections.nCopies(2, null),
        response.getItems().stream().map(TableResult::getUpdatedAt).toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_PreservesAuditFieldsForOwnerAndManager_TC011(String role) {
    when(getTablesMapper.countTablesFiltered(any(), any(), any(), any(), any())).thenReturn(1L);
    when(getTablesMapper.getTablesFiltered(
            any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
        .thenReturn(List.of(sampleTableResult));

    var response = getTablesService.process(validRequest, role, currentUserShopId, currentUserId);

    assertEquals("2026-10-08T10:15:30", response.getItems().getFirst().getCreatedAt());
    assertEquals("2026-10-08T10:15:30", response.getItems().getFirst().getUpdatedAt());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PERMISSION & BUSINESS CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdIsNull_TC012() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getTablesService.process(validRequest, managerRole, null, currentUserId));

    assertEquals("User is not assigned to any shop branch", exception.getMessage());
    verify(getTablesMapper, never()).countTablesFiltered(any(), any(), any(), any(), any());
    verify(getTablesMapper, never())
        .getTablesFiltered(any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIdIsNull_TC013() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getTablesService.process(validRequest, staffRole, null, currentUserId));

    assertEquals("User is not assigned to any shop branch", exception.getMessage());
    verify(getTablesMapper, never()).countTablesFiltered(any(), any(), any(), any(), any());
    verify(getTablesMapper, never())
        .getTablesFiltered(any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseCountFails_TC014() {
    // Arrange
    when(getTablesMapper.countTablesFiltered(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                getTablesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Database query error", exception.getMessage());
    verify(getTablesMapper, never())
        .getTablesFiltered(any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenFetchingTablesFails_TC015() {
    // Arrange
    when(getTablesMapper.countTablesFiltered(any(), any(), any(), any(), any())).thenReturn(1L);
    when(getTablesMapper.getTablesFiltered(
            any(), any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                getTablesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Database query error", exception.getMessage());
    verify(getTablesMapper)
        .countTablesFiltered(
            validRequest.trimmedSearch(),
            validRequest.getStatus(),
            managerRole,
            currentUserShopId,
            validRequest.getShopId());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC016() {
    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsNull_TC017() {
    validRequest.setSearch(null);
    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchContainsVietnameseAndAllowedCharacters_TC018() {
    validRequest.setSearch("Bàn VIP (Cửa Sổ) - Khu A, Số 1. 'Espresso'");
    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"tableId", "tableNumber", "status", "shopId", "createdAt", "updatedAt"})
  void process_ValidationSuccess_WhenSortByContainsValidPatternValues_TC019(String validSortBy) {
    validRequest.setSortBy(validSortBy);
    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsEmptyOrOnlyWhitespaces_TC020() {
    validRequest.setSearch("     ");
    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());

    validRequest.setSearch("");
    Set<ConstraintViolation<SearchTablesRequest>> emptyViolations =
        validator.validate(validRequest);
    assertEquals(0, emptyViolations.size());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {1, 2, 3})
  void requestValidation_AcceptsOptionalAndSupportedStatusFilters_TC021(Integer status) {
    validRequest.setStatus(status);

    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_AcceptsSearchAtMaximumLength_TC022() {
    validRequest.setSearch("A".repeat(100));

    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPageIsLessThanOne_TC023() {
    validRequest.setPage(0);
    assertSingleViolation("page", ValidationMessage.Msg.PAGE_MIN);
  }

  @ParameterizedTest
  @MethodSource("invalidSizeCases")
  void requestValidation_RejectsInvalidSize_TC024(Integer value, String expectedMessage) {
    validRequest.setSize(value);

    assertSingleViolation("size", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidStatusCases")
  void requestValidation_RejectsInvalidStatus_TC025(Integer value, String expectedMessage) {
    validRequest.setStatus(value);

    assertSingleViolation("status", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidSearchCases")
  void requestValidation_RejectsInvalidSearch_TC026(String value, String expectedMessage) {
    validRequest.setSearch(value);

    assertSingleViolation("search", expectedMessage);
  }

  @Test
  void process_ValidationFails_WhenSortByIsInvalid_TC027() {
    validRequest.setSortBy("unsupported_column");
    assertSingleViolation("sortBy", ValidationMessage.Msg.SORT_BY_INVALID);
  }

  @Test
  void requestValidation_ReportsAllInvalidFilters_TC028() {
    validRequest.setPage(0);
    validRequest.setSize(101);
    validRequest.setStatus(4);
    validRequest.setSearch("%");
    validRequest.setSortBy("unsupported");

    var violations = validator.validate(validRequest);

    assertEquals(5, violations.size());
    assertEquals(
        java.util.Map.of(
            "page",
            ValidationMessage.Msg.PAGE_MIN,
            "size",
            ValidationMessage.Msg.SIZE_MAX,
            "status",
            ValidationMessage.Msg.SIZE_MAX,
            "search",
            ValidationMessage.Msg.SPECIAL_CHARACTERS,
            "sortBy",
            ValidationMessage.Msg.SORT_BY_INVALID),
        violations.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage)));
  }

  // =========================================================================
  // REQUEST HELPERS - DEFAULTS, TRIMMING & PAGINATION
  // =========================================================================

  @Test
  void request_DefaultValuesAndGettersSetters_TC029() {
    SearchTablesRequest defaultReq = new SearchTablesRequest();
    assertEquals(1, defaultReq.getPage());
    assertEquals(10, defaultReq.getSize());
    assertNull(defaultReq.getShopId());
    assertNull(defaultReq.getStatus());
    assertNull(defaultReq.getSearch());
    assertEquals("tableNumber", defaultReq.getSortBy());
    assertEquals(SortDirection.ASC, defaultReq.getSortDirection());

    Set<ConstraintViolation<SearchTablesRequest>> violations = validator.validate(defaultReq);
    assertEquals(0, violations.size());
  }

  @Test
  void request_TransformsSearchAndCalculatesPagination_TC030() {
    validRequest.setSearch("  Table 5  ");
    validRequest.setPage(3);
    validRequest.setSize(25);

    assertEquals("Table 5", validRequest.trimmedSearch());
    assertEquals(50, validRequest.calcOffset());
    assertEquals(3, validRequest.totalPages(60));
    assertEquals(0, validRequest.totalPages(0));
    assertEquals(1, validRequest.totalPages(1));

    validRequest.setSearch(null);
    assertNull(validRequest.trimmedSearch());

    validRequest.setSearch("    ");
    assertNull(validRequest.trimmedSearch());

    validRequest.setSearch("");
    assertNull(validRequest.trimmedSearch());
  }

  // =========================================================================
  // HELPER STUB, VALIDATION & TEST DATA METHODS
  // =========================================================================

  private TableResult createTableResult(int tableNumber, String description, Integer status) {
    TableResult result = new TableResult();
    result.setTableId(new UUID(0L, tableNumber));
    result.setShopId(currentUserShopId);
    result.setShopName("Hoa Yên Coffee");
    result.setTableNumber(tableNumber);
    result.setDescription(description);
    result.setStatus(status);
    result.setCreatedAt("2026-10-08T10:15:30");
    result.setUpdatedAt("2026-10-08T10:15:30");
    return result;
  }

  private void assertSingleViolation(String field, String message) {
    var violations = validator.validate(validRequest);
    assertEquals(1, violations.size(), violations.toString());
    var violation = violations.iterator().next();
    assertEquals(field, violation.getPropertyPath().toString());
    assertEquals(message, violation.getMessage());
  }

  static Stream<Arguments> invalidSizeCases() {
    return Stream.of(
        Arguments.of(0, ValidationMessage.Msg.SIZE_MIN),
        Arguments.of(101, ValidationMessage.Msg.SIZE_MAX));
  }

  static Stream<Arguments> invalidStatusCases() {
    return Stream.of(
        Arguments.of(0, ValidationMessage.Msg.SIZE_MIN),
        Arguments.of(4, ValidationMessage.Msg.SIZE_MAX));
  }

  static Stream<Arguments> invalidSearchCases() {
    return Stream.of(
        Arguments.of("A".repeat(101), ValidationMessage.Msg.SIZE_MAX),
        Arguments.of("Table @ 2026! #$%*", ValidationMessage.Msg.SPECIAL_CHARACTERS));
  }
}
