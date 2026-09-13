package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import coffee.api.security.CustomUserDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class DeleteCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteCategoryMapper deleteCategoryMapper;

  @InjectMocks private DeleteCategoriesServiceImpl deleteCategoriesService;

  private Validator validator;
  private DeleteCategoriesRequest validRequest;
  private UUID currentUserShopId;
  private UUID currentProfileId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserShopId = UUID.randomUUID();
    currentProfileId = UUID.randomUUID();

    validRequest = new DeleteCategoriesRequest();
    validRequest.setCategoryId(UUID.randomUUID());

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void mockSecurityContextWithCustomUser(UUID userId) {
    CustomUserDetail userDetail = mock(CustomUserDetail.class);
    when(userDetail.getUserId()).thenReturn(userId);

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(userDetail);

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);
  }

  private void mockSecurityContextWithNonCustomUser() {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn("anonymousUser");

    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    SecurityContextHolder.setContext(securityContext);
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithActiveShop_TC001() {
    mockSecurityContextWithCustomUser(currentProfileId);

    when(commonMapper.checkShopIdIsExisted(currentProfileId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);

    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentProfileId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);

    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentProfileId);
  }

  @Test
  void process_Success_WhenUserIsOwner_BypassesShopMembershipCheck_TC002() {
    mockSecurityContextWithCustomUser(currentProfileId);

    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);

    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, ownerRole, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, currentUserShopId);

    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentProfileId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentProfileId);
  }

  @Test
  void process_Success_WhenUserRoleIsNotManager_TC003() {
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), Roles.STAFF.getValue(), currentUserShopId))
        .thenReturn(true);

    assertDoesNotThrow(
        () ->
            deleteCategoriesService.process(
                validRequest, Roles.STAFF.getValue(), currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), Roles.STAFF.getValue(), currentUserShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), Roles.STAFF.getValue(), currentUserShopId, null);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), Roles.STAFF.getValue(), currentUserShopId, null);
  }

  @Test
  void process_Success_WhenSecurityContextIsEmpty_CoversReturnNullBranch_TC004() {
    SecurityContextHolder.clearContext();

    when(commonMapper.checkShopIdIsExisted(isNull(), eq(currentUserShopId))).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);

    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(isNull(), eq(currentUserShopId));
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(validRequest.getCategoryId(), managerRole, currentUserShopId, null);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsNullPointerException_WhenRequestIsNull_TC005() {
    when(commonMapper.checkShopIdIsExisted(any(), eq(currentUserShopId))).thenReturn(true);

    // Act & Assert
    assertThrows(
        NullPointerException.class,
        () -> deleteCategoriesService.process(null, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(any(), eq(currentUserShopId));
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC006() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactiveOrDeleted_TC007() {
    mockSecurityContextWithCustomUser(currentProfileId);
    when(commonMapper.checkShopIdIsExisted(currentProfileId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentProfileId, currentUserShopId);
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void
      process_ThrowsInvalidRequestException_WhenAuthenticationPrincipalIsNotCustomUserDetail_TC008() {
    mockSecurityContextWithNonCustomUser();
    when(commonMapper.checkShopIdIsExisted(isNull(), eq(currentUserShopId))).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(isNull(), eq(currentUserShopId));
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC009() {
    mockSecurityContextWithCustomUser(currentProfileId);
    when(commonMapper.checkShopIdIsExisted(currentProfileId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getCategoryId(), exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentProfileId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsDataAccessException_WhenCascadeDeleteFails_TC010() {
    mockSecurityContextWithCustomUser(currentProfileId);
    when(commonMapper.checkShopIdIsExisted(currentProfileId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);

    doThrow(new DataAccessException("Database timeout during cascade deletion") {})
        .when(deleteCategoryMapper)
        .softDeleteDrinkDetailsByCategory(any(), any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database timeout during cascade deletion", exception.getMessage());

    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentProfileId);
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenCategoryIdIsValid_TC011() {
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenCategoryIdIsNull_TC012() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
