package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetDrinksMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetDrinkServiceImplTest {

  @Mock
  private GetDrinksMapper getDrinksMapper;

  @InjectMocks
  private GetDrinkServiceImpl getDrinkService;

  private SearchDrinksRequest validRequest;
  private DrinkResult sampleDrinkResult;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    // Initialize standard valid request
    validRequest = new SearchDrinksRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Cà Phê");
    validRequest.setSortBy("drinkName");
    validRequest.setSortDirection(SortDirection.DESC);

    // Context configurations
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentUserRoleName = "MANAGER";

    // Build mock single returned drink item
    UUID drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    UUID categoryId = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    // Build list of variants according to new DrinkResult DTO structure
    DrinkResult.DrinkVariantResult variantS = new DrinkResult.DrinkVariantResult();
    variantS.setSize("S");
    variantS.setPrice(new BigDecimal("25000"));

    DrinkResult.DrinkVariantResult variantM = new DrinkResult.DrinkVariantResult();
    variantM.setSize("M");
    variantM.setPrice(new BigDecimal("30000"));

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setDrinkCategoryId(categoryId);
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setVariants(List.of(variantS, variantM));
    sampleDrinkResult.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    sampleDrinkResult.setStatus("Đang bán");
    sampleDrinkResult.setIsDeleted(false);
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<DrinkResult> expectedItems = Collections.singletonList(sampleDrinkResult);

    when(getDrinksMapper.countDrinksFiltered(
      search, currentUserShopId, currentUserRoleName
    )).thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
      search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName
    )).thenReturn(expectedItems);

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals("Get drinks successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());

    DrinkResult drinkResult = response.getItems().get(0);
    assertEquals("Cà Phê Sữa Đá", drinkResult.getDrinkName());

    // Assert variants list
    assertNotNull(drinkResult.getVariants());
    assertEquals(2, drinkResult.getVariants().size());
    assertEquals("S", drinkResult.getVariants().get(0).getSize());
    assertEquals(new BigDecimal("25000"), drinkResult.getVariants().get(0).getPrice());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
      .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
      .getDrinksFiltered(search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    validRequest.setSearch("NonExistentDrink");
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getDrinksMapper.countDrinksFiltered(
      search, currentUserShopId, currentUserRoleName
    )).thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
      search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName
    )).thenReturn(Collections.emptyList());

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
      .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
      .getDrinksFiltered(search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    // Arrange
    validRequest.setSearch("   ");
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getDrinksMapper.countDrinksFiltered(
      argThat(s -> s == null || s.isBlank()), eq(currentUserShopId), eq(currentUserRoleName)
    )).thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
      argThat(s -> s == null || s.isBlank()), eq(sortBy), eq(sortDirection), eq(size), eq(offset), eq(currentUserShopId), eq(currentUserRoleName)
    )).thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Sữa Đá", response.getItems().get(0).getDrinkName());
    assertEquals(2, response.getItems().get(0).getVariants().size());

    verify(getDrinksMapper, times(1))
      .countDrinksFiltered(argThat(s -> s == null || s.isBlank()), eq(currentUserShopId), eq(currentUserRoleName));
    verify(getDrinksMapper, times(1))
      .getDrinksFiltered(argThat(s -> s == null || s.isBlank()), eq(sortBy), eq(sortDirection), eq(size), eq(offset), eq(currentUserShopId), eq(currentUserRoleName));
  }

  @Test
  void process_SuccessWithNullListFromMapper_TC004() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(
      search, currentUserShopId, currentUserRoleName
    )).thenReturn(totalElements);

    // Mock Mapper returning null list to test null-safe fallback (Collections.emptyList())
    when(getDrinksMapper.getDrinksFiltered(
      search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName
    )).thenReturn(null);

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());

    verify(getDrinksMapper, times(1))
      .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
      .getDrinksFiltered(search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_WhenDrinksHasVariousStatuses_ShouldNormalizeCorrectly_TC005() {
    DrinkResult d1 = new DrinkResult(); d1.setStatus("1");
    DrinkResult d2 = new DrinkResult(); d2.setStatus("ACTIVE");
    DrinkResult d3 = new DrinkResult(); d3.setStatus("0");
    DrinkResult d4 = new DrinkResult(); d4.setStatus("INACTIVE");
    DrinkResult d5 = new DrinkResult(); d5.setStatus(null);
    DrinkResult d6 = new DrinkResult(); d6.setStatus("OTHER");  

    List<DrinkResult> mockList = Arrays.asList(d1, d2, d3, d4, d5, d6);

    when(getDrinksMapper.countDrinksFiltered(anyString(), any(), anyString())).thenReturn(6L);
    when(getDrinksMapper.getDrinksFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), anyString()))
      .thenReturn(mockList);

    PageResponse<DrinkResult> response = getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertNotNull(response);
    assertEquals(6, response.getItems().size());
    assertEquals("Đang bán", response.getItems().get(0).getStatus());
    assertEquals("Đang bán", response.getItems().get(1).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(2).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(3).getStatus());
    assertEquals("UNKNOWN", response.getItems().get(4).getStatus());
    assertEquals("Không xác định", response.getItems().get(5).getStatus());
  }
}