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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class CreateTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateTablesMapper createTablesMapper;

  private CreateTablesServiceImpl createTablesService;

  private Validator validator;
  private CreateTablesRequest validRequest;
  private UUID currentUserShopId;
  private UUID currentUserId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    TableValidator tableValidator = new TableValidator(commonMapper);
    createTablesService =
        new CreateTablesServiceImpl(commonMapper, createTablesMapper, tableValidator);

    currentUserShopId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();

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
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId)))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId));
    verify(createTablesMapper, times(1)).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithDifferentShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(differentShopId),
            eq(ownerRole),
            eq(currentUserShopId)))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(validRequest, currentUserId, ownerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(differentShopId),
            eq(ownerRole),
            eq(currentUserShopId));
    verify(createTablesMapper, times(1)).createTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenOwnerHasNoAssignedShop_TC003() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(ownerRole),
            isNull()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, currentUserId, ownerRole, null));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createTablesMapper, times(1)).createTable(validRequest, ownerRole, null);
  }

  @Test
  void process_Success_TrimsDescriptionCorrectly_TC004() {
    // Arrange
    validRequest.setDescription("   Bàn VIP gần ban công   ");

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId)))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Bàn VIP gần ban công", validRequest.getDescription());
    verify(createTablesMapper, times(1)).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenDescriptionIsNull_TC005() {
    // Arrange
    validRequest.setDescription(null);

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId)))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    assertNull(validRequest.getDescription());
    verify(createTablesMapper, times(1)).createTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserRoleIsNotManager_SkipsManagerValidationBlock_TC006() {
    // Arrange: When another role (like STAFF) calls service, manager check block is bypassed
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(staffRole),
            eq(currentUserShopId)))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            createTablesService.process(validRequest, currentUserId, staffRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createTablesMapper, times(1)).createTable(validRequest, staffRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC007() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(null, currentUserId, managerRole, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC008() {
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
  void process_ThrowsInvalidRequestException_WhenTableNumberIsNull_TC009() {
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

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberIsZeroOrNegative_TC010() {
    validRequest.setTableNumber(0);

    InvalidRequestException zeroException =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));
    assertEquals("Table number must be greater than 0", zeroException.getMessage());

    validRequest.setTableNumber(-5);
    InvalidRequestException negException =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));
    assertEquals("Table number must be greater than 0", negException.getMessage());

    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC011() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC012() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, currentUserId, managerRole, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC013() {
    UUID differentShopId = UUID.randomUUID();
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

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC014() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberAlreadyExists_TC015() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId)))
        .thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createTablesService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Table number already exists in this shop", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId));
    verify(createTablesMapper, never()).createTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC016() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            isNull(),
            eq(validRequest.getTableNumber()),
            eq(validRequest.getShopId()),
            eq(managerRole),
            eq(currentUserShopId)))
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

    verify(createTablesMapper, times(1)).createTable(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC017() {
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionIsNull_TC018() {
    validRequest.setDescription(null);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionContainsVietnameseAndAllowedCharacters_TC019() {
    // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
    validRequest.setDescription("Bàn VIP (Gần Cửa/Sổ) - Khu A, Số 1. 'Espresso'");
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenStatusIsWithinValidRange_TC020() {
    validRequest.setStatus(1); // Available
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setStatus(2); // Occupied
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setStatus(3); // Reserved
    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_Success_WhenTableNumberIsBoundaryValue_TC021() {
    validRequest.setTableNumber(1);
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setTableNumber(9999);
    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void request_TrimmedDescriptionMethod_TC022() {
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
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: TABLE NUMBER
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenTableNumberIsNull_TC023() {
    validRequest.setTableNumber(null);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenTableNumberIsLessThanOne_TC024() {
    validRequest.setTableNumber(0);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.PAGE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenTableNumberExceeds9999_TC025() {
    validRequest.setTableNumber(10000);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: STATUS
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenStatusIsNull_TC026() {
    validRequest.setStatus(null);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenStatusIsLessThanOne_TC027() {
    validRequest.setStatus(0);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenStatusExceedsThree_TC028() {
    validRequest.setStatus(4);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: DESCRIPTION & SHOP ID
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenDescriptionExceeds255Characters_TC029() {
    validRequest.setDescription("A".repeat(256));
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenDescriptionContainsDisallowedCharacters_TC030() {
    validRequest.setDescription("Bàn VIP @ Cửa Sổ <script>!");
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC031() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllRequiredFieldsAreNull_TC032() {
    CreateTablesRequest emptyRequest = new CreateTablesRequest();
    emptyRequest.setTableNumber(null);
    emptyRequest.setShopId(null);
    emptyRequest.setStatus(null);

    Set<ConstraintViolation<CreateTablesRequest>> violations = validator.validate(emptyRequest);
    assertEquals(3, violations.size());
  }
}
