package coffee.api.services.services_implement.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.profile.ResetPasswordRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.ResetPasswordMapper;
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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authorization.AuthorizationDeniedException;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private ResetPasswordMapper resetPasswordMapper;

  @InjectMocks private ResetPasswordServiceImpl service;

  private Validator validator;
  private ResetPasswordRequest request;
  private UUID currentUserId;
  private UUID profileId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("00000000-0000-0000-0000-000000000701");
    profileId = UUID.fromString("00000000-0000-0000-0000-000000000702");

    request = new ResetPasswordRequest();
    request.setProfileId(profileId);
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubManageable(String role, Boolean manageable) {
    when(commonMapper.checkStaffManageable(profileId, role, currentUserId)).thenReturn(manageable);
  }

  private void process(String role) {
    service.process(request, role, currentUserId);
  }

  private void assertRequestValid() {
    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
    assertEquals(0, violations.size(), violations.toString());
  }

  private void assertHasViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<ResetPasswordRequest>> violations = validator.validate(request);
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
  void process_Success_WhenTargetIsInCallerScope_TC001(String role) {
    stubManageable(role, true);
    when(resetPasswordMapper.resetPassword(profileId)).thenReturn(1);

    assertDoesNotThrow(() -> process(role));

    // Scope is checked first, then only the target profile is reset
    InOrder inOrder = inOrder(commonMapper, resetPasswordMapper);
    inOrder.verify(commonMapper).checkStaffManageable(profileId, role, currentUserId);
    inOrder.verify(resetPasswordMapper).resetPassword(profileId);
    verifyNoMoreInteractions(commonMapper, resetPasswordMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / PERMISSION
  // =========================================================================

  @Test
  void process_ThrowsAuthorizationDeniedException_WhenCallerIsStaff_TC002() {
    AuthorizationDeniedException exception =
        assertThrows(AuthorizationDeniedException.class, () -> process(Roles.STAFF.getValue()));

    assertEquals("Access denied", exception.getMessage());
    verifyNoInteractions(commonMapper, resetPasswordMapper);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"UNKNOWN", "owner"})
  void process_ThrowsAuthorizationDeniedException_WhenCallerRoleIsUnknown_TC003(String role) {
    assertThrows(AuthorizationDeniedException.class, () -> process(role));

    verifyNoInteractions(commonMapper, resetPasswordMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / SCOPE
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_ThrowsDataNotFoundException_WhenTargetIsOutsideCallerScope_TC004(String role) {
    // OWNER target, the caller themselves, a deleted profile, a MANAGER target for a MANAGER
    // caller, or a STAFF of another shop: the query returns false and the record stays hidden
    stubManageable(role, false);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(ErrorMessage.DATA_NOT_FOUND, exception.getMessage());
    assertEquals(profileId, exception.getId());
    verify(resetPasswordMapper, never()).resetPassword(any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenScopeQueryReturnsNull_TC005() {
    String role = Roles.MANAGER.getValue();
    stubManageable(role, null);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(profileId, exception.getId());
    verify(resetPasswordMapper, never()).resetPassword(any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenProfileWasDeletedMeanwhile_TC006() {
    String role = Roles.OWNER.getValue();
    stubManageable(role, true);
    when(resetPasswordMapper.resetPassword(profileId)).thenReturn(0);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(role));

    assertEquals(profileId, exception.getId());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenScopeCheckFails_TC007() {
    String role = Roles.MANAGER.getValue();
    when(commonMapper.checkStaffManageable(profileId, role, currentUserId))
        .thenThrow(new DataAccessException("Scope check failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(role));

    assertEquals("Scope check failed", error.getMessage());
    verify(resetPasswordMapper, never()).resetPassword(any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenResettingPasswordFails_TC008() {
    String role = Roles.OWNER.getValue();
    stubManageable(role, true);
    when(resetPasswordMapper.resetPassword(profileId))
        .thenThrow(new DataAccessException("Reset password failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(role));

    assertEquals("Reset password failed", error.getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenProfileIdIsProvided_TC009() {
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_ReportsRequired_WhenProfileIdIsNull_TC010() {
    request.setProfileId(null);
    assertHasViolation("profileId", ValidationMessage.FIELD_REQUIRED);
  }
}
