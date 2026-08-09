package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteDrinkServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteDrinkMapper deleteDrinkMapper;

  @InjectMocks private DeleteDrinkServiceImpl deleteDrinkService;

  private DeleteDrinksRequest validRequest;
  private UUID drinkId;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    drinkId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();
    currentUserRoleName = Roles.MANAGER.getValue();

    validRequest = new DeleteDrinksRequest();
    validRequest.setDrinkId(drinkId);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkDrinkExisted(drinkId, currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    doNothing()
        .when(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserRoleName, currentUserShopId);

    // Act
    assertDoesNotThrow(
        () -> deleteDrinkService.process(validRequest, currentUserRoleName, currentUserShopId));

    // Assert
    verify(commonMapper, times(1))
        .checkDrinkExisted(drinkId, currentUserRoleName, currentUserShopId);
    verify(deleteDrinkMapper, times(1))
        .deleteDrink(drinkId, currentUserRoleName, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC002() {
    // Arrange
    when(commonMapper.checkDrinkExisted(drinkId, currentUserRoleName, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> deleteDrinkService.process(validRequest, currentUserRoleName, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1))
        .checkDrinkExisted(drinkId, currentUserRoleName, currentUserShopId);
    verify(deleteDrinkMapper, never()).deleteDrink(any(), any(), any());
  }

  @Test
  void process_Success_WhenUserIsOwner_TC003() {
    // Arrange
    String ownerRole = Roles.OWNER.getValue();

    when(commonMapper.checkDrinkExisted(drinkId, ownerRole, currentUserShopId)).thenReturn(true);

    doNothing().when(deleteDrinkMapper).deleteDrink(drinkId, ownerRole, currentUserShopId);

    // Act
    assertDoesNotThrow(
        () -> deleteDrinkService.process(validRequest, ownerRole, currentUserShopId));

    // Assert
    verify(commonMapper, times(1)).checkDrinkExisted(drinkId, ownerRole, currentUserShopId);
    verify(deleteDrinkMapper, times(1)).deleteDrink(drinkId, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsException_WhenCheckDrinkExistedFails_TC004() {
    // Arrange:
    when(commonMapper.checkDrinkExisted(any(), any(), any()))
        .thenThrow(new RuntimeException("Database error"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> deleteDrinkService.process(validRequest, currentUserRoleName, currentUserShopId));

    assertEquals("Database error", exception.getMessage());

    verify(deleteDrinkMapper, never()).deleteDrink(any(), any(), any());
  }
}
