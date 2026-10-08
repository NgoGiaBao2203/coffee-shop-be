package coffee.api.services.services_implement.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop.DeleteShopRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class DeleteShopServiceImplTest {

  @Mock private DeleteShopMapper deleteShopMapper;

  @InjectMocks private DeleteShopServiceImpl deleteShopService;

  private Validator validator;
  private DeleteShopRequest validRequest;
  private final UUID testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    validRequest = new DeleteShopRequest();
    validRequest.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_CascadeSoftDeleteAllRelatedEntities_TC001() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(0);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopService.process(validRequest, testUserId));

    // Assert: Verify strict order of cascade soft deletions
    InOrder inOrder = inOrder(deleteShopMapper);
    inOrder.verify(deleteShopMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    inOrder.verify(deleteShopMapper, times(1)).countActiveStaffByShopId(validRequest.getShopId());
    inOrder
        .verify(deleteShopMapper, times(1))
        .countActiveInvoicesByShopId(validRequest.getShopId());
    inOrder
        .verify(deleteShopMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopMapper, times(1))
        .softDeleteDrinksByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopMapper, times(1))
        .softDeleteCategoriesByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopMapper, times(1))
        .softDeleteTablesByShopId(validRequest.getShopId(), testUserId);
    inOrder.verify(deleteShopMapper, times(1)).softDeleteShop(validRequest.getShopId(), testUserId);

    verify(deleteShopMapper, never()).softDeleteProfileShopsByShopId(any(), any());
  }

  @Test
  void process_Success_WhenUserIdIsNull_TC002() {
    // Arrange: Null safety check when currentUserId is passed as null
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(0);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopService.process(validRequest, null));

    // Assert
    verify(deleteShopMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), null);
    verify(deleteShopMapper, times(1)).softDeleteDrinksByShopId(validRequest.getShopId(), null);
    verify(deleteShopMapper, times(1)).softDeleteCategoriesByShopId(validRequest.getShopId(), null);
    verify(deleteShopMapper, times(1)).softDeleteTablesByShopId(validRequest.getShopId(), null);
    verify(deleteShopMapper, times(1)).softDeleteShop(validRequest.getShopId(), null);
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC003() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class, () -> deleteShopService.process(validRequest, testUserId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(deleteShopMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopMapper, never()).countActiveStaffByShopId(any());
    verify(deleteShopMapper, never()).countActiveInvoicesByShopId(any());
    verify(deleteShopMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteDrinksByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteCategoriesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteTablesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteShop(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenActiveStaffExist_TC004() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(2);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(0);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteShopService.process(validRequest, testUserId));

    assertEquals("Cannot delete shop with active staff", exception.getMessage());

    verify(deleteShopMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveStaffByShopId(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveInvoicesByShopId(validRequest.getShopId());
    verify(deleteShopMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteDrinksByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteCategoriesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteTablesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteShop(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPendingInvoicesExist_TC005() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(0);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(3);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteShopService.process(validRequest, testUserId));

    assertEquals("Cannot delete shop with pending invoices", exception.getMessage());

    verify(deleteShopMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveStaffByShopId(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveInvoicesByShopId(validRequest.getShopId());
    verify(deleteShopMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteDrinksByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteCategoriesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteTablesByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteShop(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenBothActiveStaffAndPendingInvoicesExist_TC006() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(2);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(3);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteShopService.process(validRequest, testUserId));

    assertEquals(
        "Cannot delete shop with active staff and pending invoices", exception.getMessage());

    verify(deleteShopMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveStaffByShopId(validRequest.getShopId());
    verify(deleteShopMapper, times(1)).countActiveInvoicesByShopId(validRequest.getShopId());
    verify(deleteShopMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopMapper, never()).softDeleteShop(any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC007() {
    // Arrange
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopMapper.countActiveStaffByShopId(validRequest.getShopId())).thenReturn(0);
    when(deleteShopMapper.countActiveInvoicesByShopId(validRequest.getShopId())).thenReturn(0);

    doThrow(new DataAccessException("Database deletion error") {})
        .when(deleteShopMapper)
        .softDeleteDrinkDetailsByShopId(eq(validRequest.getShopId()), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> deleteShopService.process(validRequest, testUserId));

    assertEquals("Database deletion error", exception.getMessage());
    verify(deleteShopMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), testUserId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenShopIdIsValid_TC008() {
    Set<ConstraintViolation<DeleteShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationFails_WhenShopIdIsNull_TC009() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<DeleteShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenExistenceCheckReturnsNull_TC010() {
    when(deleteShopMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(null);

    assertThrows(
        DataNotFoundException.class, () -> deleteShopService.process(validRequest, testUserId));

    verify(deleteShopMapper, never()).countActiveStaffByShopId(any(UUID.class));
    verify(deleteShopMapper, never()).softDeleteShop(any(UUID.class), any());
  }
}
