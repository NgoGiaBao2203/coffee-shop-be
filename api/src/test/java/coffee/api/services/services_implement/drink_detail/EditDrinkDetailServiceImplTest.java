package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
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
class EditDrinkDetailServiceImplTest {

  @Mock private UpdateDrinkDetailMapper updateDrinkDetailMapper;

  @Mock private DrinkDetailValidator drinkDetailValidator;

  @InjectMocks private EditDrinkDetailServiceImpl service;

  private Validator requestValidator;

  private UUID drinkDetailId;
  private UUID drinkId;
  private UUID userId;
  private UUID shopId;

  private String ownerRole;
  private String managerRole;

  private EditDrinkDetailRequest validRequest;

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

    validRequest = new EditDrinkDetailRequest();
    validRequest.setDrinkDetailId(drinkDetailId);
    validRequest.setSize("M");
    validRequest.setPrice(new BigDecimal("35000.00"));
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenOwnerUpdatesDrinkDetail_TC001() {
    // Arrange
    DrinkDetailResult existingDetail = createExistingDrinkDetail();

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(existingDetail);

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "M", validRequest.getPrice(), ownerRole, null))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, ownerRole, null);

    verify(drinkDetailValidator).validateDrinkDetailExists(drinkDetailId, ownerRole, null);

    verify(drinkDetailValidator).validatePrice(validRequest.getPrice());

    verify(drinkDetailValidator).validateAndFormatSize("M");

    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "M", drinkDetailId);

    verify(updateDrinkDetailMapper)
        .updateDrinkDetail(drinkDetailId, "M", validRequest.getPrice(), ownerRole, null);
  }

  @Test
  void process_Success_WhenManagerUpdatesDrinkDetail_TC002() {
    // Arrange
    DrinkDetailResult existingDetail = createExistingDrinkDetail();

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, managerRole, shopId))
        .thenReturn(existingDetail);

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "M", validRequest.getPrice(), managerRole, shopId))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, managerRole, shopId));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, managerRole, shopId);

    verify(updateDrinkDetailMapper)
        .updateDrinkDetail(drinkDetailId, "M", validRequest.getPrice(), managerRole, shopId);
  }

  @Test
  void process_Success_WhenSizeIsNormalized_TC003() {
    // Arrange
    validRequest.setSize(" l ");

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize(" l ")).thenReturn("L");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "L", validRequest.getPrice(), ownerRole, null))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));

    // Assert
    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "L", drinkDetailId);

    verify(updateDrinkDetailMapper)
        .updateDrinkDetail(drinkDetailId, "L", validRequest.getPrice(), ownerRole, null);
  }

  // =========================================================================
  // SERVICE PROCESS - REQUEST CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC004() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> service.process(null, userId, ownerRole, null));

    assertEquals("Drink detail ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(updateDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkDetailIdIsNull_TC005() {
    validRequest.setDrinkDetailId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(updateDrinkDetailMapper);
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
    assertThrows(
        InvalidRequestException.class,
        () -> service.process(validRequest, userId, managerRole, shopId));

    verify(drinkDetailValidator, never()).validateDrinkDetailExists(any(), any(), any());

    verifyNoInteractions(updateDrinkDetailMapper);
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

    verify(drinkDetailValidator, never()).validatePrice(any());

    verifyNoInteractions(updateDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPriceIsInvalid_TC008() {
    // Arrange
    validRequest.setPrice(BigDecimal.ZERO);

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    doThrow(new InvalidRequestException("Drink detail price must be greater than zero"))
        .when(drinkDetailValidator)
        .validatePrice(BigDecimal.ZERO);

    // Act & Assert
    assertThrows(
        InvalidRequestException.class,
        () -> service.process(validRequest, userId, ownerRole, null));

    verify(drinkDetailValidator, never()).validateAndFormatSize(any());

    verifyNoInteractions(updateDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenSizeIsInvalid_TC009() {
    // Arrange
    validRequest.setSize("XXXL");

    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize("XXXL"))
        .thenThrow(new InvalidRequestException("Drink detail size is invalid"));

    // Act & Assert
    assertThrows(
        InvalidRequestException.class,
        () -> service.process(validRequest, userId, ownerRole, null));

    verify(drinkDetailValidator, never()).validateSizeAvailable(any(), any(), any());

    verifyNoInteractions(updateDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDuplicateSizeExists_TC010() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    doThrow(new InvalidRequestException("Drink detail size already exists"))
        .when(drinkDetailValidator)
        .validateSizeAvailable(drinkId, "M", drinkDetailId);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail size already exists", exception.getMessage());

    verifyNoInteractions(updateDrinkDetailMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - UPDATE RESULT CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenUpdateAffectsZeroRows_TC011() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "M", validRequest.getPrice(), ownerRole, null))
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
  void process_Success_WhenUpdateAffectsMoreThanOneRow_TC012() {
    // The current implementation rejects only affectedRows == 0.

    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "M", validRequest.getPrice(), ownerRole, null))
        .thenReturn(2);

    // Act & Assert
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));
  }

  @Test
  void process_PropagatesDataAccessException_WhenUpdateFails_TC013() {
    // Arrange
    when(drinkDetailValidator.validateDrinkDetailExists(drinkDetailId, ownerRole, null))
        .thenReturn(createExistingDrinkDetail());

    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(updateDrinkDetailMapper.updateDrinkDetail(
            drinkDetailId, "M", validRequest.getPrice(), ownerRole, null))
        .thenThrow(new DataAccessException("Update drink detail failed") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Update drink detail failed", exception.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void request_ValidationSuccess_WhenAllFieldsAreValid_TC014() {
    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationSuccess_ForAllSupportedSizes_TC015() {
    for (String size : new String[] {"S", "M", "L"}) {

      validRequest.setSize(size);

      Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
          requestValidator.validate(validRequest);

      assertTrue(violations.isEmpty());
    }
  }

  @Test
  void request_ValidationFails_WhenDrinkDetailIdIsNull_TC016() {
    validRequest.setDrinkDetailId(null);

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(
        hasEditRequestViolation(violations, "drinkDetailId", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsNull_TC017() {
    validRequest.setSize(null);

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "size", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsBlank_TC018() {
    validRequest.setSize("   ");

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "size", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsLowercase_TC019() {
    validRequest.setSize("m");

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(
        hasEditRequestViolation(violations, "size", ValidationMessage.Msg.SPECIAL_CHARACTERS));
  }

  @Test
  void request_ValidationFails_WhenSizeIsUnsupported_TC020() {
    for (String size : new String[] {"XL", "XXL", "XXXL"}) {
      validRequest.setSize(size);

      Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
          requestValidator.validate(validRequest);

      assertTrue(
          hasEditRequestViolation(violations, "size", ValidationMessage.Msg.SPECIAL_CHARACTERS));
    }
  }

  @Test
  void request_ValidationFails_WhenPriceIsNull_TC021() {
    validRequest.setPrice(null);

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "price", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenPriceIsZero_TC022() {
    validRequest.setPrice(BigDecimal.ZERO);

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "price", ValidationMessage.Msg.PRICE_MIN));
  }

  @Test
  void request_ValidationFails_WhenPriceIsNegative_TC023() {
    validRequest.setPrice(new BigDecimal("-1.00"));

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "price", ValidationMessage.Msg.PRICE_MIN));
  }

  @Test
  void request_ValidationFails_WhenPriceHasMoreThanTwoDecimalPlaces_TC024() {
    validRequest.setPrice(new BigDecimal("30000.123"));

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "price", ValidationMessage.Msg.SIZE_MAX));
  }

  @Test
  void request_ValidationFails_WhenPriceExceedsSixteenIntegerDigits_TC025() {
    validRequest.setPrice(new BigDecimal("12345678901234567"));

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasEditRequestViolation(violations, "price", ValidationMessage.Msg.SIZE_MAX));
  }

  @Test
  void request_ValidationSuccess_WhenPriceUsesMaximumAllowedDigits_TC026() {
    validRequest.setPrice(new BigDecimal("9999999999999999.99"));

    Set<ConstraintViolation<EditDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  private DrinkDetailResult createExistingDrinkDetail() {
    DrinkDetailResult result = new DrinkDetailResult();

    result.setDrinkDetailId(drinkDetailId);
    result.setDrinkId(drinkId);
    result.setSize("M");
    result.setPrice(new BigDecimal("30000.00"));

    return result;
  }

  private boolean hasEditRequestViolation(
      Set<ConstraintViolation<EditDrinkDetailRequest>> violations, String field, String message) {

    return violations.stream()
        .anyMatch(
            violation ->
                field.equals(violation.getPropertyPath().toString())
                    && message.equals(violation.getMessage()));
  }
}
