package coffee.api.services.services_implement.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.profile.ChangePasswordRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdatePasswordMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceImplTest {

  private static final String CURRENT_PASSWORD = "OldPass@123";
  private static final String NEW_PASSWORD = "NewPass@456";
  private static final String CURRENT_HASH = "$2a$10$currentHash";
  private static final String NEW_HASH = "$2a$10$newHash";

  @Mock private UpdatePasswordMapper updatePasswordMapper;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private ChangePasswordServiceImpl service;

  private Validator validator;
  private ChangePasswordRequest request;
  private UUID userId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    userId = UUID.fromString("00000000-0000-0000-0000-000000000501");

    request = new ChangePasswordRequest();
    request.setCurrentPassword(CURRENT_PASSWORD);
    request.setNewPassword(NEW_PASSWORD);
    request.setConfirmPassword(NEW_PASSWORD);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubCurrentPasswordValid() {
    when(updatePasswordMapper.findActivePasswordByProfileId(userId)).thenReturn(CURRENT_HASH);
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);
    when(passwordEncoder.matches(NEW_PASSWORD, CURRENT_HASH)).thenReturn(false);
  }

  private void stubEncodeNewPassword() {
    when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_HASH);
  }

  private void process() {
    service.process(request, userId);
  }

  private void verifyNoUpdate() {
    verify(passwordEncoder, never()).encode(any());
    verify(updatePasswordMapper, never()).updatePassword(any(), any());
  }

  private void assertRequestValid() {
    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertEquals(0, violations.size(), violations.toString());
  }

  private void assertHasViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    property.equals(v.getPropertyPath().toString())
                        && expectedMessage.equals(v.getMessage())),
        violations.toString());
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenCurrentPasswordIsCorrect_TC001() {
    stubCurrentPasswordValid();
    stubEncodeNewPassword();
    when(updatePasswordMapper.updatePassword(userId, NEW_HASH)).thenReturn(1);

    assertDoesNotThrow(this::process);

    // The stored value is the encoded hash, never the raw password
    InOrder inOrder = inOrder(updatePasswordMapper, passwordEncoder);
    inOrder.verify(updatePasswordMapper).findActivePasswordByProfileId(userId);
    inOrder.verify(passwordEncoder).matches(CURRENT_PASSWORD, CURRENT_HASH);
    inOrder.verify(passwordEncoder).encode(NEW_PASSWORD);
    inOrder.verify(updatePasswordMapper).updatePassword(userId, NEW_HASH);
  }

  @Test
  void process_Success_WhenPasswordsHaveSurroundingSpacesTheyAreTrimmed_TC002() {
    request.setCurrentPassword("  " + CURRENT_PASSWORD + " ");
    request.setNewPassword(" " + NEW_PASSWORD + "  ");
    request.setConfirmPassword("\t" + NEW_PASSWORD + " ");
    stubCurrentPasswordValid();
    stubEncodeNewPassword();
    when(updatePasswordMapper.updatePassword(userId, NEW_HASH)).thenReturn(1);

    assertDoesNotThrow(this::process);

    // Only the trimmed values reach the encoder
    verify(passwordEncoder).matches(CURRENT_PASSWORD, CURRENT_HASH);
    verify(passwordEncoder).matches(NEW_PASSWORD, CURRENT_HASH);
    verify(passwordEncoder).encode(NEW_PASSWORD);
    verify(updatePasswordMapper).updatePassword(userId, NEW_HASH);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PASSWORD CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenConfirmPasswordDoesNotMatch_TC003() {
    request.setConfirmPassword("Different@789");

    InvalidRequestException exception = assertThrows(InvalidRequestException.class, this::process);

    assertEquals(ErrorMessage.CONFIRM_PASSWORD_MISMATCH, exception.getMessage());
    verifyNoInteractions(updatePasswordMapper, passwordEncoder);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCurrentPasswordIsIncorrect_TC004() {
    when(updatePasswordMapper.findActivePasswordByProfileId(userId)).thenReturn(CURRENT_HASH);
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(false);

    InvalidRequestException exception = assertThrows(InvalidRequestException.class, this::process);

    assertEquals(ErrorMessage.CURRENT_PASSWORD_INCORRECT, exception.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenNewPasswordEqualsCurrentPassword_TC005() {
    request.setNewPassword(CURRENT_PASSWORD);
    request.setConfirmPassword(CURRENT_PASSWORD);
    when(updatePasswordMapper.findActivePasswordByProfileId(userId)).thenReturn(CURRENT_HASH);
    when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH)).thenReturn(true);

    InvalidRequestException exception = assertThrows(InvalidRequestException.class, this::process);

    assertEquals(ErrorMessage.NEW_PASSWORD_SAME_AS_CURRENT, exception.getMessage());
    verifyNoUpdate();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PROFILE CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenProfileIsDeletedOrMissing_TC006() {
    when(updatePasswordMapper.findActivePasswordByProfileId(userId)).thenReturn(null);

    DataNotFoundException exception = assertThrows(DataNotFoundException.class, this::process);

    assertEquals(ErrorMessage.DATA_NOT_FOUND, exception.getMessage());
    assertEquals(userId, exception.getId());
    verify(passwordEncoder, never()).matches(any(), any());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenProfileWasDeletedMeanwhile_TC007() {
    stubCurrentPasswordValid();
    stubEncodeNewPassword();
    when(updatePasswordMapper.updatePassword(userId, NEW_HASH)).thenReturn(0);

    DataNotFoundException exception = assertThrows(DataNotFoundException.class, this::process);

    assertEquals(userId, exception.getId());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenLoadingPasswordFails_TC008() {
    when(updatePasswordMapper.findActivePasswordByProfileId(userId))
        .thenThrow(new DataAccessException("Load password failed") {});

    var error = assertThrows(DataAccessException.class, this::process);

    assertEquals("Load password failed", error.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsDataAccessException_WhenUpdatingPasswordFails_TC009() {
    stubCurrentPasswordValid();
    stubEncodeNewPassword();
    when(updatePasswordMapper.updatePassword(userId, NEW_HASH))
        .thenThrow(new DataAccessException("Update password failed") {});

    var error = assertThrows(DataAccessException.class, this::process);

    assertEquals("Update password failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC010() {
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenNewPasswordIsAtLengthBoundary_TC011() {
    request.setNewPassword("Abc@1234");
    assertRequestValid();

    request.setNewPassword("Abc@1234" + "a".repeat(64));
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenCurrentPasswordIsBlank_TC012(String value) {
    request.setCurrentPassword(value);
    assertHasViolation("currentPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenCurrentPasswordIsNull_TC013() {
    request.setCurrentPassword(null);
    assertHasViolation("currentPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenNewPasswordIsBlank_TC014(String value) {
    request.setNewPassword(value);
    assertHasViolation("newPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenNewPasswordIsNull_TC015() {
    request.setNewPassword(null);
    assertHasViolation("newPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsPasswordMinLength_WhenNewPasswordIsShorterThan8_TC016() {
    request.setNewPassword("A".repeat(7));
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_MIN_LENGTH);
  }

  @Test
  void requestValidation_ReportsSizeMax_WhenNewPasswordExceeds72Characters_TC017() {
    request.setNewPassword("A".repeat(73));
    assertHasViolation("newPassword", ValidationMessage.SIZE_MAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenConfirmPasswordIsBlank_TC018(String value) {
    request.setConfirmPassword(value);
    assertHasViolation("confirmPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenConfirmPasswordIsNull_TC019() {
    request.setConfirmPassword(null);
    assertHasViolation("confirmPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {" NewPass@456", "NewPass@456 ", "New Pass@456", "NewPass@456\t"})
  void requestValidation_ReportsPasswordWhitespace_WhenNewPasswordContainsWhitespace_TC020(
      String value) {
    request.setNewPassword(value);
    request.setConfirmPassword(NEW_PASSWORD);
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_WHITESPACE);
  }

  @ParameterizedTest
  @ValueSource(strings = {" NewPass@456", "NewPass@456 ", "New Pass@456"})
  void requestValidation_ReportsPasswordWhitespace_WhenConfirmPasswordContainsWhitespace_TC021(
      String value) {
    request.setConfirmPassword(value);
    assertHasViolation("confirmPassword", ValidationMessage.PASSWORD_WHITESPACE);
  }

  @Test
  void requestValidation_AllowsWhitespaceInCurrentPassword_TC022() {
    // The current password is only trimmed by the service, never rejected for whitespace
    request.setCurrentPassword(" " + CURRENT_PASSWORD + " ");
    assertRequestValid();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "newpass@456", // no uppercase
        "NEWPASS@456", // no lowercase
        "NewPass@abc", // no digit
        "NewPass4567", // no special character
        "abcdefgh" // letters only
      })
  void requestValidation_ReportsPasswordWeak_WhenNewPasswordMissesACharacterClass_TC023(
      String value) {
    request.setNewPassword(value);
    request.setConfirmPassword(value);
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_WEAK);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Abc@1234", "Zz9!zzzz", "Pass_w0rd", "Pass-w0rd~", "A1b\\[]{}"})
  void requestValidation_Success_WhenNewPasswordHasEveryCharacterClass_TC024(String value) {
    request.setNewPassword(value);
    request.setConfirmPassword(value);
    assertRequestValid();
  }

  @Test
  void requestValidation_ReportsOnlyRequired_WhenNewPasswordIsEmpty_TC025() {
    // An empty value must not also be reported as a weak password
    request.setNewPassword("");
    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertTrue(
        violations.stream().noneMatch(v -> ValidationMessage.PASSWORD_WEAK.equals(v.getMessage())),
        violations.toString());
    assertHasViolation("newPassword", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"New Pass@456", " New Pass@456 ", "NewPass\t@456"})
  void process_ThrowsInvalidRequestException_WhenTrimmedNewPasswordStillHasWhitespace_TC026(
      String value) {
    request.setNewPassword(value);
    request.setConfirmPassword(value);

    InvalidRequestException exception = assertThrows(InvalidRequestException.class, this::process);

    assertEquals(ErrorMessage.PASSWORD_CONTAINS_WHITESPACE, exception.getMessage());
    verifyNoInteractions(updatePasswordMapper, passwordEncoder);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTrimmedConfirmPasswordStillHasWhitespace_TC027() {
    request.setConfirmPassword("New Pass@456");

    InvalidRequestException exception = assertThrows(InvalidRequestException.class, this::process);

    assertEquals(ErrorMessage.PASSWORD_CONTAINS_WHITESPACE, exception.getMessage());
    verifyNoInteractions(updatePasswordMapper, passwordEncoder);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Mật#Khẩu1", "Abc@1234ế", "Abc@1234😀", "Ａbc@1234"})
  void requestValidation_ReportsPasswordNonAscii_WhenNewPasswordHasNonAsciiCharacter_TC028(
      String value) {
    request.setNewPassword(value);
    request.setConfirmPassword(NEW_PASSWORD);
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_NON_ASCII);
  }

  @Test
  void requestValidation_ReportsPasswordWeak_WhenAccentedLetterIsTheOnlySpecialCharacter_TC029() {
    // An accented letter is not an ASCII special character
    request.setNewPassword("Abc12345ế");
    request.setConfirmPassword(NEW_PASSWORD);
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_WEAK);
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_NON_ASCII);
  }

  @Test
  void requestValidation_ReportsPasswordNonAscii_WhenConfirmPasswordHasNonAsciiCharacter_TC030() {
    request.setConfirmPassword("NewPass@45ế");
    assertHasViolation("confirmPassword", ValidationMessage.PASSWORD_NON_ASCII);
  }

  @Test
  void requestValidation_ReportsOnlyNonAscii_WhenPasswordWouldExceed72Bytes_TC031() {
    // 34 characters but 94 UTF-8 bytes: must stop at validation, never reach BCrypt
    String value = "Aa1@" + "ế".repeat(30);
    request.setNewPassword(value);
    request.setConfirmPassword(value);
    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertTrue(
        violations.stream()
            .allMatch(v -> ValidationMessage.PASSWORD_NON_ASCII.equals(v.getMessage())),
        violations.toString());
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_NON_ASCII);
  }

  @Test
  void requestValidation_ReportsOnlyWhitespace_WhenNewPasswordHasSpace_TC032() {
    // A space is ASCII, so it must not also be reported as a non-ASCII character
    request.setNewPassword("New Pass@456");
    request.setConfirmPassword(NEW_PASSWORD);
    Set<ConstraintViolation<ChangePasswordRequest>> violations = validator.validate(request);
    assertTrue(
        violations.stream()
            .noneMatch(v -> ValidationMessage.PASSWORD_NON_ASCII.equals(v.getMessage())),
        violations.toString());
    assertHasViolation("newPassword", ValidationMessage.PASSWORD_WHITESPACE);
  }
}
