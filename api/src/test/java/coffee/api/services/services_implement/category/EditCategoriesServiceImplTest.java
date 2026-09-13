package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.dto.response.base_response.ErrorDetail;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
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
public class EditCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private UpdateCategoryMapper updateCategoryMapper;

  @InjectMocks private EditCategoriesServiceImpl editCategoriesService;

  private Validator validator;
  private EditCategoriesRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();

    validRequest = new EditCategoriesRequest();
    validRequest.setCategoryId(UUID.randomUUID());
    validRequest.setCategoryName("Tra Trai Cay");
    validRequest.setShopId(currentUserShopId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerAndAllChecksPass_TC001() {
    // Arrange
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            validRequest.getShopId()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            validRequest.getShopId());
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithAnyShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(), validRequest.getCategoryName().trim(), differentShopId))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, ownerRole, currentUserId, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            validRequest.getCategoryId(), validRequest.getCategoryName().trim(), differentShopId);
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_TrimsCategoryNameCorrectly_TC003() {
    // Arrange
    validRequest.setCategoryName("   Ca Phe Phin   ");

    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(), "Ca Phe Phin", validRequest.getShopId()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Ca Phe Phin", validRequest.getCategoryName());
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsNullPointerException_WhenRequestIsNull_TC004() {
    assertThrows(
        NullPointerException.class,
        () -> editCategoriesService.process(null, managerRole, currentUserId, currentUserShopId));

    verifyNoInteractions(commonMapper);
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC005() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editCategoriesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC006() {
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerShopMembershipNotFoundOrInactive_TC007() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop Id not found", exception.getMessage());
    assertEquals(currentUserShopId, exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC008() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getCategoryId(), exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenCategoryNameAlreadyExists_TC009() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            validRequest.getShopId()))
        .thenReturn(true);

    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertNotNull(exception.getErrorDetails());
    assertEquals(1, exception.getErrorDetails().size());

    ErrorDetail errorDetail = exception.getErrorDetails().getFirst();
    assertEquals(ResponseCode.CONFLICT.getCode(), errorDetail.getErrorCode());
    assertEquals("Category name already exists", errorDetail.getMessage());

    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseUpdateFails_TC010() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            validRequest.getShopId()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database connection timeout") {})
        .when(updateCategoryMapper)
        .updateCategory(any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Database connection timeout", exception.getMessage());

    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, managerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC011() {
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameContainsVietnameseAndHyphen_TC012() {
    validRequest.setCategoryName("Trà Ô-long Lài");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameIsAtMinLengthOrMaxLength_TC013() {
    validRequest.setCategoryName("A");
    Set<ConstraintViolation<EditCategoriesRequest>> minViolations =
        validator.validate(validRequest);
    assertEquals(0, minViolations.size());

    validRequest.setCategoryName("A".repeat(100));
    Set<ConstraintViolation<EditCategoriesRequest>> maxViolations =
        validator.validate(validRequest);
    assertEquals(0, maxViolations.size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: CATEGORY ID
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenCategoryIdIsNull_TC014() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: CATEGORY NAME
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenCategoryNameIsNull_TC015() {
    validRequest.setCategoryName(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameIsBlank_TC016() {
    validRequest.setCategoryName("   ");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameExceeds100Characters_TC017() {
    validRequest.setCategoryName("A".repeat(101));
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameContainsSpecialCharacters_TC018() {
    validRequest.setCategoryName("Trà @# Sữa!");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: SHOP ID
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC019() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC020() {
    EditCategoriesRequest emptyRequest = new EditCategoriesRequest();
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(emptyRequest);

    assertEquals(3, violations.size());
  }
}
