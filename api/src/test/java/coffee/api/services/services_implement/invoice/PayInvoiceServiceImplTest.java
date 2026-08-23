package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.PayInvoiceRequest;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.PayInvoiceMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class PayInvoiceServiceImplTest {

  @Mock private PayInvoiceMapper payInvoiceMapper;

  @InjectMocks private PayInvoiceServiceImpl payInvoiceService;

  private PayInvoiceRequest validRequest;
  private InvoiceResult existingInvoice;
  private UUID invoiceId;
  private UUID currentShopId;

  @BeforeEach
  void setUp() {
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    validRequest = new PayInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);

    existingInvoice = new InvoiceResult();
    existingInvoice.setInvoiceId(invoiceId);
    existingInvoice.setShopId(currentShopId);
    existingInvoice.setStatus("0");
  }

  @Test
  void process_Success_AsOwner_TC001() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(payInvoiceMapper.payInvoice(eq(invoiceId), eq("OWNER"), eq(null))).thenReturn(1);

    payInvoiceService.process(validRequest, "OWNER", null);

    verify(payInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(payInvoiceMapper, times(1)).payInvoice(eq(invoiceId), eq("OWNER"), eq(null));
  }

  @Test
  void process_Success_AsStaff_TC002() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(payInvoiceMapper.payInvoice(eq(invoiceId), eq("STAFF"), eq(currentShopId))).thenReturn(1);

    payInvoiceService.process(validRequest, "STAFF", currentShopId);

    verify(payInvoiceMapper, times(1)).findInvoiceById(invoiceId);
    verify(payInvoiceMapper, times(1)).payInvoice(eq(invoiceId), eq("STAFF"), eq(currentShopId));
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenInvoiceNotFound_TC003() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> payInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("Invoice not found", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopDiffers_TC004() {
    UUID otherShopId = UUID.randomUUID();
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> payInvoiceService.process(validRequest, "STAFF", otherShopId));

    assertEquals(
        "You do not have permission to pay invoice of another shop", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIsNull_TC005() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> payInvoiceService.process(validRequest, "STAFF", null));

    assertEquals(
        "You do not have permission to pay invoice of another shop", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInvoiceAlreadyCompletedOrCancelled_TC006() {
    existingInvoice.setStatus("1");
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> payInvoiceService.process(validRequest, "OWNER", null));

    assertEquals(
        "Can not pay an invoice that is already completed or cancelled", exception.getMessage());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenRowsAffectedIsZero_TC007() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(payInvoiceMapper.payInvoice(any(), any(), any())).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> payInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("Invoice cannot be paid", exception.getMessage());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC008() {
    when(payInvoiceMapper.findInvoiceById(invoiceId)).thenReturn(existingInvoice);
    when(payInvoiceMapper.payInvoice(any(), any(), any()))
        .thenThrow(new DataAccessException("DB failure") {});

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> payInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("DB failure", exception.getMessage());
  }
}
