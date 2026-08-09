package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.Roles;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetCategoriesMapper;
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
public class GetCategoryServiceImplTest {

  @Mock private GetCategoriesMapper getCategoriesMapper;

  @InjectMocks private GetCategoryServiceImpl getCategoryService;

  private SearchCategoriesRequest validRequest;
  private CategoryResult sampleCategoryResult;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    // Initialize standard valid search request
    validRequest = new SearchCategoriesRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setBranchShopId(UUID.randomUUID());
    validRequest.setSearch("Espresso");
    validRequest.setSortBy("categoryName");
    validRequest.setSortDirection(SortDirection.ASC);

    // Context user configurations
    currentUserShopId = UUID.randomUUID();
    currentUserRoleName = Roles.MANAGER.getValue();

    // Build mock single returned item
    sampleCategoryResult = new CategoryResult();
    sampleCategoryResult.setCategoryId(UUID.randomUUID());
    sampleCategoryResult.setShopId(currentUserShopId);
    sampleCategoryResult.setCategoryName("Espresso");
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<CategoryResult> expectedItems = Collections.singletonList(sampleCategoryResult);

    when(getCategoriesMapper.countCategoriesFiltered(
            search, currentUserRoleName, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, currentUserRoleName, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals("Get categories successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());
    assertEquals("Espresso", response.getItems().getFirst().getCategoryName());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, currentUserRoleName, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    validRequest.setSearch("NonExistentCategory");
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getCategoriesMapper.countCategoriesFiltered(
            search, currentUserRoleName, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, currentUserRoleName, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, currentUserRoleName, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    // Arrange
    validRequest.setSearch("   "); // Trimmable whitespace input
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch(); // Evaluates to null
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getCategoriesMapper.countCategoriesFiltered(
            null, currentUserRoleName, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            null,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId))
        .thenReturn(Collections.singletonList(sampleCategoryResult));

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, currentUserRoleName, currentUserShopId);

    // Assert
    assertNull(search);
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Espresso", response.getItems().getFirst().getCategoryName());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(null, currentUserRoleName, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            null,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWhenMapperReturnsNullList_TC004() {
    // Arrange
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getCategoriesMapper.countCategoriesFiltered(
            search, currentUserRoleName, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId))
        .thenReturn(null);

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, currentUserRoleName, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, currentUserRoleName, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWhenBranchShopIdIsNull_TC005() {
    // Arrange
    validRequest.setBranchShopId(null); // branchShopId is null
    UUID branchShopId = validRequest.getBranchShopId(); // should be null

    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 3L;

    List<CategoryResult> expectedItems = Collections.singletonList(sampleCategoryResult);

    when(getCategoriesMapper.countCategoriesFiltered(
            search, currentUserRoleName, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            branchShopId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, currentUserRoleName, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals("Get categories successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals("Espresso", response.getItems().getFirst().getCategoryName());

    // Pagination metadata assertions
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(3L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    // Verify mapper calls
    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, currentUserRoleName, currentUserShopId, null);

    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserShopId,
            null);
  }
}
