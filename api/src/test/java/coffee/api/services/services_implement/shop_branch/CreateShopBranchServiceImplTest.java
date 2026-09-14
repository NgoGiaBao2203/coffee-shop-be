package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.CreateShopBranchRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateShopBranchMapper;
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
public class CreateShopBranchServiceImplTest {

  @Mock private CreateShopBranchMapper createShopBranchMapper;

  @InjectMocks private CreateShopBranchServiceImpl createShopBranchService;

  private Validator validator;
  private CreateShopBranchRequest validRequest;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    validRequest = new CreateShopBranchRequest();
    validRequest.setShopName("Coffee Shop - Chi nhánh 3");
    validRequest.setAddress("789 Đường 30/4, Quận Ninh Kiều, Cần Thơ");
    validRequest.setPhoneNumber("02923999111");
  }

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_ThrowsUserExistException_WhenShopNameExists_TC003() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Shop name is existed", exception.getMessage());
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, never()).checkShopExistedByAddress(anyString());
    verify(createShopBranchMapper, never()).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenAddressExists_TC004() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Address is existed", exception.getMessage());
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, never()).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC006() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createShopBranchMapper)
        .createShopBranch(any(), any(), any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Database insertion error", exception.getMessage());
    verify(createShopBranchMapper, times(1)).createShopBranch(any(), any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC009() {
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsNull_TC010() {
    validRequest.setPhoneNumber(null);
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsEmptyString_TC011() {
    validRequest.setPhoneNumber("");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsWhitespace_TC012() {
    validRequest.setPhoneNumber("     ");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs10Digits_TC013() {
    validRequest.setPhoneNumber("0912345678");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs11Digits_TC014() {
    validRequest.setPhoneNumber("02923999111");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: SHOP NAME
  // =========================================================================

  @Test
  void process_ValidationFails_WhenShopNameIsNull_TC015() {
    validRequest.setShopName(null);
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameIsBlank_TC016() {
    validRequest.setShopName("   ");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameExceeds100Characters_TC017() {
    validRequest.setShopName("A".repeat(101));
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameContainsSpecialCharacters_TC018() {
    validRequest.setShopName("Hoa Yên Coffee @ 2026!");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: ADDRESS
  // =========================================================================

  @Test
  void process_ValidationFails_WhenAddressIsNull_TC019() {
    validRequest.setAddress(null);
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressIsBlank_TC020() {
    validRequest.setAddress(" ");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressExceeds255Characters_TC021() {
    validRequest.setAddress("A".repeat(256));
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressContainsInvalidSpecialCharacters_TC022() {
    validRequest.setAddress("123 Đường 30/4 $ Phường Hưng Lợi * Ninh Kiều");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: PHONE NUMBER
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPhoneNumberContainsLettersOrSpecialChars_TC023() {
    validRequest.setPhoneNumber("091234abcd");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberDoesNotStartWithZero_TC024() {
    validRequest.setPhoneNumber("1912345678");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_NUMBER_INVALID, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsUnder10Digits_TC025() {
    validRequest.setPhoneNumber("091234567");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberExceeds11Digits_TC026() {
    validRequest.setPhoneNumber("091234567890");
    Set<ConstraintViolation<CreateShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }
}
