package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
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
class CreateDrinkDetailServiceImplTest {

  @Mock private CreateDrinkDetailMapper createDrinkDetailMapper;
  @Mock private GetDrinkDetailMapper getDrinkDetailMapper;
  @Mock private CommonMapper commonMapper;

  // Mock validator used for testing the service flow.
  @Mock private DrinkDetailValidator drinkDetailValidator;

  @InjectMocks private CreateDrinkDetailServiceImpl service;

  // Jakarta Bean Validation used for testing request annotations.
  private Validator requestValidator;

  private UUID drinkId;
  private UUID drinkDetailId;
  private UUID userId;
  private UUID shopId;

  private String ownerRole;
  private String managerRole;
  private String staffRole;

  private CreateDrinkDetailRequest validRequest;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      requestValidator = factory.getValidator();
    }

    drinkId = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");

    drinkDetailId = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");

    userId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();
    staffRole = Roles.STAFF.getValue();

    validRequest = new CreateDrinkDetailRequest();
    validRequest.setDrinkId(drinkId);
    validRequest.setSize("M");
    validRequest.setPrice(new BigDecimal("30000.00"));
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenManagerCreatesDrinkDetail_TC001() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice())))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, managerRole, shopId));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, managerRole, shopId);

    verify(drinkDetailValidator).validateDrinkExists(drinkId, managerRole, shopId);

    verify(drinkDetailValidator).validatePrice(validRequest.getPrice());

    verify(drinkDetailValidator).validateAndFormatSize("M");

    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "M", null);

    verify(createDrinkDetailMapper)
        .insertDrinkDetail(any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice()));
  }

  @Test
  void process_Success_WhenOwnerCreatesDrinkDetailWithoutAssignedShop_TC002() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice())))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));

    // Assert
    verify(drinkDetailValidator).verifyShopAssignment(userId, ownerRole, null);

    verify(drinkDetailValidator).validateDrinkExists(drinkId, ownerRole, null);
  }

  @Test
  void process_Success_WhenSizeIsNormalized_TC003() {
    // Arrange
    validRequest.setSize(" xl ");

    when(drinkDetailValidator.validateAndFormatSize(" xl ")).thenReturn("XL");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("XL"), eq(validRequest.getPrice())))
        .thenReturn(1);

    // Act
    assertDoesNotThrow(() -> service.process(validRequest, userId, ownerRole, null));

    // Assert
    verify(drinkDetailValidator).validateSizeAvailable(drinkId, "XL", null);

    verify(createDrinkDetailMapper)
        .insertDrinkDetail(any(UUID.class), eq(drinkId), eq("XL"), eq(validRequest.getPrice()));
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

    assertEquals("Drink ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkIdIsNull_TC005() {
    // Arrange
    validRequest.setDrinkId(null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(createDrinkDetailMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - VALIDATOR FAILURE CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenShopAssignmentIsInvalid_TC006() {
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

    verify(drinkDetailValidator, never()).validateDrinkExists(any(), any(), any());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC007() {
    // Arrange
    doThrow(new DataNotFoundException("Drink not found", drinkId))
        .when(drinkDetailValidator)
        .validateDrinkExists(drinkId, ownerRole, null);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink not found", exception.getMessage());

    assertEquals(drinkId, exception.getId());

    verify(drinkDetailValidator, never()).validatePrice(any());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPriceIsInvalid_TC008() {
    // Arrange
    validRequest.setPrice(BigDecimal.ZERO);

    doThrow(new InvalidRequestException("Drink detail price must be greater than zero"))
        .when(drinkDetailValidator)
        .validatePrice(BigDecimal.ZERO);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail price must be greater than zero", exception.getMessage());

    verify(drinkDetailValidator, never()).validateAndFormatSize(any());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenSizeIsInvalid_TC009() {
    // Arrange
    validRequest.setSize("XXXL");

    when(drinkDetailValidator.validateAndFormatSize("XXXL"))
        .thenThrow(new InvalidRequestException("Drink detail size is invalid"));

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail size is invalid", exception.getMessage());

    verify(drinkDetailValidator, never()).validateSizeAvailable(any(), any(), any());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenSizeAlreadyExists_TC010() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    doThrow(new InvalidRequestException("Drink detail size already exists"))
        .when(drinkDetailValidator)
        .validateSizeAvailable(drinkId, "M", null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Drink detail size already exists", exception.getMessage());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - INSERT RESULT CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenInsertAffectsZeroRows_TC011() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice())))
        .thenReturn(0);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Unable to create drink detail", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInsertAffectsMoreThanOneRow_TC012() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice())))
        .thenReturn(2);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Unable to create drink detail", exception.getMessage());
  }

  @Test
  void process_PropagatesDataAccessException_WhenInsertFails_TC013() {
    // Arrange
    when(drinkDetailValidator.validateAndFormatSize("M")).thenReturn("M");

    when(createDrinkDetailMapper.insertDrinkDetail(
            any(UUID.class), eq(drinkId), eq("M"), eq(validRequest.getPrice())))
        .thenThrow(new DataAccessException("Insert drink detail failed") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> service.process(validRequest, userId, ownerRole, null));

    assertEquals("Insert drink detail failed", exception.getMessage());
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - SHOP ASSIGNMENT CASES
  // =========================================================================

  @Test
  void validator_Success_WhenRoleIsOwner_TC014() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act
    assertDoesNotThrow(() -> realValidator.verifyShopAssignment(userId, ownerRole, null));

    // Assert
    verifyNoInteractions(commonMapper);
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenManagerHasNoShop_TC015() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> realValidator.verifyShopAssignment(userId, managerRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());
  }

  @Test
  void validator_Success_WhenManagerHasActiveShopMembership_TC016() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);

    // Act
    assertDoesNotThrow(() -> realValidator.verifyShopAssignment(userId, managerRole, shopId));

    // Assert
    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenManagerMembershipIsInactive_TC017() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> realValidator.verifyShopAssignment(userId, managerRole, shopId));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
  }

  @Test
  void validator_Success_WhenStaffHasShopId_TC018() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act
    assertDoesNotThrow(() -> realValidator.verifyShopAssignment(userId, staffRole, shopId));

    // Assert
    verifyNoInteractions(commonMapper);
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - DRINK CASES
  // =========================================================================

  @Test
  void validator_ThrowsInvalidRequestException_WhenDrinkIdIsNull_TC019() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> realValidator.validateDrinkExists(null, ownerRole, null));

    assertEquals("Drink ID is required", exception.getMessage());

    verifyNoInteractions(createDrinkDetailMapper);
  }

  @Test
  void validator_ThrowsDataNotFoundException_WhenDrinkIsNotAccessible_TC020() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(createDrinkDetailMapper.checkDrinkAccessible(drinkId, ownerRole, null)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> realValidator.validateDrinkExists(drinkId, ownerRole, null));

    assertEquals("Drink not found", exception.getMessage());

    assertEquals(drinkId, exception.getId());
  }

  @Test
  void validator_Success_WhenDrinkIsAccessible_TC021() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(createDrinkDetailMapper.checkDrinkAccessible(drinkId, ownerRole, null)).thenReturn(true);

    // Act & Assert
    assertDoesNotThrow(() -> realValidator.validateDrinkExists(drinkId, ownerRole, null));
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - DRINK DETAIL CASES
  // =========================================================================

  @Test
  void validator_ReturnsDrinkDetail_WhenDrinkDetailExists_TC022() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    DrinkDetailResult expected = new DrinkDetailResult();

    expected.setDrinkDetailId(drinkDetailId);
    expected.setDrinkId(drinkId);
    expected.setSize("M");
    expected.setPrice(new BigDecimal("30000.00"));

    when(getDrinkDetailMapper.getDrinkDetailVariantById(drinkDetailId, shopId, managerRole))
        .thenReturn(expected);

    // Act
    DrinkDetailResult actual =
        realValidator.validateDrinkDetailExists(drinkDetailId, managerRole, shopId);

    // Assert
    assertSame(expected, actual);
  }

  @Test
  void validator_ThrowsDataNotFoundException_WhenDrinkDetailDoesNotExist_TC023() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(getDrinkDetailMapper.getDrinkDetailVariantById(drinkDetailId, shopId, managerRole))
        .thenReturn(null);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> realValidator.validateDrinkDetailExists(drinkDetailId, managerRole, shopId));

    assertEquals("Drink detail not found", exception.getMessage());

    assertEquals(drinkDetailId, exception.getId());
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - SIZE CASES
  // =========================================================================

  @Test
  void validator_NormalizesSupportedSize_TC024() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    assertEquals("L", realValidator.validateAndFormatSize(" l "));
  }

  @Test
  void validator_Success_ForAllSupportedSizes_TC025() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    assertEquals("S", realValidator.validateAndFormatSize("S"));
    assertEquals("M", realValidator.validateAndFormatSize("M"));
    assertEquals("L", realValidator.validateAndFormatSize("L"));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenSizeIsNull_TC026() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    assertThrows(InvalidRequestException.class, () -> realValidator.validateAndFormatSize(null));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenSizeIsInvalid_TC027() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    for (String size : new String[] {"XL", "XXL", "XXXL"}) {
      assertThrows(InvalidRequestException.class, () -> realValidator.validateAndFormatSize(size));
    }
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - PRICE CASES
  // =========================================================================

  @Test
  void validator_Success_WhenPriceIsPositive_TC028() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    // Act & Assert
    assertDoesNotThrow(() -> realValidator.validatePrice(new BigDecimal("0.01")));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenPriceIsNull_TC029() {
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    assertThrows(InvalidRequestException.class, () -> realValidator.validatePrice(null));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenPriceIsZero_TC030() {
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    assertThrows(InvalidRequestException.class, () -> realValidator.validatePrice(BigDecimal.ZERO));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenPriceIsNegative_TC031() {
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    assertThrows(
        InvalidRequestException.class, () -> realValidator.validatePrice(new BigDecimal("-1.00")));
  }

  // =========================================================================
  // DRINK DETAIL VALIDATOR - DUPLICATE SIZE CASES
  // =========================================================================

  @Test
  void validator_Success_WhenSizeIsAvailable_TC032() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(commonMapper.checkDrinkDetailSizeExisted(drinkId, "M", null)).thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(() -> realValidator.validateSizeAvailable(drinkId, "M", null));
  }

  @Test
  void validator_ThrowsInvalidRequestException_WhenSizeAlreadyExists_TC033() {
    // Arrange
    DrinkDetailValidator realValidator = createRealDrinkDetailValidator();

    when(commonMapper.checkDrinkDetailSizeExisted(drinkId, "M", null)).thenReturn(true);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> realValidator.validateSizeAvailable(drinkId, "M", null));

    assertEquals("Drink detail size already exists", exception.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void request_ValidationSuccess_WhenAllFieldsAreValid_TC034() {
    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationSuccess_ForAllSupportedSizes_TC035() {
    for (String size : new String[] {"S", "M", "L"}) {

      validRequest.setSize(size);

      Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
          requestValidator.validate(validRequest);

      assertTrue(violations.isEmpty());
    }
  }

  @Test
  void request_ValidationFails_WhenDrinkIdIsNull_TC036() {
    validRequest.setDrinkId(null);

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(
        hasCreateRequestViolation(violations, "drinkId", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsNull_TC037() {
    validRequest.setSize(null);

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "size", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsBlank_TC038() {
    validRequest.setSize("   ");

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "size", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenSizeIsLowercase_TC039() {
    validRequest.setSize("m");

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(
        hasCreateRequestViolation(violations, "size", ValidationMessage.Msg.SPECIAL_CHARACTERS));
  }

  @Test
  void request_ValidationFails_WhenSizeIsUnsupported_TC040() {
    for (String size : new String[] {"XL", "XXL", "XXXL"}) {
      validRequest.setSize(size);

      Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
          requestValidator.validate(validRequest);

      assertTrue(
          hasCreateRequestViolation(violations, "size", ValidationMessage.Msg.SPECIAL_CHARACTERS));
    }
  }

  @Test
  void request_ValidationFails_WhenPriceIsNull_TC041() {
    validRequest.setPrice(null);

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(
        hasCreateRequestViolation(violations, "price", ValidationMessage.Msg.FIELD_REQUIRED));
  }

  @Test
  void request_ValidationFails_WhenPriceIsZero_TC042() {
    validRequest.setPrice(BigDecimal.ZERO);

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "price", ValidationMessage.Msg.PRICE_MIN));
  }

  @Test
  void request_ValidationFails_WhenPriceIsNegative_TC043() {
    validRequest.setPrice(new BigDecimal("-1.00"));

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "price", ValidationMessage.Msg.PRICE_MIN));
  }

  @Test
  void request_ValidationFails_WhenPriceHasMoreThanTwoDecimalPlaces_TC044() {
    validRequest.setPrice(new BigDecimal("30000.123"));

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "price", ValidationMessage.Msg.SIZE_MAX));
  }

  @Test
  void request_ValidationFails_WhenPriceExceedsSixteenIntegerDigits_TC045() {
    validRequest.setPrice(new BigDecimal("12345678901234567"));

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(hasCreateRequestViolation(violations, "price", ValidationMessage.Msg.SIZE_MAX));
  }

  @Test
  void request_ValidationSuccess_WhenPriceUsesMaximumAllowedDigits_TC046() {
    validRequest.setPrice(new BigDecimal("9999999999999999.99"));

    Set<ConstraintViolation<CreateDrinkDetailRequest>> violations =
        requestValidator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  private DrinkDetailValidator createRealDrinkDetailValidator() {
    return new DrinkDetailValidator(createDrinkDetailMapper, getDrinkDetailMapper, commonMapper);
  }

  private boolean hasCreateRequestViolation(
      Set<ConstraintViolation<CreateDrinkDetailRequest>> violations, String field, String message) {

    return violations.stream()
        .anyMatch(
            violation ->
                field.equals(violation.getPropertyPath().toString())
                    && message.equals(violation.getMessage()));
  }
}
