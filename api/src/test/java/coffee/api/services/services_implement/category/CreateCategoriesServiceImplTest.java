package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateCategoriesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateCategoriesMapper createCategoriesMapper;

  @InjectMocks private CreateCategoriesServiceImpl createCategoriesService;

  private CreateCategoriesRequest validRequest;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    currentUserShopId = UUID.randomUUID();

    validRequest = new CreateCategoriesRequest();
    validRequest.setCategoryName("Espresso");
    validRequest.setShopId(currentUserShopId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  @Test
  void process_Success_WhenUserIsManagerWithMatchingShop_TC001() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);

    // Act & Assert
    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerWithDifferentShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);

    // Act & Assert
    assertDoesNotThrow(
        () -> createCategoriesService.process(validRequest, ownerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(createCategoriesMapper, times(1))
        .createCategories(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC003() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Shop Ids are not match profile", exception.getMessage());

    verify(commonMapper, never()).checkShopExisted(any());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC004() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> createCategoriesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createCategoriesMapper, never()).createCategories(any(), any(), any());
  }
}
