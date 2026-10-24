package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class GetDrinkDetailServiceImplTest {

  @Mock private GetDrinkDetailMapper getDrinkDetailMapper;

  @Mock private DrinkDetailValidator drinkDetailValidator;

  @InjectMocks private GetDrinkDetailServiceImpl service;

  private UUID drinkId;
  private UUID userId;
  private UUID shopId;

  private String ownerRole;
  private String managerRole;
  private String staffRole;

  private DrinkResult sampleDrink;

  @BeforeEach
  void setUp() {
    drinkId = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");

    userId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();
    staffRole = Roles.STAFF.getValue();

    DrinkResult.DrinkVariantResult variant = new DrinkResult.DrinkVariantResult();

    variant.setDrinkDetailId(UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63"));

    variant.setSize("M");
    variant.setPrice(new BigDecimal("30000.00"));

    sampleDrink = new DrinkResult();

    sampleDrink.setDrinkId(drinkId);
    sampleDrink.setShopId(shopId);

    sampleDrink.setDrinkCategoryId(UUID.fromString("11111111-0000-0000-0000-000000000001"));

    sampleDrink.setCategoryName("Cà Phê");
    sampleDrink.setDrinkName("Cà Phê Sữa Đá");
    sampleDrink.setImageUrl("https://example.com/images/ca-phe-sua-da.jpg");

    sampleDrink.setStatus("ACTIVE");
    sampleDrink.setIsDeleted(false);
    sampleDrink.setVariants(List.of(variant));
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenManagerGetsDrinkDetail_TC001() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    // Act
    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    // Assert
    assertNotNull(result);
    assertEquals(drinkId, result.getDrinkId());
    assertEquals("Cà Phê Sữa Đá", result.getDrinkName());
    assertEquals("Đang bán", result.getStatus());

    assertNotNull(result.getVariants());
    assertEquals(1, result.getVariants().size());

    verify(drinkDetailValidator).verifyShopAssignment(userId, managerRole, shopId);

    verify(getDrinkDetailMapper).getDrinkDetailById(drinkId, shopId, managerRole);
  }

  @Test
  void process_Success_WhenStaffGetsDrinkDetail_TC002() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, staffRole))
        .thenReturn(sampleDrink);

    // Act
    DrinkResult result = service.process(drinkId, userId, staffRole, shopId);

    // Assert
    assertNotNull(result);
    assertEquals(drinkId, result.getDrinkId());

    verify(drinkDetailValidator).verifyShopAssignment(userId, staffRole, shopId);
  }

  @Test
  void process_Success_WhenOwnerHasNoAssignedShop_TC003() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, null, ownerRole)).thenReturn(sampleDrink);

    // Act
    DrinkResult result = service.process(drinkId, userId, ownerRole, null);

    // Assert
    assertNotNull(result);

    verify(drinkDetailValidator).verifyShopAssignment(userId, ownerRole, null);
  }

  // =========================================================================
  // STATUS NORMALIZATION CASES
  // =========================================================================

  @Test
  void process_NormalizesNumericActiveStatus_TC004() {
    sampleDrink.setStatus("1");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Đang bán", result.getStatus());
  }

  @Test
  void process_NormalizesTextActiveStatus_TC005() {
    sampleDrink.setStatus("ACTIVE");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Đang bán", result.getStatus());
  }

  @Test
  void process_NormalizesNumericInactiveStatus_TC006() {
    sampleDrink.setStatus("0");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Ngừng bán", result.getStatus());
  }

  @Test
  void process_NormalizesTextInactiveStatus_TC007() {
    sampleDrink.setStatus("INACTIVE");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Ngừng bán", result.getStatus());
  }

  @Test
  void process_NormalizesNullStatus_TC008() {
    sampleDrink.setStatus(null);

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("UNKNOWN", result.getStatus());
  }

  @Test
  void process_NormalizesUnsupportedStatus_TC009() {
    sampleDrink.setStatus("OTHER");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Không xác định", result.getStatus());
  }

  @Test
  void process_NormalizesEmptyStatus_TC010() {
    sampleDrink.setStatus("");

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertEquals("Không xác định", result.getStatus());
  }

  // =========================================================================
  // VARIANT CASES
  // =========================================================================

  @Test
  void process_Success_WhenVariantsAreNull_TC011() {
    sampleDrink.setVariants(null);

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertNull(result.getVariants());
  }

  @Test
  void process_Success_WhenVariantsAreEmpty_TC012() {
    sampleDrink.setVariants(Collections.emptyList());

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    assertNotNull(result.getVariants());
    assertTrue(result.getVariants().isEmpty());
  }

  @Test
  void process_Success_WhenDrinkHasMultipleVariants_TC013() {
    // Arrange
    DrinkResult.DrinkVariantResult variantM = new DrinkResult.DrinkVariantResult();

    variantM.setDrinkDetailId(UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63"));

    variantM.setSize("M");
    variantM.setPrice(new BigDecimal("30000.00"));

    DrinkResult.DrinkVariantResult variantL = new DrinkResult.DrinkVariantResult();

    variantL.setDrinkDetailId(UUID.fromString("b8861994-e947-425c-a6bb-686f28d7ba64"));

    variantL.setSize("L");
    variantL.setPrice(new BigDecimal("35000.00"));

    sampleDrink.setVariants(List.of(variantM, variantL));

    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenReturn(sampleDrink);

    // Act
    DrinkResult result = service.process(drinkId, userId, managerRole, shopId);

    // Assert
    assertEquals(2, result.getVariants().size());

    assertEquals("M", result.getVariants().get(0).getSize());

    assertEquals(new BigDecimal("30000.00"), result.getVariants().get(0).getPrice());

    assertEquals("L", result.getVariants().get(1).getSize());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkIdIsNull_TC014() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> service.process(null, userId, ownerRole, null));

    assertEquals("Drink ID is required", exception.getMessage());

    verifyNoInteractions(drinkDetailValidator);
    verifyNoInteractions(getDrinkDetailMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerHasNoShop_TC015() {
    // Arrange
    doThrow(new InvalidRequestException("User is not assigned to any shop"))
        .when(drinkDetailValidator)
        .verifyShopAssignment(userId, managerRole, null);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(drinkId, userId, managerRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());

    verify(getDrinkDetailMapper, never()).getDrinkDetailById(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffHasNoShop_TC016() {
    // Arrange
    doThrow(new InvalidRequestException("User is not assigned to any shop"))
        .when(drinkDetailValidator)
        .verifyShopAssignment(userId, staffRole, null);

    // Act & Assert
    assertThrows(
        InvalidRequestException.class, () -> service.process(drinkId, userId, staffRole, null));

    verify(getDrinkDetailMapper, never()).getDrinkDetailById(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerMembershipIsInactive_TC017() {
    // Arrange
    doThrow(new InvalidRequestException("You do not have permission to access this shop"))
        .when(drinkDetailValidator)
        .verifyShopAssignment(userId, managerRole, shopId);

    // Act & Assert
    assertThrows(
        InvalidRequestException.class, () -> service.process(drinkId, userId, managerRole, shopId));

    verify(getDrinkDetailMapper, never()).getDrinkDetailById(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC018() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole)).thenReturn(null);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(drinkId, userId, managerRole, shopId));

    assertEquals("Drink not found with id: " + drinkId, exception.getMessage());

    assertEquals(drinkId, exception.getId());
  }

  @Test
  void process_PropagatesDataAccessException_WhenDatabaseFails_TC019() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, shopId, managerRole))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> service.process(drinkId, userId, managerRole, shopId));

    assertEquals("Database query error", exception.getMessage());

    verify(drinkDetailValidator).verifyShopAssignment(userId, managerRole, shopId);
  }
}
