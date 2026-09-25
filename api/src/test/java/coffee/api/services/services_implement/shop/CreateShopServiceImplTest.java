package coffee.api.services.services_implement.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop.CreateShopRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateShopMapper;
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
public class CreateShopServiceImplTest {

  @Mock private CreateShopMapper createShopMapper;

  @InjectMocks private CreateShopServiceImpl createShopService;

  private Validator validator;
  private CreateShopRequest validRequest;
  private UUID currentUserId;
  private String currentRoleName;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    currentRoleName = "OWNER";

    validRequest = new CreateShopRequest();
    validRequest.setShopName("Coffee Shop - Chi nhánh 3");
    validRequest.setAddress("789 Đường 30/4, Quận Ninh Kiều, Cần Thơ");
    validRequest.setPhoneNumber("02923999111");
  }

  // =========================================================================
  // BUSINESS LOGIC - SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(createShopMapper.checkShopExistedByName(validRequest.getShopName())).thenReturn(false);
    when(createShopMapper.checkShopExistedByAddress(validRequest.getAddress())).thenReturn(false);

    // Act
    assertDoesNotThrow(
        () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    // Assert
    verify(createShopMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopMapper, times(1))
        .createShop(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_Success_TrimsWhitespaceBeforePersisting_TC002() {
    // Arrange
    validRequest.setShopName("  Coffee Shop Trimming Test  ");
    validRequest.setAddress("  123 Đường 3/2, Cần Thơ  ");
    validRequest.setPhoneNumber("  0901234567  ");

    when(createShopMapper.checkShopExistedByName("Coffee Shop Trimming Test")).thenReturn(false);
    when(createShopMapper.checkShopExistedByAddress("123 Đường 3/2, Cần Thơ")).thenReturn(false);

    // Act
    assertDoesNotThrow(
        () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    // Assert
    verify(createShopMapper, times(1)).checkShopExistedByName("Coffee Shop Trimming Test");
    verify(createShopMapper, times(1)).checkShopExistedByAddress("123 Đường 3/2, Cần Thơ");
    verify(createShopMapper, times(1))
        .createShop(
            any(UUID.class),
            eq("Coffee Shop Trimming Test"),
            eq("123 Đường 3/2, Cần Thơ"),
            eq("0901234567"),
            eq(false));
  }

  // =========================================================================
  // BUSINESS LOGIC - ABNORMAL CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenShopNameExists_TC003() {
    // Arrange
    when(createShopMapper.checkShopExistedByName(validRequest.getShopName())).thenReturn(true);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    assertEquals("Shop name already exists", exception.getMessage());
    verify(createShopMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopMapper, never()).checkShopExistedByAddress(anyString());
    verify(createShopMapper, never()).createShop(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenAddressExists_TC004() {
    // Arrange
    when(createShopMapper.checkShopExistedByName(validRequest.getShopName())).thenReturn(false);
    when(createShopMapper.checkShopExistedByAddress(validRequest.getAddress())).thenReturn(true);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    assertEquals("Shop address already exists", exception.getMessage());
    verify(createShopMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopMapper, never()).createShop(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC005() {
    // Arrange
    when(createShopMapper.checkShopExistedByName(validRequest.getShopName())).thenReturn(false);
    when(createShopMapper.checkShopExistedByAddress(validRequest.getAddress())).thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createShopMapper)
        .createShop(any(), any(), any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    assertEquals("Database insertion error", exception.getMessage());
    verify(createShopMapper, times(1)).createShop(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenUserIdIsNull_TC006() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createShopService.process(validRequest, null, currentRoleName));

    assertEquals("Only authenticated owners can create shops", exception.getMessage());
    verifyNoInteractions(createShopMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenRoleIsNull_TC007() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createShopService.process(validRequest, currentUserId, null));

    assertEquals("Only authenticated owners can create shops", exception.getMessage());
    verifyNoInteractions(createShopMapper);
  }

  @Test
  void process_PropagatesDataAccessException_WhenNameCheckFails_TC008() {
    doThrow(new DataAccessException("Database name check error") {})
        .when(createShopMapper)
        .checkShopExistedByName(validRequest.getShopName());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    assertEquals("Database name check error", exception.getMessage());
    verify(createShopMapper, never()).checkShopExistedByAddress(anyString());
    verify(createShopMapper, never()).createShop(any(), any(), any(), any(), any());
  }

  @Test
  void process_PropagatesDataAccessException_WhenAddressCheckFails_TC009() {
    when(createShopMapper.checkShopExistedByName(validRequest.getShopName())).thenReturn(false);
    doThrow(new DataAccessException("Database address check error") {})
        .when(createShopMapper)
        .checkShopExistedByAddress(validRequest.getAddress());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    assertEquals("Database address check error", exception.getMessage());
    verify(createShopMapper, never()).createShop(any(), any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC010() {
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs10Digits_TC011() {
    validRequest.setPhoneNumber("0912345678");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs11Digits_TC012() {
    validRequest.setPhoneNumber("02923999111");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - PHONE NUMBER CASES
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPhoneNumberIsNull_TC013() {
    validRequest.setPhoneNumber(null);
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsEmptyString_TC014() {
    validRequest.setPhoneNumber("");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsWhitespace_TC015() {
    validRequest.setPhoneNumber("     ");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberContainsLettersOrSpecialChars_TC016() {
    validRequest.setPhoneNumber("091234abcd");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberDoesNotStartWithZero_TC017() {
    validRequest.setPhoneNumber("1912345678");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_NUMBER_INVALID, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsUnder10Digits_TC018() {
    validRequest.setPhoneNumber("091234567");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberExceeds11Digits_TC019() {
    validRequest.setPhoneNumber("091234567890");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - SHOP NAME CASES
  // =========================================================================

  @Test
  void process_ValidationFails_WhenShopNameIsNull_TC020() {
    validRequest.setShopName(null);
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameIsBlank_TC021() {
    validRequest.setShopName("   ");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameExceeds100Characters_TC022() {
    validRequest.setShopName("A".repeat(101));
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenShopNameHas100Characters_TC023() {
    validRequest.setShopName("A".repeat(100));

    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenShopNameContainsSpecialCharacters_TC024() {
    validRequest.setShopName("Hoa Yên Coffee @ 2026!");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ADDRESS CASES
  // =========================================================================

  @Test
  void process_ValidationFails_WhenAddressIsNull_TC025() {
    validRequest.setAddress(null);
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressIsBlank_TC026() {
    validRequest.setAddress(" ");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressExceeds255Characters_TC027() {
    validRequest.setAddress("A".repeat(256));
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenAddressHas255Characters_TC028() {
    validRequest.setAddress("A".repeat(255));

    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenAddressContainsInvalidSpecialCharacters_TC029() {
    validRequest.setAddress("123 Đường 30/4 $ Phường Hưng Lợi * Ninh Kiều");
    Set<ConstraintViolation<CreateShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_Success_WhenDuplicateChecksReturnNull_TC030() {
    when(createShopMapper.checkShopExistedByName(anyString())).thenReturn(null);
    when(createShopMapper.checkShopExistedByAddress(anyString())).thenReturn(null);

    assertDoesNotThrow(
        () -> createShopService.process(validRequest, currentUserId, currentRoleName));

    verify(createShopMapper)
        .createShop(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenUserIsNotOwner_TC031() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createShopService.process(validRequest, currentUserId, "MANAGER"));

    assertEquals("Only authenticated owners can create shops", exception.getMessage());
    verifyNoInteractions(createShopMapper);
  }
}
