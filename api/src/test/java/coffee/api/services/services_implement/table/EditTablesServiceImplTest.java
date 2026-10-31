package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.EditTablesMapper;
import coffee.api.services.services_implement.table.common.TableValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class EditTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private EditTablesMapper editTablesMapper;

  private EditTablesServiceImpl editTablesService;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;
  private EditTablesRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

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
    currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000701");
    TableValidator tableValidator = new TableValidator(commonMapper);
    editTablesService = new EditTablesServiceImpl(commonMapper, editTablesMapper, tableValidator);

    currentUserShopId = UUID.fromString("00000000-0000-0000-0000-000000000702");

    validRequest = new EditTablesRequest();
    validRequest.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000703"));
    validRequest.setShopId(currentUserShopId);
    validRequest.setTableNumber(15);
    validRequest.setDescription("Bàn ngoài trời - Cập nhật");
    validRequest.setStatus(1);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerAndAllChecksPass_TC001() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);
    when(editTablesMapper.updateTable(validRequest, managerRole, currentUserShopId)).thenReturn(1);

    assertDoesNotThrow(
        () ->
            editTablesService.process(validRequest, managerRole, currentUserId, currentUserShopId));

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper)
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper)
        .checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId());
    verify(commonMapper)
        .checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId);
    verify(editTablesMapper).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithAnyShop_TC002() {
    UUID differentShopId = UUID.fromString("00000000-0000-0000-0000-000000000704");
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), differentShopId))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            differentShopId,
            ownerRole,
            currentUserShopId))
        .thenReturn(false);
    when(editTablesMapper.updateTable(validRequest, ownerRole, currentUserShopId)).thenReturn(1);

    assertDoesNotThrow(
        () -> editTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId));

    verify(commonMapper).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper).checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId);
    verify(commonMapper).checkTableExistsInShop(validRequest.getTableId(), differentShopId);
    verify(commonMapper)
        .checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            differentShopId,
            ownerRole,
            currentUserShopId);
    verify(editTablesMapper).updateTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_TrimsDescriptionCorrectly_TC003() {
    validRequest.setDescription("   Bàn VIP gần cửa sổ   ");

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);
    when(editTablesMapper.updateTable(validRequest, managerRole, currentUserShopId)).thenReturn(1);

    assertDoesNotThrow(
        () ->
            editTablesService.process(validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Bàn VIP gần cửa sổ", validRequest.getDescription());
    verify(editTablesMapper).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenDescriptionIsNull_TC004() {
    validRequest.setDescription(null);

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);
    when(editTablesMapper.updateTable(validRequest, managerRole, currentUserShopId)).thenReturn(1);

    assertDoesNotThrow(
        () ->
            editTablesService.process(validRequest, managerRole, currentUserId, currentUserShopId));

    assertNull(validRequest.getDescription());
    verify(editTablesMapper).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\t"})
  void process_NormalizesBlankDescriptionBeforeSaving_TC005(String description) {
    validRequest.setDescription(description);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(editTablesMapper.updateTable(validRequest, ownerRole, currentUserShopId)).thenReturn(1);

    editTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId);

    assertNull(validRequest.getDescription());
    verify(editTablesMapper).updateTable(validRequest, ownerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PERMISSION & BUSINESS CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC006() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editTablesService.process(null, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC007() {
    validRequest.setShopId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableIdIsNull_TC008() {
    validRequest.setTableId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberIsNull_TC009() {
    validRequest.setTableNumber(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table number must be greater than 0", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void process_ThrowsInvalidRequestException_WhenTableNumberIsZeroOrNegative_TC010(
      int tableNumber) {
    validRequest.setTableNumber(tableNumber);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table number must be greater than 0", error.getMessage());
    verifyNoInteractions(commonMapper, editTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC011() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verify(commonMapper, never()).checkTableExistsInShop(any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC012() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editTablesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatchesRequestShopId_TC013() {
    UUID differentShopId = UUID.fromString("00000000-0000-0000-0000-000000000705");
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(differentShopId);
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC014() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDoesNotExist_TC015() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getTableId(), exception.getId());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper)
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper, never()).checkTableExistsInShop(any(), any());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableDoesNotBelongToShopBranch_TC016() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table does not belong to this shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper)
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper)
        .checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenTableNumberAlreadyExists_TC017() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(true);

    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertEquals(1, exception.getErrorDetails().size());

    ErrorDetail errorDetail = exception.getErrorDetails().getFirst();
    assertEquals(ResponseCode.CONFLICT.getCode(), errorDetail.getErrorCode());
    assertEquals("Table number already exists in this shop", errorDetail.getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenAffectedRowsIsZero_TC018() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);
    when(editTablesMapper.updateTable(validRequest, managerRole, currentUserShopId)).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals(
        "Table has already been deleted or modified by another request", exception.getMessage());
    assertEquals(validRequest.getTableId(), exception.getId());

    verify(editTablesMapper).updateTable(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseUpdateFails_TC019() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    doThrow(new DataAccessException("Database timeout during update") {})
        .when(editTablesMapper)
        .updateTable(any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Database timeout during update", exception.getMessage());

    verify(editTablesMapper).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3, 4})
  void process_StopsBeforeUpdateWhenValidationQueryFails_TC020(int failureStage) {
    UUID shopId = validRequest.getShopId();
    UUID tableId = validRequest.getTableId();
    DataAccessException failure = new DataAccessException("Validation query failed") {};
    if (failureStage > 0) when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (failureStage > 1)
      when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    if (failureStage > 2)
      when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId))
          .thenReturn(true);
    if (failureStage > 3)
      when(commonMapper.checkTableExistsInShop(tableId, shopId)).thenReturn(true);
    switch (failureStage) {
      case 0 -> when(commonMapper.checkShopExisted(shopId)).thenThrow(failure);
      case 1 ->
          when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId))
              .thenThrow(failure);
      case 2 ->
          when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId))
              .thenThrow(failure);
      case 3 -> when(commonMapper.checkTableExistsInShop(tableId, shopId)).thenThrow(failure);
      case 4 ->
          when(commonMapper.checkTableNumberExisted(
                  tableId, validRequest.getTableNumber(), shopId, managerRole, currentUserShopId))
              .thenThrow(failure);
      default -> throw new IllegalArgumentException("Unexpected failure stage");
    }

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Validation query failed", error.getMessage());
    verifyNoInteractions(editTablesMapper);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC021() {
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionIsNull_TC022() {
    validRequest.setDescription(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionContainsVietnameseAndAllowedCharacters_TC023() {
    validRequest.setDescription("Bàn VIP (Cửa Sổ) & Ban Công - Tầng 2, Khu B. 'Espresso'");
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void requestValidation_Success_WhenStatusIsWithinRange_TC024(int status) {
    validRequest.setStatus(status);

    assertEquals(0, validator.validate(validRequest).size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 9999})
  void requestValidation_Success_WhenTableNumberIsBoundaryValue_TC025(int tableNumber) {
    validRequest.setTableNumber(tableNumber);

    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_AcceptsMaximumDescriptionLength_TC026() {
    validRequest.setDescription("A".repeat(255));

    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenTableIdIsNull_TC027() {
    validRequest.setTableId(null);
    assertSingleViolation("tableId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC028() {
    validRequest.setShopId(null);
    assertSingleViolation("shopId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @MethodSource("invalidTableNumberCases")
  void requestValidation_RejectsInvalidTableNumber_TC029(Integer value, String expectedMessage) {
    validRequest.setTableNumber(value);

    assertSingleViolation("tableNumber", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidStatusCases")
  void requestValidation_RejectsInvalidStatus_TC030(Integer value, String expectedMessage) {
    validRequest.setStatus(value);

    assertSingleViolation("status", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidDescriptionCases")
  void requestValidation_RejectsInvalidDescription_TC031(String value, String expectedMessage) {
    validRequest.setDescription(value);

    assertSingleViolation("description", expectedMessage);
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC032() {
    EditTablesRequest emptyRequest = new EditTablesRequest();
    emptyRequest.setTableId(null);
    emptyRequest.setShopId(null);
    emptyRequest.setTableNumber(null);
    emptyRequest.setStatus(null);

    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(emptyRequest);
    assertEquals(4, violations.size());

    assertEquals(
        Set.of("tableId", "tableNumber", "shopId", "status"),
        violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        Set.of(ValidationMessage.Msg.FIELD_REQUIRED),
        violations.stream()
            .map(ConstraintViolation::getMessage)
            .collect(java.util.stream.Collectors.toSet()));
  }

  // =========================================================================
  // HELPER STUB, VALIDATION & TEST DATA METHODS
  // =========================================================================

  private void assertSingleViolation(String field, String message) {
    var violations = validator.validate(validRequest);
    assertEquals(1, violations.size(), violations.toString());
    var violation = violations.iterator().next();
    assertEquals(field, violation.getPropertyPath().toString());
    assertEquals(message, violation.getMessage());
  }

  static Stream<Arguments> invalidTableNumberCases() {
    return Stream.of(
        Arguments.of(null, ValidationMessage.Msg.FIELD_REQUIRED),
        Arguments.of(0, ValidationMessage.Msg.PAGE_MIN),
        Arguments.of(10000, ValidationMessage.Msg.SIZE_MAX));
  }

  static Stream<Arguments> invalidStatusCases() {
    return Stream.of(
        Arguments.of(null, ValidationMessage.Msg.FIELD_REQUIRED),
        Arguments.of(0, ValidationMessage.Msg.SIZE_MIN),
        Arguments.of(4, ValidationMessage.Msg.SIZE_MAX));
  }

  static Stream<Arguments> invalidDescriptionCases() {
    return Stream.of(
        Arguments.of("A".repeat(256), ValidationMessage.Msg.SIZE_MAX),
        Arguments.of("Bàn VIP @ Cửa Sổ <script>!", ValidationMessage.Msg.SPECIAL_CHARACTERS));
  }
}
