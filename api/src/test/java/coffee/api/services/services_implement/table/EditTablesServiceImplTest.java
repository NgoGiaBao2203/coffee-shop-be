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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class EditTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private EditTablesMapper editTablesMapper;

  private EditTablesServiceImpl editTablesService;

  private Validator validator;
  private EditTablesRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.randomUUID();
    TableValidator tableValidator = new TableValidator(commonMapper);
    editTablesService = new EditTablesServiceImpl(commonMapper, editTablesMapper, tableValidator);

    currentUserShopId = UUID.randomUUID();

    validRequest = new EditTablesRequest();
    validRequest.setTableId(UUID.randomUUID());
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

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId());
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            validRequest.getShopId(),
            managerRole,
            currentUserShopId);
    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithAnyShop_TC002() {
    UUID differentShopId = UUID.randomUUID();
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

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExistsInShop(validRequest.getTableId(), differentShopId);
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            validRequest.getTableId(),
            validRequest.getTableNumber(),
            differentShopId,
            ownerRole,
            currentUserShopId);
    verify(editTablesMapper, times(1)).updateTable(validRequest, ownerRole, currentUserShopId);
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
    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
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
    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC005() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editTablesService.process(null, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC006() {
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
  void process_ThrowsInvalidRequestException_WhenTableIdIsNull_TC007() {
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
  void process_ThrowsInvalidRequestException_WhenTableNumberIsNull_TC008() {
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

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberIsZeroOrNegative_TC009() {
    validRequest.setTableNumber(0);
    InvalidRequestException zeroException =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));
    assertEquals("Table number must be greater than 0", zeroException.getMessage());

    validRequest.setTableNumber(-1);
    InvalidRequestException negException =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));
    assertEquals("Table number must be greater than 0", negException.getMessage());

    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC010() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verify(commonMapper, never()).checkTableExistsInShop(any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC011() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editTablesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatchesRequestShopId_TC012() {
    UUID differentShopId = UUID.randomUUID();
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

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC013() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDoesNotExist_TC014() {
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

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper, never()).checkTableExistsInShop(any(), any());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableDoesNotBelongToShopBranch_TC015() {
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

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExistsInShop(validRequest.getTableId(), validRequest.getShopId());
    verify(commonMapper, never()).checkTableNumberExisted(any(), any(), any(), any(), any());
    verifyNoInteractions(editTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenTableNumberAlreadyExists_TC016() {
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
    assertNotNull(exception.getErrorDetails());
    assertEquals(1, exception.getErrorDetails().size());

    ErrorDetail errorDetail = exception.getErrorDetails().getFirst();
    assertEquals(ResponseCode.CONFLICT.getCode(), errorDetail.getErrorCode());
    assertEquals("Table number already exists in this shop", errorDetail.getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenAffectedRowsIsZero_TC017() {
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

    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseUpdateFails_TC018() {
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

    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC019() {
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionIsNull_TC020() {
    validRequest.setDescription(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenDescriptionContainsVietnameseAndAllowedCharacters_TC021() {
    validRequest.setDescription("Bàn VIP (Cửa Sổ) & Ban Công - Tầng 2, Khu B. 'Espresso'");
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenStatusIsWithinRange_TC022() {
    validRequest.setStatus(1);
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setStatus(2);
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setStatus(3);
    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_Success_WhenTableNumberIsBoundaryValue_TC023() {
    validRequest.setTableNumber(1);
    assertEquals(0, validator.validate(validRequest).size());

    validRequest.setTableNumber(9999);
    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: TABLE ID, SHOP ID, TABLE NUMBER
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenTableIdIsNull_TC024() {
    validRequest.setTableId(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC025() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenTableNumberIsNull_TC026() {
    validRequest.setTableNumber(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenTableNumberIsLessThanOne_TC027() {
    validRequest.setTableNumber(0);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.PAGE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenTableNumberExceeds9999_TC028() {
    validRequest.setTableNumber(10000);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: STATUS & DESCRIPTION
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenStatusIsNull_TC029() {
    validRequest.setStatus(null);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenStatusIsLessThanOne_TC030() {
    validRequest.setStatus(0);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenStatusExceedsThree_TC031() {
    validRequest.setStatus(4);
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenDescriptionExceeds255Characters_TC032() {
    validRequest.setDescription("A".repeat(256));
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenDescriptionContainsDisallowedCharacters_TC033() {
    validRequest.setDescription("Bàn VIP @ Cửa Sổ <script>!");
    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC034() {
    EditTablesRequest emptyRequest = new EditTablesRequest();
    emptyRequest.setTableId(null);
    emptyRequest.setShopId(null);
    emptyRequest.setTableNumber(null);
    emptyRequest.setStatus(null);

    Set<ConstraintViolation<EditTablesRequest>> violations = validator.validate(emptyRequest);
    assertEquals(4, violations.size());
  }
}
