package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.StatusInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.UpdateInvoiceStatusMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class StatusInvoiceServiceImplTest {

  @Mock private UpdateInvoiceStatusMapper statusInvoiceMapper;

  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;
  private StatusInvoiceServiceImpl statusInvoiceService;
  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private StatusInvoiceRequest validRequest;
  private InvoiceResult existingInvoice;
  private UUID invoiceId;
  private UUID currentShopId;
  private UUID currentProfileId;

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
    statusInvoiceService =
        new StatusInvoiceServiceImpl(
            statusInvoiceMapper, new InvoiceValidator(createInvoiceMapper, commonMapper));
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentProfileId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    validRequest = new StatusInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);
    validRequest.setStatus(1);

    existingInvoice = new InvoiceResult();
    existingInvoice.setInvoiceId(invoiceId);
    existingInvoice.setShopId(currentShopId);
    existingInvoice.setStatus("0");
    existingInvoice.setProfileId(currentProfileId);
    existingInvoice.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000401"));
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubShopPermission(String role) {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    if (!"OWNER".equalsIgnoreCase(role)) {
      when(commonMapper.checkShopIdIsExisted(currentProfileId, currentShopId)).thenReturn(true);
    }
  }

  private void stubOccupiedTable() {
    when(commonMapper.lockOccupiedInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenReturn(existingInvoice.getTableId());
  }

  private void stubTableRelease() {
    when(commonMapper.hasUnpaidTableInvoices(existingInvoice.getTableId())).thenReturn(false);
    when(commonMapper.releaseInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenReturn(1);
  }

  private void assertSingleViolation(String field, String message) {
    var violations = validator.validate(validRequest);
    assertEquals(1, violations.size(), violations.toString());
    var violation = violations.iterator().next();
    assertEquals(field, violation.getPropertyPath().toString());
    assertEquals(message, violation.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_Success_AsOwner_TC001(int targetStatus) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    stubTableRelease();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId);

    verify(commonMapper).lockInvoice(invoiceId);
    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_Success_AsManagerWithoutCreatorOwnership_TC002(int targetStatus) {
    existingInvoice.setProfileId(UUID.fromString("00000000-0000-0000-0000-000000000901"));
    stubShopPermission("MANAGER");
    stubOccupiedTable();
    stubTableRelease();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "MANAGER", currentShopId, currentProfileId);
    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_Success_AsStaff_TC003(int targetStatus) {
    stubShopPermission("STAFF");
    stubOccupiedTable();
    stubTableRelease();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "STAFF", currentShopId, currentProfileId);

    verify(commonMapper).lockInvoice(invoiceId);
    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_Success_AsOwnerForAnotherShop_TC004(int targetStatus) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    stubTableRelease();
    validRequest.setStatus(targetStatus);
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000402");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "OWNER", otherShopId, currentProfileId);

    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_UpdatesInvoiceBeforeReleasingTable_TC005(int targetStatus) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    stubTableRelease();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId);

    var order = org.mockito.Mockito.inOrder(commonMapper, statusInvoiceMapper);
    order.verify(commonMapper).lockInvoice(invoiceId);
    order
        .verify(commonMapper)
        .lockOccupiedInvoiceTable(existingInvoice.getTableId(), currentShopId);
    order.verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
    order.verify(commonMapper).hasUnpaidTableInvoices(existingInvoice.getTableId());
    order.verify(commonMapper).releaseInvoiceTable(existingInvoice.getTableId(), currentShopId);
  }

  @ParameterizedTest
  @CsvSource({"owner, 1", "owner, 2", "manager, 1", "manager, 2", "staff, 1", "staff, 2"})
  void process_Success_WithCaseInsensitiveRole_TC006(String role, int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission(role);
    stubOccupiedTable();
    stubTableRelease();
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, role, currentShopId, currentProfileId);

    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
    verify(commonMapper).releaseInvoiceTable(existingInvoice.getTableId(), currentShopId);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_Success_AsOwnerWithoutAssignedShop_TC007(int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission("OWNER");
    stubOccupiedTable();
    stubTableRelease();
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);

    statusInvoiceService.process(validRequest, "OWNER", null, currentProfileId);

    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_KeepsTableOccupiedWhenAnotherUnpaidInvoiceRemains_TC008(int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission("OWNER");
    stubOccupiedTable();
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);
    when(commonMapper.hasUnpaidTableInvoices(existingInvoice.getTableId())).thenReturn(true);

    statusInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId);

    var savedStatus = org.mockito.ArgumentCaptor.forClass(Integer.class);
    verify(statusInvoiceMapper)
        .updateInvoiceStatus(
            org.mockito.ArgumentMatchers.eq(invoiceId),
            org.mockito.ArgumentMatchers.eq(currentShopId),
            savedStatus.capture());
    assertEquals(targetStatus, savedStatus.getValue());
    var order = org.mockito.Mockito.inOrder(statusInvoiceMapper, commonMapper);
    order.verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
    order.verify(commonMapper).hasUnpaidTableInvoices(existingInvoice.getTableId());
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / STATUS & PERMISSION CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsDataNotFoundException_WhenInvoiceNotFound_TC009(int targetStatus) {
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Invoice not found", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsInvalidRequestException_WhenRoleIsInvalid_TC010(int targetStatus) {
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> statusInvoiceService.process(validRequest, "CASHIER", currentShopId, null));

    assertEquals("You do not have permission to create invoices", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsInvalidRequestException_WhenStaffUserIdIsNull_TC011(int targetStatus) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> statusInvoiceService.process(validRequest, "STAFF", currentShopId, null));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsInvalidRequestException_WhenUserIsNotInvoiceCreator_TC012(int targetStatus) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    UUID otherProfileId = UUID.fromString("00000000-0000-0000-0000-000000000403");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.checkShopIdIsExisted(otherProfileId, currentShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(validRequest, "STAFF", currentShopId, otherProfileId));

    assertEquals("You do not have permission to change this invoice", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsInvalidRequestException_WhenStaffShopDiffers_TC013(int targetStatus) {
    validRequest.setStatus(targetStatus);
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000404");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(validRequest, "STAFF", otherShopId, currentProfileId));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsInvalidRequestException_WhenStaffShopIsNull_TC014(int targetStatus) {
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> statusInvoiceService.process(validRequest, "STAFF", null, currentProfileId));

    assertEquals("User is not assigned to any shop", exception.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"1, 1", "1, 2", "2, 1", "2, 2"})
  void process_ThrowsInvalidRequestException_WhenInvoiceAlreadyCompletedOrCancelled_TC015(
      int targetStatus, String storedStatus) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    existingInvoice.setStatus(storedStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals(
        "Cannot change an invoice that is already completed or cancelled", exception.getMessage());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {-1, 0, 3})
  void process_RejectsInvalidTargetStatusBeforeQuery_TC016(Integer status) {
    validRequest.setStatus(status);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Invoice status must be 1 (paid) or 2 (cancelled)", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(commonMapper, statusInvoiceMapper);
  }

  @ParameterizedTest
  @CsvSource({"1, false", "1,", "2, false", "2,"})
  void process_RejectsDeletedShop_TC017(int targetStatus, Boolean databaseResult) {
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(databaseResult);

    var error =
        assertThrows(
            DataNotFoundException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));
    assertEquals("Data not found", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
  }

  @ParameterizedTest
  @CsvSource({"1, false", "1,", "2, false", "2,"})
  void process_RejectsInactiveShopMembership_TC018(int targetStatus, Boolean databaseResult) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.checkShopIdIsExisted(currentProfileId, currentShopId))
        .thenReturn(databaseResult);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "STAFF", currentShopId, currentProfileId));
    assertEquals("You do not have permission to access this shop", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_RejectsInvoiceWithoutCreatorForStaff_TC019(int targetStatus) {
    validRequest.setStatus(targetStatus);
    existingInvoice.setProfileId(null);
    stubShopPermission("STAFF");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "STAFF", currentShopId, currentProfileId));

    assertEquals("You do not have permission to change this invoice", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / TABLE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_RejectsUnavailableTable_TC020(int targetStatus) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.lockOccupiedInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenReturn(null);
    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));
    assertEquals(
        "Current table is unavailable or does not belong to this shop", error.getMessage());
    verify(statusInvoiceMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_RejectsMissingTable_TC021(int targetStatus) {
    stubShopPermission("OWNER");
    validRequest.setStatus(targetStatus);
    existingInvoice.setTableId(null);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals(
        "Current table is unavailable or does not belong to this shop", error.getMessage());
    verify(commonMapper, never()).lockOccupiedInvoiceTable(any(), any());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_RejectsFailedTableRelease_TC022(int targetStatus) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);
    when(commonMapper.releaseInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenReturn(0);
    var error =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));
    assertEquals("Table cannot be released. Please reload and try again", error.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @ParameterizedTest
  @CsvSource({"1, 0", "1, 2", "1, -1", "2, 0", "2, 2", "2, -1"})
  void process_ThrowsInvalidRequestException_WhenUnexpectedRowsAffected_TC023(
      int targetStatus, int rowsAffected) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(any(), any(), any())).thenReturn(rowsAffected);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
    assertEquals(
        "Invoice status cannot be updated. Please reload and try again", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC024(int targetStatus) {
    stubShopPermission("OWNER");
    stubOccupiedTable();
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(any(), any(), any()))
        .thenThrow(new DataAccessException("DB failure") {});

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("DB failure", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_PropagatesInvoiceLockFailure_TC025(int targetStatus) {
    validRequest.setStatus(targetStatus);
    when(commonMapper.lockInvoice(invoiceId))
        .thenThrow(new DataAccessException("Invoice lock failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Invoice lock failed", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_PropagatesTableLockFailure_TC026(int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission("OWNER");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.lockOccupiedInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenThrow(new DataAccessException("Table lock failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Table lock failed", error.getMessage());
    org.mockito.Mockito.verifyNoInteractions(statusInvoiceMapper);
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_PropagatesTableReleaseFailure_TC027(int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission("OWNER");
    stubOccupiedTable();
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);
    when(commonMapper.releaseInvoiceTable(existingInvoice.getTableId(), currentShopId))
        .thenThrow(new DataAccessException("Table release failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Table release failed", error.getMessage());
    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void process_PropagatesRemainingInvoiceQueryFailure_TC028(int targetStatus) {
    validRequest.setStatus(targetStatus);
    stubShopPermission("OWNER");
    stubOccupiedTable();
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(statusInvoiceMapper.updateInvoiceStatus(invoiceId, currentShopId, targetStatus))
        .thenReturn(1);
    when(commonMapper.hasUnpaidTableInvoices(existingInvoice.getTableId()))
        .thenThrow(new DataAccessException("Remaining invoice query failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () ->
                statusInvoiceService.process(
                    validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Remaining invoice query failed", error.getMessage());
    verify(statusInvoiceMapper).updateInvoiceStatus(invoiceId, currentShopId, targetStatus);
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void requestValidation_AcceptsPaidAndCancelledStatus_TC029(int status) {
    validRequest.setStatus(status);

    assertEquals(0, validator.validate(validRequest).size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
  void requestValidation_RejectsMissingInvoiceId_TC030(int status) {
    validRequest.setStatus(status);
    validRequest.setInvoiceId(null);

    assertSingleViolation("invoiceId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_RejectsMissingStatus_TC031() {
    validRequest.setStatus(null);

    assertSingleViolation("status", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 3})
  void requestValidation_RejectsStatusOutsideAllowedRange_TC032(int status) {
    validRequest.setStatus(status);

    assertSingleViolation("status", ValidationMessage.Msg.STATUS_INVALID);
  }

  @Test
  void requestValidation_RejectsBothMissingFields_TC033() {
    validRequest.setInvoiceId(null);
    validRequest.setStatus(null);

    var violations = validator.validate(validRequest);

    assertEquals(2, violations.size());
    assertEquals(
        java.util.Set.of("invoiceId", "status"),
        violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet()));
    assertEquals(
        java.util.Map.of(
            "invoiceId",
            ValidationMessage.Msg.FIELD_REQUIRED,
            "status",
            ValidationMessage.Msg.FIELD_REQUIRED),
        violations.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    v -> v.getPropertyPath().toString(),
                    jakarta.validation.ConstraintViolation::getMessage)));
  }
}
