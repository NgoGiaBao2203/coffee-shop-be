package coffee.api.services.services_implement.detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.result.DrinkResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.GetDrinkDetailMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetDrinkDetailServiceImplTest {

  @Mock private GetDrinkDetailMapper getDrinkDetailMapper;

  @InjectMocks private GetDrinkDetailServiceImpl getDrinkDetailService;

  private UUID drinkId;
  private UUID currentUserShopId;
  private String currentUserRoleName;
  private DrinkResult sampleDrinkResult;

  @BeforeEach
  void setUp() {
    drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentUserRoleName = "MANAGER";

    DrinkResult.DrinkVariantResult variantS = new DrinkResult.DrinkVariantResult();
    variantS.setSize("S");
    variantS.setPrice(new BigDecimal("25000"));

    DrinkResult.DrinkVariantResult variantM = new DrinkResult.DrinkVariantResult();
    variantM.setSize("M");
    variantM.setPrice(new BigDecimal("30000"));

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setDrinkCategoryId(UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"));
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    sampleDrinkResult.setStatus("ACTIVE");
    sampleDrinkResult.setIsDeleted(false);
    sampleDrinkResult.setVariants(List.of(variantS, variantM));
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName))
        .thenReturn(sampleDrinkResult);

    // Act
    DrinkResult result =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);

    // Assert
    assertNotNull(result);
    assertEquals("Cà Phê Sữa Đá", result.getDrinkName());
    assertEquals("Đang bán", result.getStatus());
    assertNotNull(result.getVariants());
    assertEquals(2, result.getVariants().size());
    assertEquals("S", result.getVariants().getFirst().getSize());
    assertEquals(new BigDecimal("25000"), result.getVariants().getFirst().getPrice());

    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_NotFound_ThrowsDataNotFoundException_TC002() {
    // Arrange
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName))
        .thenReturn(null);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId));

    assertTrue(exception.getMessage().contains("Drink not found with id: " + drinkId));
    assertEquals(drinkId, exception.getId());

    verify(getDrinkDetailMapper, times(1))
        .getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_NormalizeStatus_AllBranches_TC003() {
    sampleDrinkResult.setStatus("1");
    when(getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName))
        .thenReturn(sampleDrinkResult);
    DrinkResult res1 =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);
    assertEquals("Đang bán", res1.getStatus());

    sampleDrinkResult.setStatus("INACTIVE");
    DrinkResult res2 =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);
    assertEquals("Ngừng bán", res2.getStatus());

    sampleDrinkResult.setStatus("0");
    DrinkResult res3 =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);
    assertEquals("Ngừng bán", res3.getStatus());

    sampleDrinkResult.setStatus(null);
    DrinkResult res4 =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);
    assertEquals("UNKNOWN", res4.getStatus());

    sampleDrinkResult.setStatus("OTHER_STATUS");
    DrinkResult res5 =
        getDrinkDetailService.process(drinkId, currentUserRoleName, currentUserShopId);
    assertEquals("Không xác định", res5.getStatus());
  }
}
