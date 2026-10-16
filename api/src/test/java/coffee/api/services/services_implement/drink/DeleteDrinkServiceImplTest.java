package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
import coffee.api.services.services_implement.drink.common.DrinkValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteDrinkServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteDrinkMapper deleteDrinkMapper;

  private DeleteDrinkServiceImpl deleteDrinkService;

  private DeleteDrinksRequest validRequest;
  private UUID drinkId;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    DrinkValidator drinkValidator = new DrinkValidator(commonMapper);
    deleteDrinkService = new DeleteDrinkServiceImpl(deleteDrinkMapper, drinkValidator);

    drinkId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();
    currentUserRoleName = Roles.MANAGER.getValue();

    validRequest = new DeleteDrinksRequest();
    validRequest.setDrinkId(drinkId);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManagerWithActiveShop_TC001() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkDrinkExisted(drinkId, managerRole, currentUserShopId)).thenReturn(true);
    when(deleteDrinkMapper.deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId))
        .thenReturn(1);

    assertDoesNotThrow(
        () ->
            deleteDrinkService.process(
                validRequest, currentUserId, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1)).checkDrinkExisted(drinkId, managerRole, currentUserShopId);
    InOrder inOrder = inOrder(deleteDrinkMapper);
    inOrder.verify(deleteDrinkMapper).softDeleteDrinkDetails(drinkId, currentUserId);
    inOrder
        .verify(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId);
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

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1)).checkDrinkExisted(drinkId, ownerRole, currentUserShopId);
    InOrder inOrder = inOrder(deleteDrinkMapper);
    inOrder.verify(deleteDrinkMapper).softDeleteDrinkDetails(drinkId, currentUserId);
    inOrder
        .verify(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserId, ownerRole, currentUserShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC003() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteDrinkService.process(null, currentUserId, managerRole, currentUserShopId));

    assertEquals("Drink ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteDrinkMapper);
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
  void process_ThrowsDataAccessException_WhenDatabaseAccessFails_TC010() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkDrinkExisted(drinkId, managerRole, currentUserShopId)).thenReturn(true);
    doThrow(new DataAccessException("Database connection timeout") {})
        .when(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId);

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Database connection timeout", exception.getMessage());
    InOrder inOrder = inOrder(deleteDrinkMapper);
    inOrder.verify(deleteDrinkMapper).softDeleteDrinkDetails(drinkId, currentUserId);
    inOrder
        .verify(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDeleteAffectsNoRows_TC011() {
    when(commonMapper.checkDrinkExisted(drinkId, ownerRole, currentUserShopId)).thenReturn(true);

    doNothing().when(deleteDrinkMapper).deleteDrink(drinkId, ownerRole, currentUserShopId);

    // Act
    assertDoesNotThrow(
        () -> deleteDrinkService.process(validRequest, ownerRole, currentUserShopId));

    assertEquals(
        "Drink has already been deleted or modified by another request", exception.getMessage());
    assertEquals(drinkId, exception.getId());
    InOrder inOrder = inOrder(deleteDrinkMapper);
    inOrder.verify(deleteDrinkMapper).softDeleteDrinkDetails(drinkId, currentUserId);
    inOrder
        .verify(deleteDrinkMapper)
        .deleteDrink(drinkId, currentUserId, ownerRole, currentUserShopId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenDrinkIdIsValid_TC012() {
    Set<ConstraintViolation<DeleteDrinksRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
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
