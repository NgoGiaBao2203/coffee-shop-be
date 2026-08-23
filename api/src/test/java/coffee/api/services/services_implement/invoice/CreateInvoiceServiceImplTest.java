package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.dto.request.invoice.CreateInvoiceItemRequest;
import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateInvoiceMapper;
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
public class CreateInvoiceServiceImplTest {

  @Mock private CreateInvoiceMapper createInvoiceMapper;

  @InjectMocks private CreateInvoiceServiceImpl createInvoiceService;

  private CreateInvoiceRequest validRequest;
  private UUID currentShopId;
  private UUID currentUserId;

  @BeforeEach
  void setUp() {
    currentShopId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    currentUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    CreateInvoiceItemRequest item1 = new CreateInvoiceItemRequest();
    item1.setDrinkDetailId(UUID.randomUUID());
    item1.setQuantity(2);
    item1.setUnitPrice(new BigDecimal("25000.00"));
    item1.setNote("Less ice");

    CreateInvoiceItemRequest item2 = new CreateInvoiceItemRequest();
    item2.setDrinkDetailId(UUID.randomUUID());
    item2.setQuantity(1);
    item2.setUnitPrice(new BigDecimal("30000.00"));
    item2.setNote(null);

    validRequest = new CreateInvoiceRequest();
    validRequest.setShopId(currentShopId);
    validRequest.setTableNumber(5);
    validRequest.setItems(List.of(item1, item2));
  }

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(createInvoiceMapper.insertInvoice(
            any(), eq(currentShopId), eq(currentUserId), eq(5), any(), anyInt()))
        .thenReturn(1);
    when(createInvoiceMapper.insertInvoiceDetails(any(), any())).thenReturn(2);

    // Act
    CreateInvoiceResponse response =
        createInvoiceService.process(validRequest, "OWNER", currentShopId, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("S0001", response.getCode());
    assertEquals("Create invoice successfully", response.getMessage());

    verify(createInvoiceMapper, times(1))
        .insertInvoice(
            any(),
            eq(currentShopId),
            eq(currentUserId),
            eq(5),
            eq(new BigDecimal("80000.00")),
            eq(0));
    verify(createInvoiceMapper, times(1)).insertInvoiceDetails(any(), any());
  }

  @Test
  void process_Success_AsStaff_TC002() {
    // Arrange
    validRequest.setShopId(null);

    when(createInvoiceMapper.insertInvoice(
            any(), eq(currentShopId), eq(currentUserId), eq(5), any(), anyInt()))
        .thenReturn(1);
    when(createInvoiceMapper.insertInvoiceDetails(any(), any())).thenReturn(2);

    // Act
    CreateInvoiceResponse response =
        createInvoiceService.process(validRequest, "STAFF", currentShopId, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("S0001", response.getCode());
    assertEquals("Create invoice successfully", response.getMessage());

    verify(createInvoiceMapper, times(1))
        .insertInvoice(
            any(),
            eq(currentShopId),
            eq(currentUserId),
            eq(5),
            eq(new BigDecimal("80000.00")),
            eq(0));
    verify(createInvoiceMapper, times(1)).insertInvoiceDetails(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenOwnerProvidesNullShopId_TC003() {
    // Arrange
    validRequest.setShopId(null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createInvoiceService.process(validRequest, "OWNER", currentShopId, currentUserId));

    assertEquals("Shop ID is required for OWNER", exception.getMessage());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC004() {
    // Arrange
    when(createInvoiceMapper.insertInvoice(any(), any(), any(), anyInt(), any(), anyInt()))
        .thenThrow(new DataAccessException("Database insert error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                createInvoiceService.process(
                    validRequest, "MANAGER", currentShopId, currentUserId));

    assertEquals("Database insert error", exception.getMessage());
  }
}
