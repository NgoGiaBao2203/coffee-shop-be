package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.SearchInvoiceRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetInvoiceMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
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
public class GetInvoiceServiceImplTest {

  @Mock private GetInvoiceMapper getInvoiceMapper;

  @InjectMocks private GetInvoiceServiceImpl getInvoiceService;

  private SearchInvoiceRequest validRequest;
  private InvoiceResult sampleInvoice;
  private UUID currentShopId;

  @BeforeEach
  void setUp() {
    currentShopId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    validRequest = new SearchInvoiceRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Table 1");
    validRequest.setSortBy("createdAt");
    validRequest.setSortDirection(SortDirection.DESC);

    InvoiceResult.InvoiceItemResult item = new InvoiceResult.InvoiceItemResult();
    item.setInvoiceDetailId(UUID.randomUUID());
    item.setDrinkId(UUID.randomUUID());
    item.setDrinkName("Cà phê sữa đá");
    item.setSize("M");
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("25000.00"));
    item.setItemTotal(new BigDecimal("50000.00"));
    item.setNote("Ít đường");

    sampleInvoice = new InvoiceResult();
    sampleInvoice.setInvoiceId(UUID.randomUUID());
    sampleInvoice.setShopId(currentShopId);
    sampleInvoice.setShopName("Coffee Shop Central");
    sampleInvoice.setStaffName("Nguyen Van A");
    sampleInvoice.setTableNumber(1);
    sampleInvoice.setTotalAmount(new BigDecimal("50000.00"));
    sampleInvoice.setStatus("1");
    sampleInvoice.setCreatedAt(LocalDateTime.now());
    sampleInvoice.setUpdateAt(LocalDateTime.now());
    sampleInvoice.setIsDeleted(false);
    sampleInvoice.setItems(List.of(item));
  }

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(getInvoiceMapper.countInvoicesFiltered(
            eq("Table 1"), eq("OWNER"), eq(currentShopId), isNull(), isNull()))
        .thenReturn(1L);

    when(getInvoiceMapper.getInvoicesFiltered(
            eq("Table 1"),
            eq("createdAt"),
            eq("DESC"),
            eq(10),
            eq(0),
            eq("OWNER"),
            eq(currentShopId),
            isNull(),
            isNull()))
        .thenReturn(List.of(sampleInvoice));

    // Act
    PageResponse<InvoiceResult> response =
        getInvoiceService.process(validRequest, "OWNER", currentShopId);

    // Assert
    assertNotNull(response);
    assertEquals("Get invoices successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals("Đã thanh toán", response.getItems().get(0).getStatus());
    assertEquals(1, response.getItems().get(0).getItems().size());
    assertEquals("Cà phê sữa đá", response.getItems().get(0).getItems().get(0).getDrinkName());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getInvoiceMapper, times(1)).countInvoicesFiltered(any(), any(), any(), any(), any());
    verify(getInvoiceMapper, times(1))
        .getInvoicesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any(), any());
  }

  @Test
  void process_Success_EmptyResults_TC002() {
    // Arrange
    when(getInvoiceMapper.countInvoicesFiltered(any(), any(), any(), any(), any())).thenReturn(0L);
    when(getInvoiceMapper.getInvoicesFiltered(
            any(), any(), any(), anyInt(), anyInt(), any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<InvoiceResult> response =
        getInvoiceService.process(validRequest, "STAFF", currentShopId);

    // Assert
    assertNotNull(response);
    assertEquals(0, response.getItems().size());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC003() {
    // Arrange
    when(getInvoiceMapper.countInvoicesFiltered(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database connection timeout") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getInvoiceService.process(validRequest, "MANAGER", currentShopId));

    assertEquals("Database connection timeout", exception.getMessage());
  }

  @Test
  void process_Success_WhenMapperReturnsNull_TC004() {
    // Arrange
    when(getInvoiceMapper.countInvoicesFiltered(any(), any(), any(), any(), any())).thenReturn(0L);
    when(getInvoiceMapper.getInvoicesFiltered(
            any(), any(), any(), anyInt(), anyInt(), any(), any(), any(), any()))
        .thenReturn(null);

    // Act
    PageResponse<InvoiceResult> response =
        getInvoiceService.process(validRequest, "STAFF", currentShopId);

    // Assert
    assertNotNull(response);
    assertEquals(0, response.getItems().size());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
  }

  @Test
  void process_Success_NormalizeDifferentStatuses_TC005() {
    // Arrange
    InvoiceResult inv0 = new InvoiceResult();
    inv0.setStatus("0");
    InvoiceResult invPending = new InvoiceResult();
    invPending.setStatus("PENDING");
    InvoiceResult inv1 = new InvoiceResult();
    inv1.setStatus("1");
    InvoiceResult invCompleted = new InvoiceResult();
    invCompleted.setStatus("COMPLETED");
    InvoiceResult inv2 = new InvoiceResult();
    inv2.setStatus("2");
    InvoiceResult invCancelled = new InvoiceResult();
    invCancelled.setStatus("CANCELLED");
    InvoiceResult invNull = new InvoiceResult();
    invNull.setStatus(null);
    InvoiceResult invOther = new InvoiceResult();
    invOther.setStatus("UNKNOWN_VALUE");

    when(getInvoiceMapper.countInvoicesFiltered(any(), any(), any(), any(), any())).thenReturn(8L);
    when(getInvoiceMapper.getInvoicesFiltered(
            any(), any(), any(), anyInt(), anyInt(), any(), any(), any(), any()))
        .thenReturn(
            List.of(inv0, invPending, inv1, invCompleted, inv2, invCancelled, invNull, invOther));

    // Act
    PageResponse<InvoiceResult> response =
        getInvoiceService.process(validRequest, "OWNER", currentShopId);

    // Assert
    assertNotNull(response);
    assertEquals(8, response.getItems().size());
    assertEquals("Đang phục vụ", response.getItems().get(0).getStatus());
    assertEquals("Đang phục vụ", response.getItems().get(1).getStatus());
    assertEquals("Đã thanh toán", response.getItems().get(2).getStatus());
    assertEquals("Đã thanh toán", response.getItems().get(3).getStatus());
    assertEquals("Đã hủy", response.getItems().get(4).getStatus());
    assertEquals("Đã hủy", response.getItems().get(5).getStatus());
    assertEquals("Không xác định", response.getItems().get(6).getStatus());
    assertEquals("Không xác định", response.getItems().get(7).getStatus());
  }
}
