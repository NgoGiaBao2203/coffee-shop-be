package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.EditCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EditCategoriesServiceImplTest {

  @Mock
  private CommonMapper commonMapper;

  @Mock
  private UpdateCategoryMapper updateCategoryMapper;

  @InjectMocks
  private EditCategoriesServiceImpl editCategoriesService;

  private EditCategoriesRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();

    validRequest = new EditCategoriesRequest();
    validRequest.setCategoryId(UUID.randomUUID());
    validRequest.setCategoryName("Latte");

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // TC001 — Manager success
  @Test
  void process_Success_WhenUserIsManagerAndAllChecksPass_TC001() {
    when(commonMapper.checkCategoryExisted(
      validRequest.getCategoryId(),
      managerRole,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkShopIdIsExisted(
      currentUserId,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkCategoryNameExisted(
      validRequest.getCategoryId(),
      validRequest.getCategoryName(),
      managerRole,
      currentUserShopId
    )).thenReturn(false);

    assertDoesNotThrow(() ->
      editCategoriesService.process(validRequest, managerRole, currentUserId, currentUserShopId)
    );

    verify(updateCategoryMapper, times(1)).updateCategory(
      validRequest,
      managerRole,
      currentUserShopId
    );
  }

  // TC002 — Owner success
  @Test
  void process_Success_WhenUserIsOwnerAndAllChecksPass_TC002() {
    when(commonMapper.checkCategoryExisted(
      validRequest.getCategoryId(),
      ownerRole,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkCategoryNameExisted(
      validRequest.getCategoryId(),
      validRequest.getCategoryName(),
      ownerRole,
      currentUserShopId
    )).thenReturn(false);

    assertDoesNotThrow(() ->
      editCategoriesService.process(validRequest, ownerRole, currentUserId, currentUserShopId)
    );

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(updateCategoryMapper, times(1)).updateCategory(
      validRequest,
      ownerRole,
      currentUserShopId
    );
  }

  // TC003 — Category does not exist
  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC003() {
    when(commonMapper.checkCategoryExisted(
      validRequest.getCategoryId(),
      managerRole,
      currentUserShopId
    )).thenReturn(false);

    DataNotFoundException ex = assertThrows(DataNotFoundException.class, () ->
      editCategoriesService.process(validRequest, managerRole, currentUserId, currentUserShopId)
    );

    assertEquals("Data not found", ex.getMessage());

    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  // TC004 — Manager shopId not found
  @Test
  void process_ThrowsDataNotFoundException_WhenManagerShopIdNotFound_TC004() {
    when(commonMapper.checkCategoryExisted(
      validRequest.getCategoryId(),
      managerRole,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkShopIdIsExisted(
      currentUserId,
      currentUserShopId
    )).thenReturn(false);

    DataNotFoundException ex = assertThrows(DataNotFoundException.class, () ->
      editCategoriesService.process(validRequest, managerRole, currentUserId, currentUserShopId)
    );

    assertEquals("Shop Id not found", ex.getMessage());

    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }

  // TC005 — Category name already exists → throw InvalidRequestWithErrorDetailsException
  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenCategoryNameExists_TC005() {
    when(commonMapper.checkCategoryExisted(
      validRequest.getCategoryId(),
      managerRole,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkShopIdIsExisted(
      currentUserId,
      currentUserShopId
    )).thenReturn(true);

    when(commonMapper.checkCategoryNameExisted(
      validRequest.getCategoryId(),
      validRequest.getCategoryName(),
      managerRole,
      currentUserShopId
    )).thenReturn(true);

    InvalidRequestWithErrorDetailsException ex = assertThrows(
      InvalidRequestWithErrorDetailsException.class,
      () -> editCategoriesService.process(validRequest, managerRole, currentUserId, currentUserShopId)
    );

    assertEquals("Invalid request", ex.getMessage());
    assertFalse(ex.getErrorDetails().isEmpty());
    assertEquals("Category name already exists", ex.getErrorDetails().getFirst().getMessage());

    verify(updateCategoryMapper, never()).updateCategory(any(), any(), any());
  }
}
