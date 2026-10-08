package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
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
public class DeleteDrinkServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteDrinkMapper deleteDrinkMapper;

  @InjectMocks private DeleteDrinkServiceImpl deleteDrinkService;

  private Validator validator;
  private DeleteDrinksRequest validRequest;
  private UUID drinkId;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    drinkId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();
    currentUserShopId = UUID.randomUUID();

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
    verify(deleteDrinkMapper, times(1))
        .deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenUserIsOwner_BypassesShopMembershipCheck_TC002() {
    when(commonMapper.checkDrinkExisted(drinkId, ownerRole, currentUserShopId)).thenReturn(true);
    when(deleteDrinkMapper.deleteDrink(drinkId, currentUserId, ownerRole, currentUserShopId))
        .thenReturn(1);

    assertDoesNotThrow(
        () ->
            deleteDrinkService.process(validRequest, currentUserId, ownerRole, currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1)).checkDrinkExisted(drinkId, ownerRole, currentUserShopId);
    verify(deleteDrinkMapper, times(1))
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
  void process_ThrowsInvalidRequestException_WhenDrinkIdIsNull_TC004() {
    validRequest.setDrinkId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Drink ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(deleteDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerIsNotAssignedToAnyShop_TC005() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> deleteDrinkService.process(validRequest, currentUserId, managerRole, null));

    assertEquals("Manager is not assigned to any shop", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, never()).checkDrinkExisted(any(), any(), any());
    verifyNoInteractions(deleteDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactiveOrDeleted_TC006() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, never()).checkDrinkExisted(any(), any(), any());
    verifyNoInteractions(deleteDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC007() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkDrinkExisted(drinkId, managerRole, currentUserShopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(drinkId, exception.getId());
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1)).checkDrinkExisted(drinkId, managerRole, currentUserShopId);
    verifyNoInteractions(deleteDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkIdIsEmptyUUID_TC008() {
    UUID emptyDrinkId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    validRequest.setDrinkId(emptyDrinkId);

    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkDrinkExisted(emptyDrinkId, managerRole, currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(emptyDrinkId, exception.getId());
    verify(commonMapper, times(1)).checkDrinkExisted(emptyDrinkId, managerRole, currentUserShopId);
    verifyNoInteractions(deleteDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenUserRoleIsStaff_TC009() {
    String staffRole = Roles.STAFF.getValue();
    when(commonMapper.checkDrinkExisted(drinkId, staffRole, currentUserShopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, staffRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteDrinkMapper, never()).deleteDrink(any(), any(), any(), any());
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
    verify(deleteDrinkMapper, times(1))
        .deleteDrink(drinkId, currentUserId, managerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDeleteAffectsNoRows_TC011() {
    when(commonMapper.checkDrinkExisted(drinkId, ownerRole, currentUserShopId)).thenReturn(true);
    when(deleteDrinkMapper.deleteDrink(drinkId, currentUserId, ownerRole, currentUserShopId))
        .thenReturn(0);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteDrinkService.process(
                    validRequest, currentUserId, ownerRole, currentUserShopId));

    assertEquals(
        "Drink has already been deleted or modified by another request", exception.getMessage());
    assertEquals(drinkId, exception.getId());
    verify(deleteDrinkMapper, times(1))
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
  void requestValidation_Fails_WhenDrinkIdIsNull_TC013() {
    validRequest.setDrinkId(null);
    Set<ConstraintViolation<DeleteDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void requestValidation_Fails_WhenRequestIsEmptyInstance_TC014() {
    DeleteDrinksRequest emptyRequest = new DeleteDrinksRequest();
    Set<ConstraintViolation<DeleteDrinksRequest>> violations = validator.validate(emptyRequest);

    assertFalse(violations.isEmpty());
    assertTrue(
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage())));
  }
}
