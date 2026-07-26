package coffee.api.services.category;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.SortDirection;
import coffee.api.repository.category.GetCategoryRepository;
import coffee.api.services.services_implement.Category.GetCategoryServiceImpl;
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
public class GetCategoryServiceImplTest {

  @Mock
  private GetCategoryRepository getCategoryRepository;

  @InjectMocks
  private GetCategoryServiceImpl getCategoryService;

  private SearchCategoriesRequest defaultRequest;
  private CategoryResult categoryResult1;
  private CategoryResult categoryResult2;

  @BeforeEach
  void setUp() {
    defaultRequest = new SearchCategoriesRequest();
    defaultRequest.setPage(1);
    defaultRequest.setSize(10);
    defaultRequest.setSortBy("drinkCategoryId");
    defaultRequest.setSortDirection(SortDirection.ASC);

    UUID shopId = UUID.fromString("e8f9b2d3-c4a1-4d9c-8f1a-2b3c4d5e6f7a");

    categoryResult1 = new CategoryResult();
    categoryResult1.setCategoryId(UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d"));
    categoryResult1.setShopId(shopId);
    categoryResult1.setCategoryName("Cà Phê Truyền Thống");

    categoryResult2 = new CategoryResult();
    categoryResult2.setCategoryId(UUID.fromString("b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e"));
    categoryResult2.setShopId(shopId);
    categoryResult2.setCategoryName("Trà Trái Cây");
  }

  // =========================================================================
  // NORMAL CASES
  // =========================================================================

  @Test
  void process_SuccessWithSingleItem_TC001() {
    defaultRequest.setSearch("Cà Phê");
    when(getCategoryRepository.countCategoriesFiltered(anyString(), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.singletonList(categoryResult1));

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertNotNull(response);
    assertEquals("Get categories successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());

    CategoryResult resultItem = response.getItems().getFirst();
    assertEquals(categoryResult1.getCategoryId(), resultItem.getCategoryId());
    assertEquals(categoryResult1.getShopId(), resultItem.getShopId());
    assertEquals(categoryResult1.getCategoryName(), resultItem.getCategoryName());
  }

  @Test
  void process_SuccessWithMultipleItems_TC002() {
    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(2L);
    when(getCategoryRepository.getCategoriesFiltered(any(), anyString(), anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Arrays.asList(categoryResult1, categoryResult2));

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(2, response.getItems().size());
    assertEquals(2L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Truyền Thống", response.getItems().get(0).getCategoryName());
    assertEquals("Trà Trái Cây", response.getItems().get(1).getCategoryName());
  }

  @Test
  void process_SuccessWithNullAndBlankSearch_TC003() {
    defaultRequest.setSearch("   ");
    String search = defaultRequest.trimmedSearch();
    long totalElements = 5L;

    when(getCategoryRepository.countCategoriesFiltered(isNull(), isNull(), isNull())).thenReturn(totalElements);
    when(getCategoryRepository.getCategoriesFiltered(isNull(), eq("drinkCategoryId"), eq("ASC"), eq(10), eq(0), isNull(), isNull()))
      .thenReturn(Collections.singletonList(categoryResult1));

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertNull(search);
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    assertEquals(5L, response.getPagination().getTotalElements());

    verify(getCategoryRepository, times(1)).countCategoriesFiltered(null, null, null);
    verify(getCategoryRepository, times(1)).getCategoriesFiltered(null, "drinkCategoryId", "ASC", 10, 0, null, null);
  }

  @Test
  void process_SuccessWithPaginationOnSecondPage_TC004() {
    defaultRequest.setPage(2);
    defaultRequest.setSize(10);
    List<CategoryResult> last5Items = IntStream.range(0, 5).mapToObj(i -> new CategoryResult()).collect(Collectors.toList());

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(15L);
    when(getCategoryRepository.getCategoriesFiltered(any(), anyString(), anyString(), eq(10), eq(10), any(), any()))
      .thenReturn(last5Items);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(5, response.getItems().size());
    assertEquals(2, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(15L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWithCategoryNameSortingDesc_TC005() {
    defaultRequest.setSortBy("categoryName");
    defaultRequest.setSortDirection(SortDirection.DESC);

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.emptyList());

    getCategoryService.process(defaultRequest);

    verify(getCategoryRepository, times(1))
      .getCategoriesFiltered(any(), eq("categoryName"), eq("DESC"), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_SuccessWithExactPageDivision_TC006() {
    defaultRequest.setPage(2);
    defaultRequest.setSize(10);
    List<CategoryResult> last10Items = IntStream.range(0, 10).mapToObj(i -> new CategoryResult()).collect(Collectors.toList());

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(20L);
    when(getCategoryRepository.getCategoriesFiltered(any(), anyString(), anyString(), eq(10), eq(10), any(), any()))
      .thenReturn(last10Items);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(10, response.getItems().size());
    assertEquals(2, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(20L, response.getPagination().getTotalElements());
  }

  @Test
  void process_SuccessWithSearchContainingSpecialChars_TC007() {
    String searchTerm = "Trà O'long";
    defaultRequest.setSearch(searchTerm);

    when(getCategoryRepository.countCategoriesFiltered(eq(searchTerm), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(eq(searchTerm), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.singletonList(categoryResult2));

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(1, response.getItems().size());
    verify(getCategoryRepository, times(1)).countCategoriesFiltered(eq(searchTerm), isNull(), isNull());
  }
  @Test
  void process_SuccessWhenRequestingLastPageExactly_TC008() {
    defaultRequest.setPage(3);
    defaultRequest.setSize(10);
    List<CategoryResult> last3Items = IntStream.range(0, 3)
      .mapToObj(i -> new CategoryResult())
      .collect(Collectors.toList());

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(23L);
    when(getCategoryRepository.getCategoriesFiltered(any(), anyString(), anyString(), eq(10), eq(20), any(), any()))
      .thenReturn(last3Items);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(3, response.getItems().size());
    assertEquals(3, response.getPagination().getPage());
    assertEquals(3, response.getPagination().getTotalPages());
    assertEquals(23L, response.getPagination().getTotalElements());
  }

  // =========================================================================
  // ABNORMAL CASES
  // =========================================================================

  @Test
  void process_SuccessWithEmptyResult_TC009() {
    defaultRequest.setSearch("NonExistentCategory");
    when(getCategoryRepository.countCategoriesFiltered(anyString(), any(), any())).thenReturn(0L);
    when(getCategoryRepository.getCategoriesFiltered(anyString(), anyString(), anyString(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.emptyList());

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
  }

  @Test
  void process_ThrowsExceptionWhenCountRepositoryFails_TC010() {
    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any()))
      .thenThrow(new DataAccessException("DB count error") {
      });

    assertThrows(DataAccessException.class, () -> getCategoryService.process(defaultRequest));
  }

  @Test
  void process_ThrowsExceptionWhenGetRepositoryFails_TC011() {
    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenThrow(new DataAccessException("DB get error") {
      });

    assertThrows(DataAccessException.class, () -> getCategoryService.process(defaultRequest));
  }

  @Test
  void process_HandlesRepositoryReturningNullList_TC012() {
    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(null);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesRepositoryReturningNullItemInList_TC013() {
    List<CategoryResult> listWithNull = new ArrayList<>();
    listWithNull.add(categoryResult1);
    listWithNull.add(null);
    listWithNull.add(categoryResult2);

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(3L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(listWithNull);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(3, response.getItems().size());
    assertNotNull(response.getItems().get(0));
    assertNull(response.getItems().get(1));
    assertNotNull(response.getItems().get(2));
  }

  @Test
  void process_HandlesPageOutOfBounds_TC014() {
    defaultRequest.setPage(5);
    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(15L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.emptyList());

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertTrue(response.getItems().isEmpty());
    assertEquals(5, response.getPagination().getPage());
    assertEquals(2, response.getPagination().getTotalPages());
    assertEquals(15L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesInconsistentCountLessThanGet_TC015() {
    List<CategoryResult> fiveItems = IntStream.range(0, 5)
      .mapToObj(i -> new CategoryResult())
      .collect(Collectors.toList());

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(1L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(fiveItems);

    PageResponse<CategoryResult> response = getCategoryService.process(defaultRequest);

    assertEquals(5, response.getItems().size());
    assertEquals(1L, response.getPagination().getTotalElements());
  }

  @Test
  void process_HandlesRequestForPageZeroOrNegative_TC016() {
    defaultRequest.setPage(0);
    int expectedOffset = -10;

    when(getCategoryRepository.countCategoriesFiltered(any(), any(), any())).thenReturn(0L);
    when(getCategoryRepository.getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
      .thenReturn(Collections.emptyList());

    getCategoryService.process(defaultRequest);

    verify(getCategoryRepository, times(1))
      .getCategoriesFiltered(any(), any(), any(), anyInt(), eq(expectedOffset), any(), any());
  }
}