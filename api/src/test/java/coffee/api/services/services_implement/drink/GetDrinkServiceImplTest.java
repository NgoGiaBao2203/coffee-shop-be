package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetDrinksMapper;
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
public class GetDrinkServiceImplTest {

  @Mock private GetDrinksMapper getDrinksMapper;

  @InjectMocks private GetDrinkServiceImpl getDrinkService;

  private SearchDrinksRequest validRequest;
  private DrinkResult sampleDrinkResult;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    validRequest = new SearchDrinksRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Cà Phê");
    validRequest.setSortBy("drinkName");
    validRequest.setSortDirection(SortDirection.DESC);

    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentUserRoleName = "MANAGER";

    UUID drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    UUID categoryId = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setDrinkCategoryId(categoryId);
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    sampleDrinkResult.setStatus("ACTIVE");
    sampleDrinkResult.setIsDeleted(false);
  }

  @Test
  void process_SuccessWithItems_TC001() {
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<DrinkResult> expectedItems = Collections.singletonList(sampleDrinkResult);

    when(getDrinksMapper.countDrinksFiltered(search, currentUserShopId, currentUserRoleName))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName))
        .thenReturn(expectedItems);

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertNotNull(response);
    assertEquals("Get drinks successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());

    DrinkResult drinkResult = response.getItems().getFirst();
    assertEquals("Cà Phê Sữa Đá", drinkResult.getDrinkName());
    assertEquals("Đang bán", drinkResult.getStatus());

    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    validRequest.setSearch("NonExistentDrink");
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getDrinksMapper.countDrinksFiltered(search, currentUserShopId, currentUserRoleName))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName))
        .thenReturn(Collections.emptyList());

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    validRequest.setSearch("   ");
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getDrinksMapper.countDrinksFiltered(
            argThat(s -> s == null || s.isBlank()), eq(currentUserShopId), eq(currentUserRoleName)))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            argThat(s -> s == null || s.isBlank()),
            eq(sortBy),
            eq(sortDirection),
            eq(size),
            eq(offset),
            eq(currentUserShopId),
            eq(currentUserRoleName)))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Sữa Đá", response.getItems().getFirst().getDrinkName());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(
            argThat(s -> s == null || s.isBlank()), eq(currentUserShopId), eq(currentUserRoleName));
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            argThat(s -> s == null || s.isBlank()),
            eq(sortBy),
            eq(sortDirection),
            eq(size),
            eq(offset),
            eq(currentUserShopId),
            eq(currentUserRoleName));
  }

  @Test
  void process_SuccessWithNullListFromMapper_TC004() {
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, currentUserShopId, currentUserRoleName))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName))
        .thenReturn(null);

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, currentUserShopId, currentUserRoleName);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, currentUserShopId, currentUserRoleName);
  }

  @Test
  void process_NormalizeStatus_AllBranches_TC005() {
    DrinkResult item1 = new DrinkResult();
    item1.setStatus("1");

    DrinkResult item2 = new DrinkResult();
    item2.setStatus("INACTIVE");

    DrinkResult item3 = new DrinkResult();
    item3.setStatus("0");

    DrinkResult item4 = new DrinkResult();
    item4.setStatus(null);

    DrinkResult item5 = new DrinkResult();
    item5.setStatus("OTHER");

    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(5L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
        .thenReturn(List.of(item1, item2, item3, item4, item5));

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, currentUserRoleName, currentUserShopId);

    assertEquals("Đang bán", response.getItems().get(0).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(1).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(2).getStatus());
    assertEquals("UNKNOWN", response.getItems().get(3).getStatus());
    assertEquals("Không xác định", response.getItems().get(4).getStatus());
  }
}
