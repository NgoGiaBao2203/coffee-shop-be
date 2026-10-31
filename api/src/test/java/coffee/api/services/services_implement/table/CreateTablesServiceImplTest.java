package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateTablesMapper;
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
class CreateTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateTablesMapper createTablesMapper;

  private CreateTablesServiceImpl createTablesService;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;
  private CreateTablesRequest validRequest;
  private UUID currentUserShopId;
  private UUID currentUserId;
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
    TableValidator tableValidator = new TableValidator(commonMapper);
    createTablesService =
        new CreateTablesServiceImpl(commonMapper, createTablesMapper, tableValidator);

    currentUserShopId = UUID.fromString("00000000-0000-0000-0000-000000000501");
    currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000502");

    validRequest = new CreateTablesRequest();
    validRequest.setTableNumber(1);
    validRequest.setDescription("Table near window");
    validRequest.setShopId(currentUserShopId);
    validRequest.setStatus(1);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithMatchingShopAndValidMembership_TC001() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper)
        .checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId);
    verify(createTablesMapper).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithDifferentShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.fromString("00000000-0000-0000-0000-000000000503");
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null, validRequest.getTableNumber(), differentShopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(validRequest, currentUserId, ownerRole, currentUserShopId));

    verify(commonMapper).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper)
        .checkTableNumberExisted(
            null, validRequest.getTableNumber(), differentShopId, ownerRole, currentUserShopId);
    verify(createTablesMapper).createTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenOwnerHasNoAssignedShop_TC003() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null, validRequest.getTableNumber(), validRequest.getShopId(), ownerRole, null))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, currentUserId, ownerRole, null));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createTablesMapper).createTable(validRequest, ownerRole, null);
  }

  @Test
  void process_Success_TrimsDescriptionCorrectly_TC004() {
    // Arrange
    validRequest.setDescription("   Bàn VIP gần ban công   ");

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Bàn VIP gần ban công", validRequest.getDescription());
    verify(createTablesMapper).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenDescriptionIsNull_TC005() {
    // Arrange
    validRequest.setDescription(null);

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    assertNull(validRequest.getDescription());
    verify(createTablesMapper).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserRoleIsNotManager_SkipsManagerValidationBlock_TC006() {
    // Arrange: When another role (like STAFF) calls service, manager check block is bypassed
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            staffRole,
            currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(validRequest, currentUserId, staffRole, currentUserShopId));

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createTablesMapper).createTable(validRequest, staffRole, currentUserShopId);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "\t"})
  void process_NormalizesBlankDescriptionBeforeSaving_TC007(String description) {
    validRequest.setDescription(description);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    createTablesService.process(validRequest, currentUserId, ownerRole, currentUserShopId);

    assertNull(validRequest.getDescription());
    verify(createTablesMapper).createTable(validRequest, ownerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PERMISSION & BUSINESS CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC008() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(null, currentUserId, managerRole, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC009() {
    validRequest.setShopId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberIsNull_TC010() {
    validRequest.setTableNumber(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Table number must be greater than 0", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createTablesMapper);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void process_ThrowsInvalidRequestException_WhenTableNumberIsZeroOrNegative_TC011(
      int tableNumber) {
    validRequest.setTableNumber(tableNumber);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Table number must be greater than 0", error.getMessage());
    verifyNoInteractions(commonMapper, createTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC012() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC013() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, currentUserId, managerRole, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC014() {
    UUID differentShopId = UUID.fromString("00000000-0000-0000-0000-000000000504");
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(differentShopId);
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC015() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberAlreadyExists_TC016() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Table number already exists in this shop", exception.getMessage());

    verify(commonMapper).checkShopExisted(validRequest.getShopId());
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper)
        .checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId);
    verify(createTablesMapper, never()).createTable(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC017() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            null,
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createTablesMapper)
        .createTable(any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Database insertion error", exception.getMessage());

    verify(createTablesMapper).createTable(validRequest, managerRole, currentUserShopId);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2})
  void process_StopsBeforeInsertWhenValidationQueryFails_TC018(int failureStage) {
    UUID shopId = validRequest.getShopId();
    DataAccessException failure = new DataAccessException("Validation query failed") {};
    if (failureStage > 0) when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (failureStage > 1)
      when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    switch (failureStage) {
      case 0 -> when(commonMapper.checkShopExisted(shopId)).thenThrow(failure);
      case 1 ->
          when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId))
              .thenThrow(failure);
      case 2 ->
          when(commonMapper.checkTableNumberExisted(
                  null, validRequest.getTableNumber(), shopId, managerRole, currentUserShopId))
              .thenThrow(failure);
      default -> throw new IllegalArgumentException("Unexpected failure stage");
    }

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Validation query failed", error.getMessage());
    verifyNoInteractions(createTablesMapper);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC019() {
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionIsNull_TC020() {
    validRequest.setDescription(null);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionContainsVietnameseAndAllowedCharacters_TC021() {
    // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
    validRequest.setDescription("Bàn VIP (Gần Cửa/Sổ) - Khu A, Số 1. 'Espresso'");
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void requestValidation_Success_WhenStatusIsWithinValidRange_TC022(int status) {
    validRequest.setStatus(status);

    assertEquals(0, validator.validate(validRequest).size());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 9999})
  void requestValidation_Success_WhenTableNumberIsBoundaryValue_TC023(int tableNumber) {
    validRequest.setTableNumber(tableNumber);

    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_AcceptsMaximumDescriptionLength_TC024() {
    validRequest.setDescription("A".repeat(255));

    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @MethodSource("invalidTableNumberCases")
  void requestValidation_RejectsInvalidTableNumber_TC025(Integer value, String expectedMessage) {
    validRequest.setTableNumber(value);

    assertSingleViolation("tableNumber", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidStatusCases")
  void requestValidation_RejectsInvalidStatus_TC026(Integer value, String expectedMessage) {
    validRequest.setStatus(value);

    assertSingleViolation("status", expectedMessage);
  }

  @ParameterizedTest
  @MethodSource("invalidDescriptionCases")
  void requestValidation_RejectsInvalidDescription_TC027(String value, String expectedMessage) {
    validRequest.setDescription(value);

    assertSingleViolation("description", expectedMessage);
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC028() {
    validRequest.setShopId(null);
    assertSingleViolation("shopId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenAllRequiredFieldsAreNull_TC029() {
    CreateTablesRequest emptyRequest = new CreateTablesRequest();
    emptyRequest.setTableNumber(null);
    emptyRequest.setShopId(null);
    emptyRequest.setStatus(null);

    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(emptyRequest);
    assertEquals(3, violations.size());

    assertEquals(
        Set.of("tableNumber", "shopId", "status"),
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
  // REQUEST HELPERS - DEFAULTS, TRIMMING & PAGINATION
  // =========================================================================

  @Test
  void request_TrimmedDescriptionMethod_TC030() {
    validRequest.setDescription("   Tầng 2   ");
    assertEquals("Tầng 2", validRequest.trimmedDescription());

    validRequest.setDescription(null);
    assertNull(validRequest.trimmedDescription());

    validRequest.setDescription("    ");
    assertNull(validRequest.trimmedDescription());

    validRequest.setDescription("");
    assertNull(validRequest.trimmedDescription());
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
