package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.CancelInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CancelInvoiceMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class CancelInvoiceServiceImplTest {

  @Mock private CancelInvoiceMapper cancelInvoiceMapper;

  @InjectMocks private CancelInvoiceServiceImpl cancelInvoiceService;

  private CancelInvoiceRequest validRequest;
  private InvoiceResult existingInvoice;
  private UUID invoiceId;
  private UUID currentShopId;

  @BeforeEach
  void setUp() {
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    validRequest = new CancelInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);

    existingInvoice = new InvoiceResult();
    existingInvoice.setInvoiceId(invoiceId);
    existingInvoice.setShopId(currentShopId);
    existingInvoice.setStatus("0");
  }

  @Test
  void process_Success_AsOwner_TC001() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(cancelInvoiceMapper.cancelInvoice(eq(invoiceId), eq("OWNER"), eq(null))).thenReturn(1);

    cancelInvoiceService.process(validRequest, "OWNER", null);

    verify(cancelInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(cancelInvoiceMapper, times(1)).cancelInvoice(eq(invoiceId), eq("OWNER"), eq(null));
  }

  @Test
  void process_Success_AsStaff_TC002() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(cancelInvoiceMapper.cancelInvoice(eq(invoiceId), eq("STAFF"), eq(currentShopId)))
        .thenReturn(1);

    cancelInvoiceService.process(validRequest, "STAFF", currentShopId);

    verify(cancelInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(cancelInvoiceMapper, times(1))
        .cancelInvoice(eq(invoiceId), eq("STAFF"), eq(currentShopId));
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenInvoiceNotFound_TC003() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> cancelInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("Invoice not found", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopDiffers_TC004() {
    UUID otherShopId = UUID.randomUUID();
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> cancelInvoiceService.process(validRequest, "STAFF", otherShopId));

    assertEquals(
        "You do not have permission to cancel invoice of another shop", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIsNull_TC005() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> cancelInvoiceService.process(validRequest, "STAFF", null));

    assertEquals(
        "You do not have permission to cancel invoice of another shop", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInvoiceAlreadyCompletedOrCancelled_TC006() {
    existingInvoice.setStatus("2");
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> cancelInvoiceService.process(validRequest, "OWNER", null));

    assertEquals(
        "Cannot cancel an invoice that is already completed or cancelled", exception.getMessage());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenRowsAffectedIsZero_TC007() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(cancelInvoiceMapper.cancelInvoice(any(), any(), any())).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> cancelInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("Invoice cannot be cancelled", exception.getMessage());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC008() {
    when(cancelInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(cancelInvoiceMapper.cancelInvoice(any(), any(), any()))
        .thenThrow(new DataAccessException("DB failure") {});

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> cancelInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("DB failure", exception.getMessage());
  }
}
