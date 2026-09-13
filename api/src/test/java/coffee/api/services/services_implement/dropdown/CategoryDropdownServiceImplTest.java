package coffee.api.services.services_implement.dropdown;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.result.CategoryDropdownResult;
import coffee.api.mapper.GetCategoryDropdownMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CategoryDropdownServiceImplTest {

  @Mock private GetCategoryDropdownMapper getCategoryDropdownMapper;

  @InjectMocks private CategoryDropdownServiceImpl categoryDropdownService;

  private UUID ownerShopId;
  private UUID managerShopId;

  @BeforeEach
  void setUp() {
    ownerShopId = UUID.randomUUID();
    managerShopId = UUID.randomUUID();
  }

  @Test
  void process_SuccessWithOwnerRoleGetsAllData_TC001() {
    // Arrange
    UUID category1Id = UUID.randomUUID();
    UUID category2Id = UUID.randomUUID();

    CategoryDropdownResult category1 = new CategoryDropdownResult();
    category1.setCategoryId(category1Id);
    category1.setCategoryName("Coffee");
    category1.setShopId(ownerShopId);
    category1.setShopName("Shop A");

    CategoryDropdownResult category2 = new CategoryDropdownResult();
    category2.setCategoryId(category2Id);
    category2.setCategoryName("Tea");
    category2.setShopId(managerShopId);
    category2.setShopName("Shop B");

    List<CategoryDropdownResult> allCategories = Arrays.asList(category1, category2);

    when(getCategoryDropdownMapper.getCategoryDropdown("OWNER", ownerShopId))
        .thenReturn(allCategories);

    // Act
    List<CategoryDropdownResult> results = categoryDropdownService.process("OWNER", ownerShopId);

    // Assert
    assertNotNull(results);
    assertEquals(2, results.size());
    assertEquals("Coffee", results.get(0).getCategoryName());
    assertEquals("Shop A", results.get(0).getShopName());
    assertEquals("Tea", results.get(1).getCategoryName());
    assertEquals("Shop B", results.get(1).getShopName());

    verify(getCategoryDropdownMapper, times(1)).getCategoryDropdown("OWNER", ownerShopId);
  }

  @Test
  void process_SuccessWithManagerRoleGetsOnlyTheirShopData_TC002() {
    // Arrange
    UUID category1Id = UUID.randomUUID();

    CategoryDropdownResult category1 = new CategoryDropdownResult();
    category1.setCategoryId(category1Id);
    category1.setCategoryName("Coffee");
    category1.setShopId(managerShopId);
    category1.setShopName("Shop B");

    List<CategoryDropdownResult> managerCategories = Collections.singletonList(category1);

    when(getCategoryDropdownMapper.getCategoryDropdown("MANAGER", managerShopId))
        .thenReturn(managerCategories);

    // Act
    List<CategoryDropdownResult> results =
        categoryDropdownService.process("MANAGER", managerShopId);

    // Assert
    assertNotNull(results);
    assertEquals(1, results.size());
    assertEquals("Coffee", results.get(0).getCategoryName());
    assertEquals("Shop B", results.get(0).getShopName());
    assertTrue(
        results.stream().allMatch(c -> c.getShopId().equals(managerShopId)),
        "Manager should only see categories from their shop");

    verify(getCategoryDropdownMapper, times(1)).getCategoryDropdown("MANAGER", managerShopId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC003() {
    // Arrange
    when(getCategoryDropdownMapper.getCategoryDropdown("MANAGER", managerShopId))
        .thenReturn(Collections.emptyList());

    // Act
    List<CategoryDropdownResult> results =
        categoryDropdownService.process("MANAGER", managerShopId);

    // Assert
    assertNotNull(results);
    assertTrue(results.isEmpty());

    verify(getCategoryDropdownMapper, times(1)).getCategoryDropdown("MANAGER", managerShopId);
  }
}
