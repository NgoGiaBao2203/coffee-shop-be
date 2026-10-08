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
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(),
            validRequest.getCategoryName().trim(),
            validRequest.getShopId()))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, validRequest.getShopId());
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
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(validRequest.getCategoryId(), differentShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(), validRequest.getCategoryName().trim(), differentShopId))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, ownerRole, currentUserId, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExistsInShop(validRequest.getCategoryId(), differentShopId);
    verify(commonMapper, times(1))
        .checkCategoryNameExisted(
            validRequest.getCategoryId(), validRequest.getCategoryName().trim(), differentShopId);
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_TrimsCategoryNameCorrectly_TC003() {
    validRequest.setCategoryName("   Ca Phe Phin   ");

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryNameExisted(
            validRequest.getCategoryId(), "Ca Phe Phin", validRequest.getShopId()))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editCategoriesService.process(
                validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Ca Phe Phin", validRequest.getCategoryName());
    verify(updateCategoryMapper, times(1))
        .updateCategory(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCategoryNameIsNull_TC004() {
    validRequest.setCategoryName(null);

    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Category name is required", exception.getMessage());
    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC005() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(null, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC006() {
    validRequest.setShopId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTargetShopDoesNotExist_TC007() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerDoesNotHavePermissionOnShop_TC008() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, validRequest.getShopId());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC009() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(true);
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

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, validRequest.getShopId());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenCategoryNameAlreadyExists_TC010() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(true);
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
  void process_ThrowsDataAccessException_WhenDatabaseUpdateFails_TC011() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, validRequest.getShopId()))
        .thenReturn(true);
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

  @Test
  void process_ThrowsDataNotFoundException_WhenOwnerShopIdMismatchesActualCategoryShop_TC012() {
    // Owner sends shopId of shop 0001 while the category actually belongs to shop 0002
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(
            validRequest.getCategoryId(), validRequest.getShopId()))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editCategoriesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getCategoryId(), exception.getId());

    verify(commonMapper, times(1))
        .checkCategoryExistsInShop(validRequest.getCategoryId(), validRequest.getShopId());
    verify(commonMapper, never()).checkCategoryNameExisted(any(), any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCategoryIdIsNull_TC013() {
    validRequest.setCategoryId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Category ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerCurrentShopIdIsNull_TC014() {
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> editCategoriesService.process(validRequest, managerRole, currentUserId, null));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatchesRequestShopId_TC015() {
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editCategoriesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInteractions(updateCategoryMapper);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC016() {
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameContainsVietnameseAndHyphen_TC017() {
    validRequest.setCategoryName("Trà Ô-long Lài");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Success_WhenCategoryNameIsAtMinLengthOrMaxLength_TC018() {
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
  void requestValidation_Fails_WhenCategoryIdIsNull_TC019() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: CATEGORY NAME
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenCategoryNameIsNull_TC020() {
    validRequest.setCategoryName(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameIsBlank_TC021() {
    validRequest.setCategoryName("   ");
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameExceeds100Characters_TC022() {
    validRequest.setCategoryName("A".repeat(101));
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameContainsSpecialCharacters_TC023() {
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
  void requestValidation_Fails_WhenShopIdIsNull_TC024() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_TC025() {
    EditCategoriesRequest emptyRequest = new EditCategoriesRequest();
    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(emptyRequest);

    assertEquals(3, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenCategoryIdAndShopIdAreNull_TC026() {
    validRequest.setCategoryId(null);
    validRequest.setShopId(null);

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(2, violations.size());
    boolean allFieldRequired =
        violations.stream()
            .allMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage()));
    assertTrue(allFieldRequired);
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameAndShopIdAreNull_TC027() {
    validRequest.setCategoryName(null);
    validRequest.setShopId(null);

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(2, violations.size());
    boolean allFieldRequired =
        violations.stream()
            .allMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage()));
    assertTrue(allFieldRequired);
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameIsEmptyString_TC028() {
    validRequest.setCategoryName("");

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    // Chuỗi rỗng vi phạm @NotBlank và @Pattern
    assertFalse(violations.isEmpty());
    boolean hasNotBlankError =
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage()));
    assertTrue(hasNotBlankError);
  }

  @Test
  void requestValidation_Fails_WhenCategoryNameOnlyTabsOrNewlines_TC029() {
    validRequest.setCategoryName("\t\n  ");

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertFalse(violations.isEmpty());
    boolean hasNotBlankError =
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage()));
    assertTrue(hasNotBlankError);
  }

  @Test
  void requestValidation_Success_WhenCategoryNameContainsAlphanumericAndApostrophe_TC030() {
    validRequest.setCategoryName("Cafe 3in1 O'Coffee");

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenAllFieldsAreNull_VerifyEachFieldMessage_TC031() {
    EditCategoriesRequest emptyRequest = new EditCategoriesRequest();

    Set<ConstraintViolation<EditCategoriesRequest>> violations = validator.validate(emptyRequest);

    assertEquals(3, violations.size());

    Set<String> violatedFields =
        violations.stream()
            .map(v -> v.getPropertyPath().toString())
            .collect(java.util.stream.Collectors.toSet());

    assertTrue(violatedFields.contains("categoryId"));
    assertTrue(violatedFields.contains("categoryName"));
    assertTrue(violatedFields.contains("shopId"));
  }
}
