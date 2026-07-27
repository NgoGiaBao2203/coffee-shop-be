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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setDrinkCategoryId(categoryId);
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setSize("S");
    sampleDrinkResult.setPrice("25.000 đ");
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
    assertEquals("Cà Phê Sữa Đá", response.getItems().getFirst().getDrinkName());

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
    validRequest.setSearch("   "); // Trimmable whitespace input
    String search = validRequest.trimmedSearch(); // Evaluates to null
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getDrinksMapper.countDrinksFiltered(
      null, currentUserShopId, currentUserRoleName
    )).thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
      null, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName
    )).thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNull(search);
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Sữa Đá", response.getItems().getFirst().getDrinkName());

    verify(getDrinksMapper, times(1))
      .countDrinksFiltered(null, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
      .getDrinksFiltered(null, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
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
}