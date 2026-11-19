package coffee.api.services.services_implement.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.staff.DeleteStaffRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteStaffMapper;
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
class DeleteStaffServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private DeleteStaffMapper deleteStaffMapper;

  private DeleteStaffServiceImpl service;
  private Validator validator;

  private DeleteStaffRequest request;
  private UUID shopId;
  private UUID currentUserId;
  private UUID profileId;
  private String ownerRole;
  private String managerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    service = new DeleteStaffServiceImpl(deleteStaffMapper, new StaffValidator(commonMapper));

    shopId = UUID.fromString("00000000-0000-0000-0000-000000000501");
    currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000502");
    profileId = UUID.fromString("00000000-0000-0000-0000-000000000504");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();

    request = new DeleteStaffRequest();
    request.setProfileId(profileId);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubManageable(String role) {
    when(commonMapper.checkStaffManageable(profileId, role, currentUserId)).thenReturn(true);
  }

  private void verifyNoDelete() {
    verify(deleteStaffMapper, never()).softDeleteStaffShops(any(), any(), any());
    verify(deleteStaffMapper, never()).softDeleteStaff(any());
  }

  private void stubNoPendingInvoices(UUID targetShopId) {
    when(commonMapper.countPendingInvoicesByStaff(profileId, targetShopId)).thenReturn(0);
  }

  private void assertHasViolation() {
    Set<ConstraintViolation<DeleteStaffRequest>> violations = validator.validate(request);
    assertTrue(
        violations.stream()
            .anyMatch(
                v ->
                    "profileId".equals(v.getPropertyPath().toString())
                        && ValidationMessage.FIELD_REQUIRED.equals(v.getMessage())),
        violations.toString());
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenOwnerDeletesStaffFromEveryShop_TC001() {
    stubManageable(ownerRole);
    stubNoPendingInvoices(null);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, null, currentUserId)).thenReturn(2);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(0);
    when(deleteStaffMapper.softDeleteStaff(profileId)).thenReturn(1);

    assertDoesNotThrow(() -> service.process(request, ownerRole, currentUserId, null));

    // Shop assignments are soft deleted before the profile
    InOrder order = inOrder(deleteStaffMapper);
    order.verify(deleteStaffMapper).softDeleteStaffShops(profileId, null, currentUserId);
    order.verify(deleteStaffMapper).countActiveStaffShops(profileId);
    order.verify(deleteStaffMapper).softDeleteStaff(profileId);
  }

  @Test
  void process_Success_WhenManagerDeletesStaffWhoOnlyWorksInManagerShop_TC002() {
    stubManageable(managerRole);
    stubNoPendingInvoices(shopId);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, shopId, currentUserId)).thenReturn(1);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(0);
    when(deleteStaffMapper.softDeleteStaff(profileId)).thenReturn(1);

    assertDoesNotThrow(() -> service.process(request, managerRole, currentUserId, shopId));

    verify(deleteStaffMapper).softDeleteStaff(profileId);
  }

  @Test
  void process_Success_WhenManagerRemovesStaffWhoAlsoWorksInAnotherShop_TC003() {
    stubManageable(managerRole);
    stubNoPendingInvoices(shopId);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, shopId, currentUserId)).thenReturn(1);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(1);

    assertDoesNotThrow(() -> service.process(request, managerRole, currentUserId, shopId));

    // The staff keeps the other shop, so the profile itself must stay active
    verify(deleteStaffMapper, never()).softDeleteStaff(any());
  }

  @Test
  void process_Success_WhenOwnerHasNoActiveAssignmentLeft_TC004() {
    stubManageable(ownerRole);
    stubNoPendingInvoices(null);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, null, currentUserId)).thenReturn(0);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(0);
    when(deleteStaffMapper.softDeleteStaff(profileId)).thenReturn(1);

    assertDoesNotThrow(() -> service.process(request, ownerRole, currentUserId, null));

    verify(deleteStaffMapper).softDeleteStaff(profileId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SCOPE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsDataNotFoundException_WhenStaffIsOutOfScope_TC005(String role) {
    when(commonMapper.checkStaffManageable(profileId, role, currentUserId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(request, role, currentUserId, shopId));

    assertEquals(ErrorMessage.DATA_NOT_FOUND, exception.getMessage());
    assertEquals(profileId, exception.getId());
    verifyNoDelete();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToShop_TC006() {
    stubManageable(managerRole);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, currentUserId, null));

    assertEquals(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH, exception.getMessage());
    verifyNoDelete();
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenStaffDoesNotWorkInManagerShop_TC007() {
    stubManageable(managerRole);
    stubNoPendingInvoices(shopId);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, shopId, currentUserId)).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(request, managerRole, currentUserId, shopId));

    assertEquals(profileId, exception.getId());
    verify(deleteStaffMapper, never()).softDeleteStaff(any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenStaffWasAlreadyDeleted_TC008() {
    stubManageable(ownerRole);
    stubNoPendingInvoices(null);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, null, currentUserId)).thenReturn(1);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(0);
    when(deleteStaffMapper.softDeleteStaff(profileId)).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(request, ownerRole, currentUserId, null));

    assertEquals(profileId, exception.getId());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenOwnerDeletesStaffWithPendingInvoices_TC009() {
    stubManageable(ownerRole);
    when(commonMapper.countPendingInvoicesByStaff(profileId, null)).thenReturn(2);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, ownerRole, currentUserId, null));

    assertEquals(ErrorMessage.STAFF_HAS_PENDING_INVOICES, exception.getMessage());
    verifyNoDelete();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerRemovesStaffWithPendingInvoices_TC010() {
    stubManageable(managerRole);
    when(commonMapper.countPendingInvoicesByStaff(profileId, shopId)).thenReturn(1);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, managerRole, currentUserId, shopId));

    assertEquals(ErrorMessage.STAFF_HAS_PENDING_INVOICES, exception.getMessage());
    verifyNoDelete();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenDeletingShopAssignmentFails_TC011() {
    stubManageable(ownerRole);
    stubNoPendingInvoices(null);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, null, currentUserId))
        .thenThrow(new DataAccessException("Delete assignment failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () -> service.process(request, ownerRole, currentUserId, null));

    assertEquals("Delete assignment failed", error.getMessage());
    verify(deleteStaffMapper, never()).softDeleteStaff(any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDeletingProfileFails_TC012() {
    stubManageable(ownerRole);
    stubNoPendingInvoices(null);
    when(deleteStaffMapper.softDeleteStaffShops(profileId, null, currentUserId)).thenReturn(1);
    when(deleteStaffMapper.countActiveStaffShops(profileId)).thenReturn(0);
    when(deleteStaffMapper.softDeleteStaff(profileId))
        .thenThrow(new DataAccessException("Delete profile failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () -> service.process(request, ownerRole, currentUserId, null));

    assertEquals("Delete profile failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenProfileIdIsProvided_TC013() {
    assertEquals(0, validator.validate(request).size());
  }

  @Test
  void requestValidation_ReportsRequired_WhenProfileIdIsNull_TC014() {
    request.setProfileId(null);
    assertHasViolation();
  }
}
