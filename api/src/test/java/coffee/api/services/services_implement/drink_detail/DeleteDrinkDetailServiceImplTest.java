package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class DeleteDrinkDetailServiceImplTest {

  @Mock private DeleteDrinkDetailMapper deleteDrinkDetailMapper;

  @Mock private DrinkDetailValidator drinkDetailValidator;

  @InjectMocks private DeleteDrinkDetailServiceImpl service;

  private Validator requestValidator;

  private UUID drinkDetailId;
  private UUID drinkId;
  private UUID userId;
  private UUID shopId;

  private String ownerRole;
  private String managerRole;

  private DeleteDrinkDetailRequest validRequest;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      requestValidator = factory.getValidator();
    }

    drinkDetailId = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");

    drinkId = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");

    userId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();

    validRequest = new DeleteDrinkDetailRequest();
    validRequest.setDrinkDetailId(drinkDetailId);
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenManagerDeletesDrinkDetail_TC001() {
    // Arrange
    DrinkDetailResult existingDetail = createExistingDrinkDetail();

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, managerRole, shopId))
        .thenReturn(existingDetail);

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(2);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, managerRole, shopId))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, managerRole, shopId));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, managerRole, shopId);

    verify(drinkDetailValidator).validateDrinkDetailExists(drinkDetailId, managerRole, shopId);

    verify(deleteDrinkDetailMapper).countActiveDrinkDetails(drinkId);

    verify(deleteDrinkDetailMapper)
        .softDeleteDrinkDetail(drinkDetailId, userId, managerRole, shopId);
  }

  @Test
  void process_Success_WhenOwnerDeletesDrinkDetailWithoutAssignedShop_TC002() {
    // Arrange
    DrinkDetailResult existingDetail = createExistingDrinkDetail();

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(existingDetail);

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(3);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, ownerRole, null);

    verify(deleteDrinkDetailMapper).softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null);
  }

  @Test
  void process_Success_WhenExactlyTwoActiveSizesExist_TC003() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(2);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null))
        .thenReturn(1);

    // Act & Assert
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));
  }

  // =========================================================================
  // SERVICE PROCESS - REQUEST VALIDATION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC004() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> service.process(null, userId, ownerRole, null));

    assertEquals("Drink detail ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(deleteDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkDetailIdIsNull_TC005() {
    // Arrange
    validRequest.setDrinkDetailId(null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(deleteDrinkDetailMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - VALIDATOR FAILURE CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenShopAssignmentFails_TC006() {
    // Arrange
    doThrow(new InvalidRequestException("You do not have permission to access this shop"))
        .when(drinkDetailValidator)
        .verifyShopAssignment(userId, managerRole, shopId);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, managerRole, shopId));

    assertEquals("You do not have permission to access this shop", exception.getMessage());

    verify(drinkDetailValidator, never()).validateDrinkDetailExists(any(), any(), any());

    verifyNoInteractions(deleteDrinkDetailMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDetailDoesNotExist_TC007() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenThrow(new DataNotFoundException("Drink detail not found", drinkDetailId));

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail not found", exception.getMessage());

    assertEquals(drinkDetailId, exception.getId());

    verify(deleteDrinkDetailMapper, never()).countActiveDrinkDetails(any());

    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ACTIVE SIZE BUSINESS RULE CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenOnlyOneActiveSizeExists_TC008() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(1);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("A drink must have at least one active size", exception.getMessage());

    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenNoActiveSizesExist_TC009() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(0);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("A drink must have at least one active size", exception.getMessage());

    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenActiveSizeCountIsNegative_TC010() {
    // Defensive case for unexpected database results.

    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(-1);

    // Act & Assert
    assertThrows(
        InvalidRequestException.class,
        () -> service.process(validRequest, userId, ownerRole, null));

    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - DELETE RESULT CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenDeleteAffectsZeroRows_TC011() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(2);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null))
        .thenReturn(0);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail has already been deleted or modified", exception.getMessage());

    assertEquals(drinkDetailId, exception.getId());
  }

  @Test
  void process_Success_WhenDeleteAffectsMoreThanOneRow_TC012() {
    // The current implementation rejects only affectedRows == 0.

    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(2);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null))
        .thenReturn(2);

    // Act & Assert
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));
  }

  // =========================================================================
  // SERVICE PROCESS - DATABASE EXCEPTION CASES
  // =========================================================================

  @Test
  void process_PropagatesDataAccessException_WhenCountingActiveSizesFails_TC013() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId))
        .thenThrow(new DataAccessException("Count active drink details failed") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Count active drink details failed", exception.getMessage());

    verify(deleteDrinkDetailMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_PropagatesDataAccessException_WhenSoftDeleteFails_TC014() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId)).thenReturn(2);

    when(deleteDrinkDetailMapper.softDeleteDrinkDetail(drinkDetailId, userId, ownerRole, null))
        .thenThrow(new DataAccessException("Soft delete drink detail failed") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Soft delete drink detail failed", exception.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void request_ValidationSuccess_WhenDrinkDetailIdIsValid_TC015() {
    // Act
    Set<ConstraintViolation<DeleteDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    // Assert
    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationFails_WhenDrinkDetailIdIsNull_TC016() {
    // Arrange
    validRequest.setDrinkDetailId(null);

    // Act
    Set<ConstraintViolation<DeleteDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    // Assert
    assertTrue(
        violations.stream()
            .anyMatch(
                violation ->
                    "drinkDetailId".equals(violation.getPropertyPath().toString())
                        && ValidationMessage.Msg.FIELD_REQUIRED.equals(violation.getMessage())));
  }

  private DrinkDetailResult createExistingDrinkDetail() {
    DrinkDetailResult result = new DrinkDetailResult();

    result.setDrinkDetailId(drinkDetailId);
    result.setDrinkId(drinkId);
    result.setSize("M");

    return result;
  }
}
