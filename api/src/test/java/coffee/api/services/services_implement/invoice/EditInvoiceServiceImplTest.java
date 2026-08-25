package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.EditInvoiceItemRequest;
import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateInvoiceMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class EditInvoiceServiceImplTest {

  @Mock private UpdateInvoiceMapper updateInvoiceMapper;

  @InjectMocks private EditInvoiceServiceImpl editInvoiceService;

  private EditInvoiceRequest validRequest;
  private InvoiceResult existingInvoice;
  private UUID invoiceId;
  private UUID currentShopId;
  private UUID currentProfileId;

  @BeforeEach
  void setUp() {
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentProfileId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    EditInvoiceItemRequest item1 = new EditInvoiceItemRequest();
    item1.setDrinkDetailId(UUID.fromString("dd100000-0000-0000-0000-000000000002"));
    item1.setQuantity(2);
    item1.setUnitPrice(new BigDecimal("25000.00"));
    item1.setNote("Less ice");

    EditInvoiceItemRequest item2 = new EditInvoiceItemRequest();
    item2.setDrinkDetailId(UUID.fromString("dd100000-0000-0000-0000-000000000005"));
    item2.setQuantity(1);
    item2.setUnitPrice(new BigDecimal("29000.00"));
    item2.setNote(null);

    validRequest = new EditInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);
    validRequest.setTableNumber(5);
    validRequest.setItems(List.of(item1, item2));

    existingInvoice = new InvoiceResult();
    existingInvoice.setInvoiceId(invoiceId);
    existingInvoice.setShopId(currentShopId);
    existingInvoice.setStatus("0");
  }

  @Test
  void process_Success_AsOwner_TC001() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(currentShopId)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    editInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId);

    verify(updateInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(updateInvoiceMapper, times(1))
        .updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(currentShopId));
    verify(updateInvoiceMapper, times(1)).deleteInvoiceDetails(invoiceId);
    verify(updateInvoiceMapper, times(1)).batchInsertInvoiceDetails(eq(invoiceId), any());
  }

  @Test
  void process_Success_AsStaff_TC002() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.isInvoiceCreatedBy(invoiceId, currentProfileId)).thenReturn(true);
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("STAFF"), eq(currentShopId)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    editInvoiceService.process(validRequest, "STAFF", currentShopId, currentProfileId);

    verify(updateInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(updateInvoiceMapper, times(1))
        .updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("STAFF"), eq(currentShopId));
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenInvoiceNotFound_TC003() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Invoice not found", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenRoleIsInvalid_TC004() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "CASHIER", currentShopId, null));

    assertEquals("You do not have permission to edit this invoice", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffUserIdIsNull_TC005() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "STAFF", currentShopId, null));

    assertEquals("You do not have permission to edit this invoice", exception.getMessage());
  }

  @Test
  void process_Success_AsManagerWithoutCreatorOwnership_TC004() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("MANAGER"), eq(currentShopId)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    editInvoiceService.process(validRequest, "MANAGER", currentShopId, null);
    verify(updateInvoiceMapper)
        .updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("MANAGER"), eq(currentShopId));
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenUserIsNotInvoiceCreator_TC005() {
    UUID otherProfileId = UUID.randomUUID();
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.isInvoiceCreatedBy(invoiceId, otherProfileId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "STAFF", currentShopId, otherProfileId));

    assertEquals("You do not have permission to edit this invoice", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopDiffers_TC006() {
    UUID otherShopId = UUID.randomUUID();
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "STAFF", otherShopId, currentProfileId));

    assertEquals(
        "You do not have permission to edit invoice of another shop", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIsNull_TC007() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "STAFF", null, currentProfileId));

    assertEquals(
        "You do not have permission to edit invoice of another shop", exception.getMessage());
  }

  @Test
  void process_Success_AsOwnerForAnotherShop_TC008() {
    UUID otherShopId = UUID.randomUUID();
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(otherShopId)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    editInvoiceService.process(validRequest, "OWNER", otherShopId, null);

    verify(updateInvoiceMapper)
        .updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(otherShopId));
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInvoiceAlreadyCompletedOrCancelled_TC009() {
    existingInvoice.setStatus("1");
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editInvoiceService.process(validRequest, "OWNER", currentShopId, null));

    assertEquals(
        "Cannot edit an invoice that is already completed or cancelled", exception.getMessage());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenNoRowUpdated_TC009() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.updateInvoice(any(), any(), any(), any(), any())).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editInvoiceService.process(validRequest, "OWNER", currentShopId, currentProfileId));

    assertEquals("Invoice not found or cannot be edited", exception.getMessage());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC010() {
    when(updateInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(updateInvoiceMapper.isInvoiceCreatedBy(invoiceId, currentProfileId)).thenReturn(true);
    when(updateInvoiceMapper.updateInvoice(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database connection timeout") {});

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                editInvoiceService.process(validRequest, "STAFF", currentShopId, currentProfileId));

    assertEquals("Database connection timeout", exception.getMessage());
  }
}
