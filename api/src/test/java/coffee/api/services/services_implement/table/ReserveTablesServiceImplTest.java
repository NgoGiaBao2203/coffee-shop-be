package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.table.ReserveTablesRequest;
import coffee.api.dto.result.TableResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateTableStatusMapper;
import coffee.api.services.services_implement.table.common.TableStatusChanger;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class ReserveTablesServiceImplTest {

  private static final int TARGET = TableStatusChanger.STATUS_RESERVED;

  @Mock private UpdateTableStatusMapper updateTableStatusMapper;
  @Mock private CommonMapper commonMapper;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private ReserveTablesServiceImpl service;
  private ReserveTablesRequest validRequest;
  private TableResult lockedTable;
  private UUID tableId;
  private UUID shopId;
  private UUID profileId;

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
        new ReserveTablesServiceImpl(new TableStatusChanger(updateTableStatusMapper, commonMapper));
    tableId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    shopId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    profileId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    validRequest = new ReserveTablesRequest();
    validRequest.setTableId(tableId);

    lockedTable = new TableResult();
    lockedTable.setTableId(tableId);
    lockedTable.setShopId(shopId);
    lockedTable.setTableNumber(1);
    lockedTable.setStatus(1);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubLockedTable(String role, UUID currentShopId) {
    when(updateTableStatusMapper.lockTable(tableId, role, currentShopId)).thenReturn(lockedTable);
  }

  private void stubShopMember(boolean isMember) {
    when(commonMapper.checkShopIdIsExisted(profileId, shopId)).thenReturn(isMember);
  }

  private void stubNoUnpaidInvoices() {
    when(commonMapper.hasUnpaidTableInvoices(tableId)).thenReturn(false);
  }

  private void stubUpdate(int affectedRows) {
    when(updateTableStatusMapper.updateTableStatus(tableId, shopId, TARGET))
        .thenReturn(affectedRows);
  }

  private void verifyNoUpdate() {
    verify(updateTableStatusMapper, never()).updateTableStatus(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_Success_WhenEachRoleChangesTableOfOwnShop_TC001(String role) {
    UUID currentShopId = shopId;
    stubLockedTable(role, currentShopId);
    if (!Roles.OWNER.getValue().equals(role)) {
      stubShopMember(true);
    }
    stubNoUnpaidInvoices();
    stubUpdate(1);

    service.process(validRequest, role, currentShopId, profileId);

    var order = inOrder(updateTableStatusMapper, commonMapper);
    order.verify(updateTableStatusMapper).lockTable(tableId, role, currentShopId);
    order.verify(commonMapper).hasUnpaidTableInvoices(tableId);
    order.verify(updateTableStatusMapper).updateTableStatus(tableId, shopId, TARGET);
  }

  @Test
  void process_Success_WhenOwnerHasNoAssignedShop_TC002() {
    stubLockedTable(Roles.OWNER.getValue(), null);
    stubNoUnpaidInvoices();
    stubUpdate(1);

    service.process(validRequest, Roles.OWNER.getValue(), null, profileId);

    verify(updateTableStatusMapper).updateTableStatus(tableId, shopId, TARGET);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SHOP & ROLE
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_ThrowsInvalidRequestException_WhenUserHasNoAssignedShop_TC003(String role) {
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, role, null, profileId));

    assertEquals(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP, error.getMessage());
    verify(updateTableStatusMapper, never()).lockTable(any(), any(), any());
    verifyNoUpdate();
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_ThrowsInvalidRequestException_WhenUserIsNotActiveShopMember_TC004(String role) {
    stubLockedTable(role, shopId);
    stubShopMember(false);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, role, shopId, profileId));

    assertEquals(ErrorMessage.SHOP_ACCESS_DENIED, error.getMessage());
    verifyNoUpdate();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / TABLE STATUS
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_ThrowsDataNotFoundException_WhenTableIsOutsideScopeOrMissing_TC005(String role) {
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    when(updateTableStatusMapper.lockTable(tableId, role, otherShopId)).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, role, otherShopId, profileId));

    assertEquals(ErrorMessage.DATA_NOT_FOUND, error.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableAlreadyHasTargetStatus_TC006() {
    lockedTable.setStatus(TARGET);
    stubLockedTable(Roles.OWNER.getValue(), null);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, Roles.OWNER.getValue(), null, profileId));

    assertEquals("Table is already in this status", error.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenUpdateAffectsNoRow_TC007() {
    stubLockedTable(Roles.OWNER.getValue(), null);
    stubNoUnpaidInvoices();
    stubUpdate(0);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, Roles.OWNER.getValue(), null, profileId));

    assertEquals("Table status cannot be updated. Please reload and try again", error.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PENDING INVOICES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenTableHasUnpaidInvoices_TC008() {
    stubLockedTable(Roles.OWNER.getValue(), null);
    when(commonMapper.hasUnpaidTableInvoices(tableId)).thenReturn(true);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, Roles.OWNER.getValue(), null, profileId));

    assertEquals(ErrorMessage.TABLE_HAS_PENDING_INVOICES, error.getMessage());
    verifyNoUpdate();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenLockTableFails_TC009() {
    when(updateTableStatusMapper.lockTable(tableId, Roles.OWNER.getValue(), null))
        .thenThrow(new DataAccessException("db down") {});

    assertThrows(
        DataAccessException.class,
        () -> service.process(validRequest, Roles.OWNER.getValue(), null, profileId));
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsDataAccessException_WhenUpdateTableStatusFails_TC010() {
    stubLockedTable(Roles.OWNER.getValue(), null);
    stubNoUnpaidInvoices();
    when(updateTableStatusMapper.updateTableStatus(tableId, shopId, TARGET))
        .thenThrow(new DataAccessException("db down") {});

    assertThrows(
        DataAccessException.class,
        () -> service.process(validRequest, Roles.OWNER.getValue(), null, profileId));
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenTableIdIsPresent_TC011() {
    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_ReportsFieldRequired_WhenTableIdIsNull_TC012() {
    validRequest.setTableId(null);

    var violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
