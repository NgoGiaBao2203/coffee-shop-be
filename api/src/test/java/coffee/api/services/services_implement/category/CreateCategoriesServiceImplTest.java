package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
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
public class CreateCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateCategoriesMapper createCategoriesMapper;

  @InjectMocks private CreateCategoriesServiceImpl createCategoriesService;

  private Validator validator;
  private CreateCategoriesRequest validRequest;
  private UUID currentUserShopId;
  private UUID currentUserId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserShopId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();

    validRequest = new CreateCategoriesRequest();
    validRequest.setCategoryName("Espresso");
    validRequest.setShopId(currentUserShopId);

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
  void process_Success_WhenUserIsManagerWithMatchingShopAndValidAuth_TC001() {
    mockSecurityContextWithCustomUser(currentUserId);

    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId())))
        .thenReturn(false);

    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId()));
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithDifferentShop_TC002() {
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(differentShopId)))
        .thenReturn(false);

    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, ownerRole, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(differentShopId));
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_TrimsCategoryNameCorrectly_TC003() {
    mockSecurityContextWithCustomUser(currentUserId);
    validRequest.setCategoryName("   Trà Đào Cam Sả   ");

    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq("Trà Đào Cam Sả"), eq(validRequest.getShopId())))
        .thenReturn(false);

    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Trà Đào Cam Sả", validRequest.getCategoryName());
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(isNull(), eq("Trà Đào Cam Sả"), eq(validRequest.getShopId()));
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserRoleIsNotManager_SkipsManagerValidationBlock_TC004() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId())))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            createCategoriesService.process(
                validRequest, Roles.STAFF.getValue(), currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, Roles.STAFF.getValue(), currentUserShopId);
  }

  @Test
  void
      process_Success_WhenManagerWithNoAuthenticationInSecurityContext_CoversReturnNullBranch_TC005() {
    SecurityContextHolder.clearContext();

    when(commonMapper.checkShopIdIsExisted(isNull(), eq(currentUserShopId))).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId())))
        .thenReturn(false);

    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(isNull(), eq(currentUserShopId));
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsNullPointerException_WhenRequestIsNull_TC006() {
    assertThrows(
        NullPointerException.class,
        () -> createCategoriesService.process(null, managerRole, currentUserShopId));

    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createCategoriesMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC007() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkShopExisted(any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC008() {
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkShopExisted(any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC009() {
    mockSecurityContextWithCustomUser(currentUserId);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkShopExisted(any());
  }

  @Test
  void
      process_ThrowsInvalidRequestException_WhenAuthenticationPrincipalIsNotCustomUserDetail_TC010() {
    mockSecurityContextWithNonCustomUser();
    when(commonMapper.checkShopIdIsExisted(isNull(), eq(currentUserShopId))).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(isNull(), eq(currentUserShopId));
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC011() {
    mockSecurityContextWithCustomUser(currentUserId);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenCategoryNameExists_TC012() {
    mockSecurityContextWithCustomUser(currentUserId);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId())))
        .thenReturn(true);

    UserExistException exception =
        assertThrows(
            UserExistException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Category name is existed", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId()));
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC013() {
    mockSecurityContextWithCustomUser(currentUserId);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            isNull(), eq(validRequest.getCategoryName().trim()), eq(validRequest.getShopId())))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createCategoriesMapper)
        .createCategories(any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database insertion error", exception.getMessage());

    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC014() {
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameContainsVietnameseAndHyphen_TC015() {
    validRequest.setCategoryName("Cà-phê Sữa Đá");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameIsAtMinLengthOrMaxLength_TC016() {
    validRequest.setCategoryName("A");
    Set<ConstraintViolation<CreateCategoriesRequest>> minViolations =
        validator.validate(validRequest);
    assertEquals(0, minViolations.size());

    validRequest.setCategoryName("A".repeat(100));
    Set<ConstraintViolation<CreateCategoriesRequest>> maxViolations =
        validator.validate(validRequest);
    assertEquals(0, maxViolations.size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: CATEGORY NAME
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenCategoryNameIsNull_TC017() {
    validRequest.setCategoryName(null);
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameIsBlank_TC018() {
    validRequest.setCategoryName("   ");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameExceeds100Characters_TC019() {
    validRequest.setCategoryName("A".repeat(101));
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameContainsSpecialCharacters_TC020() {
    validRequest.setCategoryName("Cà phê @ 2026!");
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: SHOP ID
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC021() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC022() {
    CreateCategoriesRequest emptyRequest = new CreateCategoriesRequest();
    Set<ConstraintViolation<CreateCategoriesRequest>> violations = validator.validate(emptyRequest);

    assertEquals(2, violations.size());
  }
}
