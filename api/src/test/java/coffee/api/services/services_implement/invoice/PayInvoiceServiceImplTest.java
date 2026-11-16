package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.invoice.PayInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.UpdateInvoiceStatusMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceStatusChanger;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class PayInvoiceServiceImplTest {

  private static final int TARGET = 1;

  @Mock private UpdateInvoiceStatusMapper statusMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private PayInvoiceServiceImpl service;
  private PayInvoiceRequest validRequest;
  private InvoiceResult existingInvoice;
  private UUID invoiceId;
  private UUID shopId;
  private UUID profileId;
  private UUID tableId;

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
        new PayInvoiceServiceImpl(
            new InvoiceStatusChanger(
                statusMapper, new InvoiceValidator(createInvoiceMapper, commonMapper)));
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    profileId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    tableId = UUID.fromString("00000000-0000-0000-0000-000000000401");

    validRequest = new PayInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);

    existingInvoice = new InvoiceResult();
    existingInvoice.setInvoiceId(invoiceId);
    existingInvoice.setShopId(shopId);
    existingInvoice.setStatus("0");
    existingInvoice.setProfileId(profileId);
    existingInvoice.setTableId(tableId);
  }

  // =========================================================================
  // HELPERS
  // =========================================================================

  private void stubInvoiceAndPermission(String role) {
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (!"OWNER".equals(role)) {
      when(commonMapper.checkShopIdIsExisted(profileId, shopId)).thenReturn(true);
    }
  }

  private void stubTableAndUpdate() {
    when(commonMapper.lockOccupiedInvoiceTable(tableId, shopId)).thenReturn(tableId);
    when(statusMapper.updateInvoiceStatus(invoiceId, shopId, TARGET)).thenReturn(1);
  }

  private void stubTableRelease() {
    when(commonMapper.hasUnpaidTableInvoices(tableId)).thenReturn(false);
    when(commonMapper.releaseInvoiceTable(tableId, shopId)).thenReturn(1);
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_Success_SetsServerDecidedStatus_TC001(String role) {
    stubInvoiceAndPermission(role);
    stubTableAndUpdate();
    stubTableRelease();

    service.process(validRequest, role, shopId, profileId);

    var order = inOrder(commonMapper, statusMapper);
    order.verify(commonMapper).lockInvoice(invoiceId);
    order.verify(statusMapper).updateInvoiceStatus(invoiceId, shopId, TARGET);
    order.verify(commonMapper).releaseInvoiceTable(tableId, shopId);
  }

  @Test
  void process_Success_ManagerChangesInvoiceOfAnotherStaff_TC002() {
    existingInvoice.setProfileId(UUID.fromString("00000000-0000-0000-0000-000000000901"));
    stubInvoiceAndPermission("MANAGER");
    stubTableAndUpdate();
    stubTableRelease();

    service.process(validRequest, "MANAGER", shopId, profileId);

    verify(statusMapper).updateInvoiceStatus(invoiceId, shopId, TARGET);
  }

  @Test
  void process_KeepsTableOccupied_WhenAnotherUnpaidInvoiceRemains_TC003() {
    stubInvoiceAndPermission("OWNER");
    stubTableAndUpdate();
    when(commonMapper.hasUnpaidTableInvoices(tableId)).thenReturn(true);

    service.process(validRequest, "OWNER", shopId, profileId);

    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - FAILURES
  // =========================================================================

  @Test
  void process_ThrowsNotFound_WhenInvoiceDoesNotExist_TC004() {
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals(ErrorMessage.INVOICE_NOT_FOUND, error.getMessage());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenStaffIsNotInvoiceCreator_TC005() {
    UUID otherProfileId = UUID.fromString("00000000-0000-0000-0000-000000000403");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(otherProfileId, shopId)).thenReturn(true);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "STAFF", shopId, otherProfileId));

    assertEquals("You do not have permission to change this invoice", error.getMessage());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenStaffUsesAnotherShop_TC006() {
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000404");
    when(commonMapper.lockInvoice(invoiceId)).thenReturn(existingInvoice);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "STAFF", otherShopId, profileId));

    assertEquals("You do not have permission to access this shop", error.getMessage());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2"})
  void process_ThrowsInvalid_WhenInvoiceAlreadyCompletedOrCancelled_TC007(String storedStatus) {
    stubInvoiceAndPermission("OWNER");
    existingInvoice.setStatus(storedStatus);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals(
        "Cannot change an invoice that is already completed or cancelled", error.getMessage());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenTableIsNotOccupied_TC008() {
    stubInvoiceAndPermission("OWNER");
    when(commonMapper.lockOccupiedInvoiceTable(tableId, shopId)).thenReturn(null);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals(ErrorMessage.CURRENT_TABLE_UNAVAILABLE, error.getMessage());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenInvoiceHasNoTable_TC009() {
    existingInvoice.setTableId(null);
    stubInvoiceAndPermission("OWNER");

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals(ErrorMessage.CURRENT_TABLE_UNAVAILABLE, error.getMessage());
    verify(commonMapper, never()).lockOccupiedInvoiceTable(any(), any());
    verify(statusMapper, never()).updateInvoiceStatus(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenUpdateAffectsNoRow_TC010() {
    stubInvoiceAndPermission("OWNER");
    when(commonMapper.lockOccupiedInvoiceTable(tableId, shopId)).thenReturn(tableId);
    when(statusMapper.updateInvoiceStatus(invoiceId, shopId, TARGET)).thenReturn(0);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals(
        "Invoice status cannot be updated. Please reload and try again", error.getMessage());
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  @Test
  void process_ThrowsInvalid_WhenTableReleaseFails_TC011() {
    stubInvoiceAndPermission("OWNER");
    stubTableAndUpdate();
    when(commonMapper.hasUnpaidTableInvoices(tableId)).thenReturn(false);
    when(commonMapper.releaseInvoiceTable(tableId, shopId)).thenReturn(0);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, "OWNER", shopId, profileId));

    assertEquals("Table cannot be released. Please reload and try again", error.getMessage());
  }

  @Test
  void process_PropagatesDataAccessException_WhenUpdateFails_TC012() {
    stubInvoiceAndPermission("OWNER");
    when(commonMapper.lockOccupiedInvoiceTable(tableId, shopId)).thenReturn(tableId);
    when(statusMapper.updateInvoiceStatus(invoiceId, shopId, TARGET))
        .thenThrow(new DataAccessException("db down") {});

    assertThrows(
        DataAccessException.class, () -> service.process(validRequest, "OWNER", shopId, profileId));
    verify(commonMapper, never()).releaseInvoiceTable(any(), any());
  }

  // =========================================================================
  // REQUEST VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenInvoiceIdIsPresent_TC013() {
    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_Fails_WhenInvoiceIdIsNull_TC014() {
    validRequest.setInvoiceId(null);

    var violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
