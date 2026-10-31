package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteTablesMapper;
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
public class DeleteTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteTablesMapper deleteTablesMapper;

  private DeleteTablesServiceImpl deleteTablesService;

  private Validator validator;
  private DeleteTablesRequest validRequest;
  private UUID currentUserShopId;
  private UUID currentUserId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    TableValidator tableValidator = new TableValidator(commonMapper);
    deleteTablesService = new DeleteTablesServiceImpl(deleteTablesMapper, tableValidator);

    currentUserShopId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();

    validRequest = new DeleteTablesRequest();
    validRequest.setTableId(UUID.randomUUID());
    validRequest.setShopId(currentUserShopId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithActiveShop_TC001() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(0);
    when(deleteTablesMapper.softDeleteTable(
            validRequest.getTableId(), managerRole, currentUserShopId, currentUserId))
        .thenReturn(1);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            deleteTablesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verify(deleteTablesMapper, times(1)).countPendingInvoicesByTable(validRequest.getTableId());
    verify(deleteTablesMapper, times(1))
        .softDeleteTable(validRequest.getTableId(), managerRole, currentUserShopId, currentUserId);
  }

  @Test
  void process_Success_WhenUserIsOwner_BypassesShopMembershipCheck_TC002() {
    // Arrange
    when(commonMapper.checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(0);
    when(deleteTablesMapper.softDeleteTable(
            validRequest.getTableId(), ownerRole, currentUserShopId, currentUserId))
        .thenReturn(1);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            deleteTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), ownerRole, currentUserShopId);
    verify(deleteTablesMapper, times(1)).countPendingInvoicesByTable(validRequest.getTableId());
    verify(deleteTablesMapper, times(1))
        .softDeleteTable(validRequest.getTableId(), ownerRole, currentUserShopId, currentUserId);
  }

  @Test
  void process_Success_WhenOwnerHasNoAssignedShop_TC003() {
    // Arrange: Owner does not belong to any branch (currentUserShopId is null)
    when(commonMapper.checkTableExisted(validRequest.getTableId(), ownerRole, null))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(0);
    when(deleteTablesMapper.softDeleteTable(
            validRequest.getTableId(), ownerRole, null, currentUserId))
        .thenReturn(1);

    // Act & Assert
    assertDoesNotThrow(
        () -> deleteTablesService.process(validRequest, ownerRole, currentUserId, null));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1)).checkTableExisted(validRequest.getTableId(), ownerRole, null);
    verify(deleteTablesMapper, times(1))
        .softDeleteTable(validRequest.getTableId(), ownerRole, null, currentUserId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenUserRoleIsStaff_TC004() {
    // Arrange
    String staffRole = Roles.STAFF.getValue();
    when(commonMapper.checkTableExisted(validRequest.getTableId(), staffRole, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, staffRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteTablesMapper, never()).countPendingInvoicesByTable(any());
    verifyNoMoreInteractions(deleteTablesMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC005() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteTablesService.process(null, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableIdIsNull_TC006() {
    // Arrange
    validRequest.setTableId(null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC007() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteTablesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verifyNoInteractions(deleteTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactiveOrDeleted_TC008() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkTableExisted(any(), any(), any());
    verifyNoInteractions(deleteTablesMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDoesNotExist_TC009() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getTableId(), exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId);
    verifyNoInteractions(deleteTablesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableHasPendingInvoices_TC010() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(1);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Can not delete table with pending invoices", exception.getMessage());

    verify(deleteTablesMapper, times(1)).countPendingInvoicesByTable(validRequest.getTableId());
    verify(deleteTablesMapper, never()).softDeleteTable(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenCountingPendingInvoicesFails_TC011() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    doThrow(new DataAccessException("Database timeout during invoice check") {})
        .when(deleteTablesMapper)
        .countPendingInvoicesByTable(validRequest.getTableId());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Database timeout during invoice check", exception.getMessage());
    verify(deleteTablesMapper, never()).softDeleteTable(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFailsDuringSoftDelete_TC012() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(0);

    doThrow(new DataAccessException("Database timeout during soft deletion") {})
        .when(deleteTablesMapper)
        .softDeleteTable(validRequest.getTableId(), managerRole, currentUserShopId, currentUserId);

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Database timeout during soft deletion", exception.getMessage());

    verify(deleteTablesMapper, times(1)).countPendingInvoicesByTable(validRequest.getTableId());
    verify(deleteTablesMapper, times(1))
        .softDeleteTable(validRequest.getTableId(), managerRole, currentUserShopId, currentUserId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDeleteAffectsNoRows_TC013() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableExisted(validRequest.getTableId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteTablesMapper.countPendingInvoicesByTable(validRequest.getTableId())).thenReturn(0);
    when(deleteTablesMapper.softDeleteTable(
            validRequest.getTableId(), managerRole, currentUserShopId, currentUserId))
        .thenReturn(0);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals(
        "Table has already been deleted or modified by another request", exception.getMessage());
    assertEquals(validRequest.getTableId(), exception.getId());

    verify(deleteTablesMapper, times(1)).countPendingInvoicesByTable(validRequest.getTableId());
    verify(deleteTablesMapper, times(1))
        .softDeleteTable(validRequest.getTableId(), managerRole, currentUserShopId, currentUserId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC014() {
    Set<ConstraintViolation<DeleteTablesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenTableIdIsNull_TC015() {
    validRequest.setTableId(null);
    Set<ConstraintViolation<DeleteTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC016() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<DeleteTablesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC017() {
    DeleteTablesRequest emptyRequest = new DeleteTablesRequest();
    Set<ConstraintViolation<DeleteTablesRequest>> violations = validator.validate(emptyRequest);

    assertEquals(2, violations.size());
    Set<String> violatedFields =
        violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

    assertTrue(violatedFields.contains("tableId"));
    assertTrue(violatedFields.contains("shopId"));
  }
}
