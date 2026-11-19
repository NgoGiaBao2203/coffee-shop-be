package coffee.api.services.services_implement.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.staff.CreateStaffRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.EmailExistedException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateStaffMapper;
import coffee.api.services.services_implement.staff.common.StaffValidator;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class CreateStaffServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private CreateStaffMapper createStaffMapper;

  private CreateStaffServiceImpl service;
  private Validator validator;

  private CreateStaffRequest request;
  private UUID shopId;
  private UUID userId;
  private UUID roleId;
  private String ownerRole;
  private String managerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    service =
        new CreateStaffServiceImpl(
            commonMapper, createStaffMapper, new StaffValidator(commonMapper));

    shopId = UUID.fromString("00000000-0000-0000-0000-000000000301");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000302");
    roleId = UUID.fromString("00000000-0000-0000-0000-000000000303");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();

    request = new CreateStaffRequest();
    request.setEmail("newstaff@coffee.com");
    request.setUsername("newstaff");
    request.setFullName("Nguyen Van B");
    request.setPhoneNumber("0901234567");
    request.setRoleId(roleId);
    request.setShopId(shopId);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubShopAccess(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (managerRole.equals(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    }
  }

  private void stubValidAccess(String role) {
    stubShopAccess(role);
    if (ownerRole.equals(role)) {
      when(commonMapper.findRoleNameById(roleId)).thenReturn(Roles.STAFF.getValue());
    }
  }

  private void stubValidCreate(String role) {
    stubValidAccess(role);
    when(commonMapper.checkStaffExistedByUsername("newstaff")).thenReturn(false);
    when(commonMapper.checkStaffEmailExisted("newstaff@coffee.com")).thenReturn(false);
    when(commonMapper.checkStaffPhoneExisted("0901234567", null)).thenReturn(false);
  }

  private UUID shopOf(String role) {
    return ownerRole.equals(role) ? null : shopId;
  }

  private void process(String role) {
    service.process(request, role, userId, shopOf(role));
  }

  private void verifyNoInsert() {
    verify(createStaffMapper, never())
        .createStaff(any(), any(), any(), any(), any(), any(), any(), any());
  }

  private void assertRequestValid() {
    Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);
    assertEquals(0, violations.size(), violations.toString());
  }

  private void assertHasViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<CreateStaffRequest>> violations = validator.validate(request);
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

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_Success_WhenCreatingStaffWithValidData_TC001(String role) {
    stubValidCreate(role);

    assertDoesNotThrow(() -> process(role));

    verify(createStaffMapper)
        .createStaff(
            "newstaff@coffee.com",
            "newstaff",
            "Nguyen Van B",
            "0901234567",
            roleId,
            shopId,
            userId,
            role);
  }

  @Test
  void process_Success_WhenFieldsHaveSurroundingSpaces_TC002() {
    request.setEmail("  newstaff@coffee.com ");
    request.setUsername(" newstaff  ");
    request.setFullName("  Nguyen Van B ");
    request.setPhoneNumber(" 0901234567 ");
    stubValidCreate(ownerRole);

    assertDoesNotThrow(() -> process(ownerRole));

    verify(createStaffMapper)
        .createStaff(
            "newstaff@coffee.com",
            "newstaff",
            "Nguyen Van B",
            "0901234567",
            roleId,
            shopId,
            userId,
            ownerRole);
  }

  @Test
  void process_Success_WhenUsernameEmailAndPhoneAreUnique_TC004() {
    stubValidCreate(ownerRole);

    assertDoesNotThrow(() -> process(ownerRole));

    // Username, email and phone number are each checked once; full name is never checked
    verify(commonMapper).checkStaffExistedByUsername("newstaff");
    verify(commonMapper).checkStaffEmailExisted("newstaff@coffee.com");
    verify(commonMapper).checkStaffPhoneExisted("0901234567", null);
    verify(commonMapper).checkShopExisted(shopId);
    verify(commonMapper).findRoleNameById(roleId);
    verifyNoMoreInteractions(commonMapper);
  }

  @Test
  void process_Success_WhenManagerCreatesStaffRoleCheckIsSkipped_TC005() {
    stubValidCreate(managerRole);

    assertDoesNotThrow(() -> process(managerRole));

    verify(commonMapper, never()).findRoleNameById(any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SHOP & ROLE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC006(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(ErrorMessage.DATA_NOT_FOUND, exception.getMessage());
    assertEquals(shopId, exception.getId());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToShop_TC007() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, userId, null));

    assertEquals(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH, exception.getMessage());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMismatchesRequestShop_TC008() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000309");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, userId, otherShopId));

    assertEquals(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED, exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotShopMember_TC009() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(managerRole));

    assertEquals(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED, exception.getMessage());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenRoleDoesNotExist_TC010() {
    stubShopAccess(ownerRole);
    when(commonMapper.findRoleNameById(roleId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(ownerRole));

    assertEquals(roleId, exception.getId());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenRoleIsOwner_TC011() {
    stubShopAccess(ownerRole);
    when(commonMapper.findRoleNameById(roleId)).thenReturn(Roles.OWNER.getValue());

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals(ErrorMessage.ROLE_NOT_ASSIGNABLE, exception.getMessage());
    verifyNoInsert();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DUPLICATE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsUserExistException_WhenUsernameIsExisted_TC012(String role) {
    stubValidAccess(role);
    when(commonMapper.checkStaffExistedByUsername("newstaff")).thenReturn(true);

    UserExistException exception = assertThrows(UserExistException.class, () -> process(role));

    assertEquals(ErrorMessage.STAFF_USERNAME_EXISTED, exception.getMessage());
    verify(commonMapper, never()).checkStaffEmailExisted(any());
    verifyNoInsert();
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsEmailExistedException_WhenEmailIsExisted_TC013(String role) {
    stubValidAccess(role);
    when(commonMapper.checkStaffExistedByUsername("newstaff")).thenReturn(false);
    when(commonMapper.checkStaffEmailExisted("newstaff@coffee.com")).thenReturn(true);

    EmailExistedException exception =
        assertThrows(EmailExistedException.class, () -> process(role));

    assertEquals(ErrorMessage.STAFF_EMAIL_EXISTED, exception.getMessage());
    assertEquals("newstaff@coffee.com", exception.getEmail());
    verifyNoInsert();
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsPhoneNumberExistedException_WhenPhoneNumberIsExisted_TC014(String role) {
    stubValidAccess(role);
    when(commonMapper.checkStaffExistedByUsername("newstaff")).thenReturn(false);
    when(commonMapper.checkStaffEmailExisted("newstaff@coffee.com")).thenReturn(false);
    when(commonMapper.checkStaffPhoneExisted("0901234567", null)).thenReturn(true);

    PhoneNumberExistedException exception =
        assertThrows(PhoneNumberExistedException.class, () -> process(role));

    assertEquals(ErrorMessage.STAFF_PHONE_EXISTED, exception.getMessage());
    assertEquals("0901234567", exception.getPhoneNumber());
    verifyNoInsert();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenInsertingStaffFails_TC015() {
    stubValidCreate(ownerRole);
    doThrow(new DataAccessException("Insert staff failed") {})
        .when(createStaffMapper)
        .createStaff(any(), any(), any(), any(), any(), any(), any(), any());

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));

    assertEquals("Insert staff failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC016() {
    assertRequestValid();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenFullNameIsBlank(String fullName) {
    request.setFullName(fullName);
    assertHasViolation("fullName", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenFullNameIsNull() {
    request.setFullName(null);
    assertHasViolation("fullName", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Success_WhenPhoneNumberIsAtLengthBoundary_TC018() {
    request.setPhoneNumber("0123456789");
    assertRequestValid();

    request.setPhoneNumber("01234567890");
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenTextFieldsAreAtMaxLength100_TC019() {
    request.setEmail("a".repeat(40) + "@" + "b".repeat(55) + ".com");
    request.setUsername("a".repeat(100));
    request.setFullName("A".repeat(100));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenUsernameHasAllowedSymbols_TC020() {
    request.setUsername("new.staff_01-a");
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenEmailIsBlank_TC021(String email) {
    request.setEmail(email);
    assertHasViolation("email", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenEmailIsNull_TC022() {
    request.setEmail(null);
    assertHasViolation("email", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "abc@", "@coffee.com", "a b@coffee.com"})
  void requestValidation_ReportsEmailInvalid_WhenEmailFormatIsWrong_TC023(String email) {
    request.setEmail(email);
    assertHasViolation("email", ValidationMessage.EMAIL_INVALID);
  }

  @Test
  void requestValidation_ReportsSizeMax_WhenEmailExceeds100Characters_TC024() {
    request.setEmail("a".repeat(40) + "@" + "b".repeat(56) + ".com");
    assertHasViolation("email", ValidationMessage.SIZE_MAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenUsernameIsBlank_TC025(String username) {
    request.setUsername(username);
    assertHasViolation("username", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenUsernameIsNull_TC026() {
    request.setUsername(null);
    assertHasViolation("username", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"new staff", "new@staff", "nguyễn", "new/staff"})
  void requestValidation_ReportsSpecialCharacters_WhenUsernameHasInvalidCharacters_TC027(
      String username) {
    request.setUsername(username);
    assertHasViolation("username", ValidationMessage.SPECIAL_CHARACTERS);
  }

  @Test
  void requestValidation_ReportsSizeMax_WhenUsernameExceeds100Characters_TC028() {
    request.setUsername("a".repeat(101));
    assertHasViolation("username", ValidationMessage.SIZE_MAX);
  }

  @Test
  void requestValidation_ReportsSizeMax_WhenFullNameExceeds100Characters_TC029() {
    request.setFullName("A".repeat(101));
    assertHasViolation("fullName", ValidationMessage.SIZE_MAX);
  }

  @ParameterizedTest
  @ValueSource(strings = {"Name@", "Name#1", "Name<script>", "Name!"})
  void requestValidation_ReportsSpecialCharacters_WhenFullNameHasInvalidCharacters_TC030(
      String fullName) {
    request.setFullName(fullName);
    assertHasViolation("fullName", ValidationMessage.SPECIAL_CHARACTERS);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenPhoneNumberIsBlank_TC031(String phoneNumber) {
    request.setPhoneNumber(phoneNumber);
    assertHasViolation("phoneNumber", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenPhoneNumberIsNull_TC032() {
    request.setPhoneNumber(null);
    assertHasViolation("phoneNumber", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"09012a4567", "0901 34567", "+84901234567"})
  void requestValidation_ReportsSpecialCharacters_WhenPhoneNumberIsNotDigits_TC033(
      String phoneNumber) {
    request.setPhoneNumber(phoneNumber);
    assertHasViolation("phoneNumber", ValidationMessage.SPECIAL_CHARACTERS);
  }

  @Test
  void requestValidation_ReportsPhoneInvalid_WhenPhoneNumberDoesNotStartWithZero_TC034() {
    request.setPhoneNumber("1901234567");
    assertHasViolation("phoneNumber", ValidationMessage.PHONE_NUMBER_INVALID);
  }

  @ParameterizedTest
  @ValueSource(strings = {"090123456", "012345678901"})
  void requestValidation_ReportsPhoneInvalidLength_WhenPhoneNumberLengthIsOutOfRange_TC035(
      String phoneNumber) {
    request.setPhoneNumber(phoneNumber);
    assertHasViolation("phoneNumber", ValidationMessage.PHONE_INVALID_LENGTH);
  }

  @Test
  void requestValidation_ReportsRequired_WhenRoleIdIsNull_TC036() {
    request.setRoleId(null);
    assertHasViolation("roleId", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenShopIdIsNull_TC037() {
    request.setShopId(null);
    assertHasViolation("shopId", ValidationMessage.FIELD_REQUIRED);
  }
}
