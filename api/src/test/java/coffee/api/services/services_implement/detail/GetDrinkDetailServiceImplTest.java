package coffee.api.services.services_implement.detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetDrinkDetailMapper;
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
public class GetDrinkDetailServiceImplTest {

  @Mock private GetDrinkDetailMapper getDrinkDetailMapper;

  @InjectMocks private GetDrinkDetailServiceImpl getDrinkDetailService;

  private UUID drinkId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;
  private DrinkResult sampleDrinkResult;

  @BeforeEach
  void setUp() {
    drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();

    DrinkResult.DrinkVariantResult variantS = new DrinkResult.DrinkVariantResult();
    variantS.setDrinkDetailId(UUID.fromString("c5f1a234-789a-4def-1234-56789abcdef0"));
    variantS.setSize("S");
    variantS.setPrice(new BigDecimal("25000"));

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setShopId(currentUserShopId);
    sampleDrinkResult.setDrinkCategoryId(UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"));
    sampleDrinkResult.setCategoryName("Cà Phê Truyền Thống");
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    sampleDrinkResult.setStatus("ACTIVE");
    sampleDrinkResult.setIsDeleted(false);
    sampleDrinkResult.setVariants(List.of(variantS));
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_WhenUserIsManager_TC001() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertEquals("Cà Phê Sữa Đá", result.getDrinkName());
    assertEquals("Đang bán", result.getStatus());
    assertNotNull(result.getVariants());
    assertEquals(1, result.getVariants().size());

    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, managerRole);
  }

  @Test
  void process_Success_WhenUserIsStaff_TC002() {
    // Arrange: Phủ nhánh STAFF có shopId hợp lệ
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, staffRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, staffRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertEquals(drinkId, result.getDrinkId());
    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, staffRole);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNull_TC003() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, null, ownerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, ownerRole, null);

    // Assert
    assertNotNull(result);
    assertEquals(drinkId, result.getDrinkId());
    verify(getDrinkDetailMapper, times(1)).getDrinkDetailById(drinkId, null, ownerRole);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNotNull_TC004() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, ownerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, ownerRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertEquals(drinkId, result.getDrinkId());
    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, ownerRole);
  }

  @Test
  void process_NormalizeStatus_CoverAllBranchesIndividually_TC005() {
    sampleDrinkResult.setStatus("1");
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(sampleDrinkResult);
    DrinkResult res1 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Đang bán", res1.getStatus());

    sampleDrinkResult.setStatus("ACTIVE");
    DrinkResult res2 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Đang bán", res2.getStatus());

    sampleDrinkResult.setStatus("0");
    DrinkResult res3 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Ngừng bán", res3.getStatus());

    sampleDrinkResult.setStatus("INACTIVE");
    DrinkResult res4 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Ngừng bán", res4.getStatus());

    sampleDrinkResult.setStatus(null);
    DrinkResult res5 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("UNKNOWN", res5.getStatus());

    sampleDrinkResult.setStatus("OTHER");
    DrinkResult res6 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Không xác định", res6.getStatus());

    sampleDrinkResult.setStatus("");
    DrinkResult res7 = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);
    assertEquals("Không xác định", res7.getStatus());
  }

  @Test
  void process_Success_WhenVariantsIsNull_TC005A() {
    // Arrange
    sampleDrinkResult.setVariants(null);
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertNull(result.getVariants());
  }

  @Test
  void process_Success_WhenVariantsIsEmpty_TC005B() {
    // Arrange
    sampleDrinkResult.setVariants(Collections.emptyList());
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertNotNull(result.getVariants());
    assertTrue(result.getVariants().isEmpty());
  }

  @Test
  void process_Success_WhenVariantsHasMultipleItems_TC005C() {
    // Arrange
    DrinkResult.DrinkVariantResult variantM = new DrinkResult.DrinkVariantResult();
    variantM.setDrinkDetailId(UUID.fromString("c5f1a234-789a-4def-1234-56789abcdef1"));
    variantM.setSize("M");
    variantM.setPrice(new BigDecimal("30000"));

    DrinkResult.DrinkVariantResult variantL = new DrinkResult.DrinkVariantResult();
    variantL.setDrinkDetailId(UUID.fromString("c5f1a234-789a-4def-1234-56789abcdef2"));
    variantL.setSize("L");
    variantL.setPrice(new BigDecimal("35000"));

    sampleDrinkResult.setVariants(List.of(variantM, variantL));
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result = getDrinkDetailService.process(drinkId, managerRole, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.getVariants().size());
    assertEquals("M", result.getVariants().get(0).getSize());
    assertEquals(new BigDecimal("30000"), result.getVariants().get(0).getPrice());
    assertEquals("L", result.getVariants().get(1).getSize());
    assertEquals(new BigDecimal("35000"), result.getVariants().get(1).getPrice());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_NotFound_ThrowsDataNotFoundException_TC006() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenReturn(null);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> getDrinkDetailService.process(drinkId, managerRole, currentUserShopId));

    assertTrue(exception.getMessage().contains("Drink not found with id: " + drinkId));
    assertEquals(drinkId, exception.getId());

    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, managerRole);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdIsNull_TC007() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getDrinkDetailService.process(drinkId, managerRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());
    verify(getDrinkDetailMapper, never()).getDrinkDetailById(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIdIsNull_TC008() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getDrinkDetailService.process(drinkId, staffRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());
    verify(getDrinkDetailMapper, never()).getDrinkDetailById(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC009() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, managerRole))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getDrinkDetailService.process(drinkId, managerRole, currentUserShopId));

    assertEquals("Database query error", exception.getMessage());
    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, managerRole);
  }
}
