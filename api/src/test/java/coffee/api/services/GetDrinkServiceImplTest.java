package coffee.api.services;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetDrinksMapper;
import coffee.api.services.services_implement.drink.GetDrinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetDrinkServiceImplTest {

  @Mock
  private GetDrinksMapper getDrinksMapper;

  @InjectMocks
  private GetDrinkServiceImpl getDrinkService;

  private SearchDrinksRequest defaultRequest;
  private DrinkResult drinkResultSizeS;
  private DrinkResult drinkResultSizeM;

  @BeforeEach
  void setUp() {
    defaultRequest = new SearchDrinksRequest();
    defaultRequest.setPage(1);
    defaultRequest.setSize(10);
    defaultRequest.setSortBy("drinkName");
    defaultRequest.setSortDirection(SortDirection.ASC);

    UUID drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    UUID categoryId = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    drinkResultSizeS = new DrinkResult();
    drinkResultSizeS.setDrinkId(drinkId);
    drinkResultSizeS.setDrinkCategoryId(categoryId);
    drinkResultSizeS.setDrinkName("Cà Phê Sữa Đá");
    drinkResultSizeS.setSize("S");
    drinkResultSizeS.setPrice("25.000 đ");
    drinkResultSizeS.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    drinkResultSizeS.setStatus("Đang bán");
    drinkResultSizeS.setIsDeleted(false);

    drinkResultSizeM = new DrinkResult();
    drinkResultSizeM.setDrinkId(drinkId);
    drinkResultSizeM.setDrinkCategoryId(categoryId);
    drinkResultSizeM.setDrinkName("Cà Phê Sữa Đá");
    drinkResultSizeM.setSize("M");
    drinkResultSizeM.setPrice("29.000 đ");
    drinkResultSizeM.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    drinkResultSizeM.setStatus("Đang bán");
    drinkResultSizeM.setIsDeleted(false);
  }

  @Test
  void process_SuccessWithSingleItem_TC001() {
    defaultRequest.setSearch("Cà Phê Sữa Đá");
    when(getDrinksMapper.countDrinksFiltered(anyString(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.singletonList(drinkResultSizeS));

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(1, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());
    DrinkResult resultItem = response.getItems().getFirst();
    assertEquals(drinkResultSizeS.getDrinkId(), resultItem.getDrinkId());
    assertEquals(drinkResultSizeS.getDrinkCategoryId(), resultItem.getDrinkCategoryId());
    assertEquals(drinkResultSizeS.getDrinkName(), resultItem.getDrinkName());
    assertEquals(drinkResultSizeS.getSize(), resultItem.getSize());
    assertEquals(drinkResultSizeS.getPrice(), resultItem.getPrice());
    assertEquals(drinkResultSizeS.getImageUrl(), resultItem.getImageUrl());
    assertEquals(drinkResultSizeS.getStatus(), resultItem.getStatus());
    assertEquals(drinkResultSizeS.getIsDeleted(), resultItem.getIsDeleted());
  }

  @Test
  void process_SuccessWithMultipleSizes_TC002() {
    defaultRequest.setSearch("Cà Phê Sữa Đá");
    when(getDrinksMapper.countDrinksFiltered(anyString(), any(), any())).thenReturn(2L);
    when(getDrinksMapper.getDrinksFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), any())).thenReturn(Arrays.asList(drinkResultSizeS, drinkResultSizeM));

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(2, response.getItems().size());
    assertEquals(2L, response.getPagination().getTotalElements());
    assertEquals("S", response.getItems().get(0).getSize());
    assertEquals("M", response.getItems().get(1).getSize());
  }

  @Test
  void process_SuccessWithEmptyResult_TC003() {
    defaultRequest.setSearch("NonExistentDrink");
    when(getDrinksMapper.countDrinksFiltered(anyString(), any(), any())).thenReturn(0L);
    when(getDrinksMapper.getDrinksFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWithNullAndBlankSearch_TC004() {
    defaultRequest.setSearch("   ");
    String search = defaultRequest.trimmedSearch();
    long totalElements = 5L;
    when(getDrinksMapper.countDrinksFiltered(isNull(), isNull(), isNull())).thenReturn(totalElements);
    when(getDrinksMapper.getDrinksFiltered(isNull(), eq("drinkName"), eq("ASC"), eq(10), eq(0), isNull(), isNull()))
      .thenReturn(Collections.singletonList(drinkResultSizeS));

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertNull(search);
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    assertEquals(5L, response.getPagination().getTotalElements());
    verify(getDrinksMapper, times(1)).countDrinksFiltered(null, null, null);
    verify(getDrinksMapper, times(1)).getDrinksFiltered(null, "drinkName", "ASC", 10, 0, null, null);
  }

  @Test
  void process_SuccessWithPaginationOnSecondPage_TC005() {
    defaultRequest.setPage(2);
    defaultRequest.setSize(10);
    List<DrinkResult> last5Items = IntStream.range(0, 5).mapToObj(i -> new DrinkResult()).collect(Collectors.toList());
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(15L);
    when(getDrinksMapper.getDrinksFiltered(any(), anyString(), anyString(), eq(10), eq(10), any(), any())).thenReturn(last5Items);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(5, response.getItems().size());
    assertEquals(2, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(15L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWithExactPageDivision_TC006() {
    defaultRequest.setPage(2);
    defaultRequest.setSize(10);
    List<DrinkResult> last10Items = IntStream.range(0, 10).mapToObj(i -> new DrinkResult()).collect(Collectors.toList());
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(20L);
    when(getDrinksMapper.getDrinksFiltered(any(), anyString(), anyString(), eq(10), eq(10), any(), any())).thenReturn(last10Items);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(10, response.getItems().size());
    assertEquals(2, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(20L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWithPriceSortingDesc_TC007() {
    defaultRequest.setSortBy("price");
    defaultRequest.setSortDirection(SortDirection.DESC);
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    getDrinkService.process(defaultRequest);

    verify(getDrinksMapper, times(1)).getDrinksFiltered(any(), eq("price"), eq("DESC"), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_SuccessWithDefaultSorting_TC008() {
    SearchDrinksRequest requestWithDefaults = new SearchDrinksRequest();
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    getDrinkService.process(requestWithDefaults);

    verify(getDrinksMapper, times(1)).getDrinksFiltered(any(), eq("drinkId"), eq("ASC"), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_SuccessWithSearchContainingSpecialChars_TC009() {
    String searchTerm = "Trà O'long";
    defaultRequest.setSearch(searchTerm);
    when(getDrinksMapper.countDrinksFiltered(eq(searchTerm), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(eq(searchTerm), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    getDrinkService.process(defaultRequest);

    verify(getDrinksMapper, times(1)).countDrinksFiltered(eq(searchTerm), any(), any());
  }

  @Test
  void process_SuccessWhenRequestingLastPageExactly_TC010() {
    defaultRequest.setPage(3);
    defaultRequest.setSize(10);
    List<DrinkResult> last3Items = IntStream.range(0, 3).mapToObj(i -> new DrinkResult()).collect(Collectors.toList());
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(23L);
    when(getDrinksMapper.getDrinksFiltered(any(), anyString(), anyString(), eq(10), eq(20), any(), any())).thenReturn(last3Items);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(3, response.getItems().size());
    assertEquals(3, response.getPagination().getPage());
    assertEquals(3, response.getPagination().getTotalPages());
    assertEquals(23L, response.getPagination().getTotalElements());
  }

  @Test
  void process_ThrowsExceptionWhenCountRepositoryFails_TC011() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenThrow(new DataAccessException("DB count error") {
    });

    assertThrows(DataAccessException.class, () -> getDrinkService.process(defaultRequest));
  }

  @Test
  void process_ThrowsExceptionWhenGetRepositoryFails_TC012() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenThrow(new DataAccessException("DB get error") {
    });

    assertThrows(DataAccessException.class, () -> getDrinkService.process(defaultRequest));
  }

  @Test
  void process_HandlesRepositoryReturningNullList_TC013() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(null);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesPageOutOfBounds_TC014() {
    defaultRequest.setPage(3);
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(15L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertTrue(response.getItems().isEmpty());
    assertEquals(3, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(15L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesInconsistentCountGreaterThanGet_TC015() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(10L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertTrue(response.getItems().isEmpty());
    assertEquals(10L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesInconsistentCountLessThanGet_TC016() {
    List<DrinkResult> fiveItems = IntStream.range(0, 5).mapToObj(i -> new DrinkResult()).collect(Collectors.toList());
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(fiveItems);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(5, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesRepositoryReturningNullItemInList_TC017() {
    List<DrinkResult> listWithNull = new ArrayList<>();
    listWithNull.add(drinkResultSizeS);
    listWithNull.add(null);
    listWithNull.add(drinkResultSizeM);
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(3L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(listWithNull);

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(3, response.getItems().size());
    assertNotNull(response.getItems().get(0));
    assertNull(response.getItems().get(1));
    assertNotNull(response.getItems().get(2));
  }

  @Test
  void process_VerifyGetCallIsMadeWhenCountIsZero_TC018() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(0L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    getDrinkService.process(defaultRequest);

    verify(getDrinksMapper, times(1)).getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_HandlesRequestForPageZeroOrNegative_TC019() {
    defaultRequest.setPage(0);
    int expectedOffset = -10;
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(0L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

    getDrinkService.process(defaultRequest);

    verify(getDrinksMapper, times(1)).getDrinksFiltered(any(), any(), any(), anyInt(), eq(expectedOffset), any(), any());
  }

  @Test
  void process_DoesNotReturnDeletedItems_TC020() {
    when(getDrinksMapper.countDrinksFiltered(any(), any(), any())).thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any())).thenReturn(Collections.singletonList(drinkResultSizeS));

    PageResponse<DrinkResult> response = getDrinkService.process(defaultRequest);

    assertEquals(1, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertFalse(response.getItems().getFirst().getIsDeleted());
  }
}