package coffee.api.services.dropdown;

import coffee.api.dto.result.ShopNameResult;
import coffee.api.mapper.GetShopNameMapper;
import coffee.api.services.services_implement.dropdown.ShopNameServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShopNameServiceImplTest {

  @Mock
  private GetShopNameMapper getShopNameMapper;

  @InjectMocks
  private ShopNameServiceImpl shopNameService;

  private ShopNameResult shop1;
  private ShopNameResult shop2;

  @BeforeEach
  void setUp() {
    // Build mock returned items for common test configurations
    shop1 = new ShopNameResult();
    shop1.setShopId(UUID.randomUUID());
    shop1.setShopName("Saigon Drip & Brew");

    shop2 = new ShopNameResult();
    shop2.setShopId(UUID.randomUUID());
    shop2.setShopName("Hanoi Old Quarter Coffee");
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    List<ShopNameResult> expectedShops = Arrays.asList(shop1, shop2);
    when(getShopNameMapper.getShopNames()).thenReturn(expectedShops);

    // Act
    List<ShopNameResult> results = shopNameService.process();

    // Assert
    assertNotNull(results);
    assertEquals(2, results.size());

    // Core data validations
    assertEquals(shop1.getShopId(), results.get(0).getShopId());
    assertEquals("Saigon Drip & Brew", results.get(0).getShopName());

    assertEquals(shop2.getShopId(), results.get(1).getShopId());
    assertEquals("Hanoi Old Quarter Coffee", results.get(1).getShopName());

    verify(getShopNameMapper, times(1)).getShopNames();
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    when(getShopNameMapper.getShopNames()).thenReturn(Collections.emptyList());

    // Act
    List<ShopNameResult> results = shopNameService.process();

    // Assert
    assertNotNull(results);
    assertTrue(results.isEmpty());

    verify(getShopNameMapper, times(1)).getShopNames();
  }
}