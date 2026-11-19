package coffee.api.services.services_implement.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.staff.EditStaffRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateStaffMapper;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class EditStaffServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private UpdateStaffMapper updateStaffMapper;

  private EditStaffServiceImpl service;
  private Validator validator;

  private EditStaffRequest request;
  private UUID shopId;
  private UUID userId;
  private UUID roleId;
  private UUID profileId;
  private String ownerRole;
  private String managerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    service =
        new EditStaffServiceImpl(commonMapper, updateStaffMapper, new StaffValidator(commonMapper));

    shopId = UUID.fromString("00000000-0000-0000-0000-000000000401");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000402");
    roleId = UUID.fromString("00000000-0000-0000-0000-000000000403");
    profileId = UUID.fromString("00000000-0000-0000-0000-000000000404");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();

    request = new EditStaffRequest();
    request.setProfileId(profileId);
    request.setFullName("Nguyen Van A");
    request.setPhoneNumber("0912345678");
    request.setRoleId(roleId);
    request.setShopId(shopId);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubManageable(String role) {
    when(commonMapper.checkStaffManageable(profileId, role, userId)).thenReturn(true);
  }

  private void stubShopAccess(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (managerRole.equals(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    }
  }

  private void stubValidAccess(String role) {
    stubManageable(role);
    stubShopAccess(role);
    if (ownerRole.equals(role)) {
      when(commonMapper.findRoleNameById(roleId)).thenReturn(Roles.STAFF.getValue());
    }
  }

  private UUID shopOf(String role) {
    return ownerRole.equals(role) ? null : shopId;
  }

  private void process(String role) {
    service.process(request, role, userId, shopOf(role));
  }

  private void stubUpdate(String role) {
    when(updateStaffMapper.updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, role))
        .thenReturn(1);
    if (ownerRole.equals(role)) {
      when(updateStaffMapper.updateStaffShop(profileId, shopId, roleId)).thenReturn(1);
    }
  }

  private void verifyNoUpdate() {
    verify(updateStaffMapper, never()).updateStaff(any(), any(), any(), any(), any());
    verify(updateStaffMapper, never()).updateStaffShop(any(), any(), any());
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
    verify(updateStaffMapper, never()).removeOtherStaffShops(any(), any(), any());
  }

  private void assertRequestValid() {
    Set<ConstraintViolation<EditStaffRequest>> violations = validator.validate(request);
    assertEquals(0, violations.size(), violations.toString());
  }

  private void assertHasViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<EditStaffRequest>> violations = validator.validate(request);
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
  void process_Success_WhenEditingStaffInScope_TC001(String role) {
    stubValidAccess(role);
    stubUpdate(role);

    assertDoesNotThrow(() -> process(role));

    verify(updateStaffMapper).updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, role);
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
    if (ownerRole.equals(role)) {
      verify(updateStaffMapper).updateStaffShop(profileId, shopId, roleId);
    } else {
      verify(updateStaffMapper, never()).updateStaffShop(any(), any(), any());
    }
  }

  @Test
  void process_Success_WhenFieldsHaveSurroundingSpaces_TC002() {
    request.setFullName("  Nguyen Van A ");
    request.setPhoneNumber(" 0912345678 ");
    stubValidAccess(ownerRole);
    stubUpdate(ownerRole);

    assertDoesNotThrow(() -> process(ownerRole));

    verify(updateStaffMapper)
        .updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, ownerRole);
  }

  @Test
  void process_Success_WhenPhoneNumberIsNotUsedByAnotherStaff_TC003() {
    stubValidAccess(ownerRole);
    stubUpdate(ownerRole);

    assertDoesNotThrow(() -> process(ownerRole));

    // The phone number is checked ignoring the edited profile itself
    verify(commonMapper).checkStaffManageable(profileId, ownerRole, userId);
    verify(commonMapper).checkStaffPhoneExisted("0912345678", profileId);
    verify(commonMapper).checkShopExisted(shopId);
    verify(commonMapper).findRoleNameById(roleId);
    verify(commonMapper).countOtherActiveStaffShops(profileId, shopId);
    verifyNoMoreInteractions(commonMapper);
  }

  @Test
  void process_Success_WhenManagerEditsStaffRoleCheckIsSkipped_TC004() {
    stubValidAccess(managerRole);
    stubUpdate(managerRole);

    assertDoesNotThrow(() -> process(managerRole));

    verify(commonMapper, never()).findRoleNameById(any());
  }

  @Test
  void process_Success_WhenOwnerAssignsStaffToAnotherShop_TC005() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, ownerRole))
        .thenReturn(1);
    when(updateStaffMapper.updateStaffShop(profileId, shopId, roleId)).thenReturn(0);

    assertDoesNotThrow(() -> process(ownerRole));

    // Only the assignment of the requested shop is touched, other shops stay as they are
    verify(updateStaffMapper).insertStaffShop(profileId, shopId, roleId);
  }

  @Test
  void process_Success_WhenOwnerMovesStaffToAnotherShop_TC006() {
    stubValidAccess(ownerRole);
    stubUpdate(ownerRole);
    when(commonMapper.countOtherActiveStaffShops(profileId, shopId)).thenReturn(1);
    when(commonMapper.countPendingInvoicesByStaff(profileId, null)).thenReturn(0);
    when(updateStaffMapper.removeOtherStaffShops(profileId, shopId, userId)).thenReturn(1);

    assertDoesNotThrow(() -> process(ownerRole));

    // The old shop is left first, then the role is set in the new shop
    InOrder inOrder = inOrder(updateStaffMapper);
    inOrder.verify(updateStaffMapper).removeOtherStaffShops(profileId, shopId, userId);
    inOrder.verify(updateStaffMapper).updateStaffShop(profileId, shopId, roleId);
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
  }

  @Test
  void process_Success_WhenOwnerMovesStaffToShopWithoutAssignment_TC007() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, ownerRole))
        .thenReturn(1);
    when(commonMapper.countOtherActiveStaffShops(profileId, shopId)).thenReturn(1);
    when(commonMapper.countPendingInvoicesByStaff(profileId, null)).thenReturn(0);
    when(updateStaffMapper.removeOtherStaffShops(profileId, shopId, userId)).thenReturn(1);
    when(updateStaffMapper.updateStaffShop(profileId, shopId, roleId)).thenReturn(0);

    assertDoesNotThrow(() -> process(ownerRole));

    verify(updateStaffMapper).insertStaffShop(profileId, shopId, roleId);
  }

  @Test
  void process_Success_WhenOwnerKeepsStaffInSameShop_TC008() {
    stubValidAccess(ownerRole);
    stubUpdate(ownerRole);
    when(commonMapper.countOtherActiveStaffShops(profileId, shopId)).thenReturn(0);

    assertDoesNotThrow(() -> process(ownerRole));

    // Same shop: only the role changes, nobody is removed and invoices are not checked
    verify(updateStaffMapper, never()).removeOtherStaffShops(any(), any(), any());
    verify(commonMapper, never()).countPendingInvoicesByStaff(any(), any());
  }

  @Test
  void process_Success_WhenManagerEditsStaffShopIsNotTouched_TC009() {
    stubValidAccess(managerRole);
    stubUpdate(managerRole);

    assertDoesNotThrow(() -> process(managerRole));

    verify(commonMapper, never()).countOtherActiveStaffShops(any(), any());
    verify(updateStaffMapper, never()).removeOtherStaffShops(any(), any(), any());
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SCOPE, SHOP & ROLE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsDataNotFoundException_WhenStaffIsOutOfScope_TC010(String role) {
    when(commonMapper.checkStaffManageable(profileId, role, userId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(ErrorMessage.DATA_NOT_FOUND, exception.getMessage());
    assertEquals(profileId, exception.getId());
    verify(commonMapper, never()).checkShopExisted(any());
    verifyNoUpdate();
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC011(String role) {
    stubManageable(role);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(shopId, exception.getId());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToShop_TC012() {
    stubManageable(managerRole);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, userId, null));

    assertEquals(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH, exception.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMismatchesRequestShop_TC013() {
    stubManageable(managerRole);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000409");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, userId, otherShopId));

    assertEquals(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED, exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotShopMember_TC014() {
    stubManageable(managerRole);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(managerRole));

    assertEquals(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED, exception.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenRoleDoesNotExist_TC015() {
    stubManageable(ownerRole);
    stubShopAccess(ownerRole);
    when(commonMapper.findRoleNameById(roleId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(ownerRole));

    assertEquals(roleId, exception.getId());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenRoleIsOwner_TC016() {
    stubManageable(ownerRole);
    stubShopAccess(ownerRole);
    when(commonMapper.findRoleNameById(roleId)).thenReturn(Roles.OWNER.getValue());

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals(ErrorMessage.ROLE_NOT_ASSIGNABLE, exception.getMessage());
    verifyNoUpdate();
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenStaffWasDeletedMeanwhile_TC017() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, ownerRole))
        .thenReturn(0);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(ownerRole));

    assertEquals(profileId, exception.getId());
    verify(updateStaffMapper, never()).updateStaffShop(any(), any(), any());
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SHOP TRANSFER CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffHasPendingInvoices_TC018() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(profileId, "Nguyen Van A", "0912345678", roleId, ownerRole))
        .thenReturn(1);
    when(commonMapper.countOtherActiveStaffShops(profileId, shopId)).thenReturn(1);
    when(commonMapper.countPendingInvoicesByStaff(profileId, null)).thenReturn(2);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals(ErrorMessage.STAFF_HAS_PENDING_INVOICES, exception.getMessage());
    verify(updateStaffMapper, never()).removeOtherStaffShops(any(), any(), any());
    verify(updateStaffMapper, never()).updateStaffShop(any(), any(), any());
    verify(updateStaffMapper, never()).insertStaffShop(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DUPLICATE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsPhoneNumberExistedException_WhenPhoneNumberBelongsToAnotherStaff_TC019(
      String role) {
    stubValidAccess(role);
    when(commonMapper.checkStaffPhoneExisted("0912345678", profileId)).thenReturn(true);

    PhoneNumberExistedException exception =
        assertThrows(PhoneNumberExistedException.class, () -> process(role));

    assertEquals(ErrorMessage.STAFF_PHONE_EXISTED, exception.getMessage());
    assertEquals("0912345678", exception.getPhoneNumber());
    verifyNoUpdate();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenUpdatingStaffFails_TC020() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(any(), any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Update staff failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));

    assertEquals("Update staff failed", error.getMessage());
    verify(updateStaffMapper, never()).updateStaffShop(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenInsertingStaffShopFails_TC021() {
    stubValidAccess(ownerRole);
    when(updateStaffMapper.updateStaff(any(), any(), any(), any(), any())).thenReturn(1);
    when(updateStaffMapper.updateStaffShop(any(), any(), any())).thenReturn(0);
    doThrow(new DataAccessException("Insert staff shop failed") {})
        .when(updateStaffMapper)
        .insertStaffShop(profileId, shopId, roleId);

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));

    assertEquals("Insert staff shop failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC022() {
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenPhoneNumberIsAtLengthBoundary_TC023() {
    request.setPhoneNumber("0123456789");
    assertRequestValid();

    request.setPhoneNumber("01234567890");
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenFullNameIsAtMaxLength100_TC024() {
    request.setFullName("A".repeat(100));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenFullNameHasAllowedPunctuation_TC025() {
    request.setFullName("Nguyễn Văn A-B & C (D), E. F'G/H");
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_ReportsRequired_WhenProfileIdIsNull_TC026() {
    request.setProfileId(null);
    assertHasViolation("profileId", ValidationMessage.FIELD_REQUIRED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void requestValidation_ReportsRequired_WhenFullNameIsBlank_TC027(String fullName) {
    request.setFullName(fullName);
    assertHasViolation("fullName", ValidationMessage.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_ReportsRequired_WhenFullNameIsNull_TC028() {
    request.setFullName(null);
    assertHasViolation("fullName", ValidationMessage.FIELD_REQUIRED);
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
    request.setPhoneNumber("1912345678");
    assertHasViolation("phoneNumber", ValidationMessage.PHONE_NUMBER_INVALID);
  }

  @ParameterizedTest
  @ValueSource(strings = {"091234567", "012345678901"})
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
