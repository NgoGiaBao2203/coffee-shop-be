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
import coffee.api.exceptions.DataNotFoundException;
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
public class EditInvoiceServiceImplTest {

  @Mock private UpdateInvoiceMapper updateInvoiceMapper;

  @InjectMocks private EditInvoiceServiceImpl editInvoiceService;

  private EditInvoiceRequest validRequest;
  private UUID invoiceId;
  private UUID currentShopId;

  @BeforeEach
  void setUp() {
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

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
  }

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(null)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    // Act
    editInvoiceService.process(validRequest, "OWNER", null);

    // Assert
    verify(updateInvoiceMapper, times(1))
        .updateInvoice(eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("OWNER"), eq(null));
    verify(updateInvoiceMapper, times(1)).deleteInvoiceDetails(invoiceId);
    verify(updateInvoiceMapper, times(1)).batchInsertInvoiceDetails(eq(invoiceId), any());
  }

  @Test
  void process_Success_AsStaff_TC002() {
    // Arrange
    when(updateInvoiceMapper.updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("STAFF"), eq(currentShopId)))
        .thenReturn(1);
    when(updateInvoiceMapper.deleteInvoiceDetails(invoiceId)).thenReturn(2);
    when(updateInvoiceMapper.batchInsertInvoiceDetails(eq(invoiceId), any())).thenReturn(2);

    // Act
    editInvoiceService.process(validRequest, "STAFF", currentShopId);

    // Assert
    verify(updateInvoiceMapper, times(1))
        .updateInvoice(
            eq(invoiceId), eq(5), eq(new BigDecimal("79000.00")), eq("STAFF"), eq(currentShopId));
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenNoRowUpdated_TC003() {
    // Arrange
    when(updateInvoiceMapper.updateInvoice(any(), any(), any(), any(), any())).thenReturn(0);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> editInvoiceService.process(validRequest, "OWNER", null));

    assertEquals("Invoice not found or cannot be edited", exception.getMessage());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC004() {
    // Arrange
    when(updateInvoiceMapper.updateInvoice(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database connection timeout") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> editInvoiceService.process(validRequest, "STAFF", currentShopId));

    assertEquals("Database connection timeout", exception.getMessage());
  }
}
