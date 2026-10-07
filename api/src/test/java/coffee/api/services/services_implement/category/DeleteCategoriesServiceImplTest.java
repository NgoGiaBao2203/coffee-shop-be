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
public class DeleteCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteCategoryMapper deleteCategoryMapper;

  @InjectMocks private DeleteCategoriesServiceImpl deleteCategoriesService;

  private Validator validator;
  private DeleteCategoriesRequest validRequest;
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

    validRequest = new DeleteCategoriesRequest();
    validRequest.setCategoryId(UUID.randomUUID());

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithActiveShop_TC001() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteCategoryMapper.countPendingInvoicesByCategory(validRequest.getCategoryId()))
        .thenReturn(0);
    when(deleteCategoryMapper.softDeleteCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId))
        .thenReturn(1);

    assertDoesNotThrow(
        () ->
            deleteCategoriesService.process(
                validRequest, managerRole, currentUserShopId, currentUserId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verify(deleteCategoryMapper, times(1))
        .countPendingInvoicesByCategory(validRequest.getCategoryId());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
  }

  @Test
  void process_Success_WhenUserIsOwner_BypassesShopMembershipCheck_TC002() {
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), ownerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteCategoryMapper.countPendingInvoicesByCategory(validRequest.getCategoryId()))
        .thenReturn(0);
    when(deleteCategoryMapper.softDeleteCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentUserId))
        .thenReturn(1);

    assertDoesNotThrow(
        () ->
            deleteCategoriesService.process(
                validRequest, ownerRole, currentUserShopId, currentUserId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), ownerRole, currentUserShopId);
    verify(deleteCategoryMapper, times(1))
        .countPendingInvoicesByCategory(validRequest.getCategoryId());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), ownerRole, currentUserShopId, currentUserId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenUserRoleIsStaff_TC003() {
    String staffRole = Roles.STAFF.getValue();
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), staffRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, staffRole, currentUserShopId, currentUserId));

    assertEquals("Data not found", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteCategoryMapper, never()).countPendingInvoicesByCategory(any());
    verifyNoMoreInteractions(deleteCategoryMapper);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC004() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteCategoriesService.process(
                    null, managerRole, currentUserShopId, currentUserId));

    assertEquals("Category ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCategoryIdIsNull_TC005() {
    validRequest.setCategoryId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Category ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC006() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteCategoriesService.process(validRequest, managerRole, null, currentUserId));

    assertEquals("Manager is not assigned to any shop branch", exception.getMessage());

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactiveOrDeleted_TC007() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("You do not have permission to access this shop branch", exception.getMessage());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkCategoryExisted(any(), any(), any());
    verifyNoInteractions(deleteCategoryMapper);
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
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getCategoryId(), exception.getId());

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkCategoryExisted(validRequest.getCategoryId(), managerRole, currentUserShopId);
    verifyNoInteractions(deleteCategoryMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCategoryHasPendingInvoices_TC009() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteCategoryMapper.countPendingInvoicesByCategory(validRequest.getCategoryId()))
        .thenReturn(1);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Can not delete category with pending invoices", exception.getMessage());

    verify(deleteCategoryMapper, times(1))
        .countPendingInvoicesByCategory(validRequest.getCategoryId());
    verify(deleteCategoryMapper, never())
        .softDeleteDrinkDetailsByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenCascadeDeleteFails_TC010() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteCategoryMapper.countPendingInvoicesByCategory(validRequest.getCategoryId()))
        .thenReturn(0);

    doThrow(new DataAccessException("Database timeout during cascade deletion") {})
        .when(deleteCategoryMapper)
        .softDeleteDrinkDetailsByCategory(any(), any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals("Database timeout during cascade deletion", exception.getMessage());

    verify(deleteCategoryMapper, times(1))
        .countPendingInvoicesByCategory(validRequest.getCategoryId());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, never()).softDeleteDrinksByCategory(any(), any(), any(), any());
    verify(deleteCategoryMapper, never()).softDeleteCategory(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDeleteAffectsNoRows_TC011() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            validRequest.getCategoryId(), managerRole, currentUserShopId))
        .thenReturn(true);
    when(deleteCategoryMapper.countPendingInvoicesByCategory(validRequest.getCategoryId()))
        .thenReturn(0);
    when(deleteCategoryMapper.softDeleteCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId))
        .thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, managerRole, currentUserShopId, currentUserId));

    assertEquals(
        "Category has already been deleted or modified by another request", exception.getMessage());
    assertEquals(validRequest.getCategoryId(), exception.getId());

    verify(deleteCategoryMapper, times(1))
        .countPendingInvoicesByCategory(validRequest.getCategoryId());
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinkDetailsByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteDrinksByCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
    verify(deleteCategoryMapper, times(1))
        .softDeleteCategory(
            validRequest.getCategoryId(), managerRole, currentUserShopId, currentUserId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenCategoryIdIsValid_TC012() {
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenCategoryIdIsNull_TC013() {
    validRequest.setCategoryId(null);
    Set<ConstraintViolation<DeleteCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
