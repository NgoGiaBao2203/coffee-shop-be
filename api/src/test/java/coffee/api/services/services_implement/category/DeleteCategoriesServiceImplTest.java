package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteCategoryMapper deleteCategoryMapper;

  @InjectMocks private DeleteCategoriesServiceImpl deleteCategoriesService;

  private DeleteCategoriesRequest validRequest;
  private UUID categoryId;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    categoryId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();
    currentUserRoleName = Roles.MANAGER.getValue();

    validRequest = new DeleteCategoriesRequest();
    validRequest.setCategoryId(categoryId);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkCategoryExisted(categoryId, currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    // Using doNothing() for void mapper methods
    doNothing()
        .when(deleteCategoryMapper)
        .deleteCategory(categoryId, currentUserRoleName, currentUserShopId);

    // Act
    assertDoesNotThrow(
        () ->
            deleteCategoriesService.process(validRequest, currentUserRoleName, currentUserShopId));

    // Assert
    verify(commonMapper, times(1))
        .checkCategoryExisted(categoryId, currentUserRoleName, currentUserShopId);
    verify(deleteCategoryMapper, times(1))
        .deleteCategory(categoryId, currentUserRoleName, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryDoesNotExist_TC002() {
    // Arrange
    when(commonMapper.checkCategoryExisted(categoryId, currentUserRoleName, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteCategoriesService.process(
                    validRequest, currentUserRoleName, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1))
        .checkCategoryExisted(categoryId, currentUserRoleName, currentUserShopId);
    verify(deleteCategoryMapper, never()).deleteCategory(any(), any(), any());
  }

  @Test
  void process_Success_WhenUserIsOwner_TC003() {
    // Arrange
    String ownerRole = Roles.OWNER.getValue();

    when(commonMapper.checkCategoryExisted(categoryId, ownerRole, currentUserShopId))
        .thenReturn(true);

    doNothing().when(deleteCategoryMapper).deleteCategory(categoryId, ownerRole, currentUserShopId);

    // Act
    assertDoesNotThrow(
        () -> deleteCategoriesService.process(validRequest, ownerRole, currentUserShopId));

    // Assert
    verify(commonMapper, times(1)).checkCategoryExisted(categoryId, ownerRole, currentUserShopId);
    verify(deleteCategoryMapper, times(1)).deleteCategory(categoryId, ownerRole, currentUserShopId);
  }
}
