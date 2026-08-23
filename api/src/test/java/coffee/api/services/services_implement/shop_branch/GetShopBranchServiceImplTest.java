package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.SearchShopBranchRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopBranchResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetShopBranchMapper;
import java.time.LocalDateTime;
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
public class GetShopBranchServiceImplTest {

  @Mock private GetShopBranchMapper getShopBranchMapper;

  @InjectMocks private GetShopBranchServiceImpl getShopBranchService;

  private SearchShopBranchRequest validRequest;
  private ShopBranchResult sampleShopBranchResult;
  private UUID currentUserId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    currentUserRoleName = "OWNER";

    validRequest = new SearchShopBranchRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("District 1");
    validRequest.setSortBy("createdAt");
    validRequest.setSortDirection(SortDirection.DESC);

    sampleShopBranchResult = new ShopBranchResult();
    sampleShopBranchResult.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    sampleShopBranchResult.setShopName("Coffee Central Branch");
    sampleShopBranchResult.setAddress("123 Le Loi, District 1, HCMC");
    sampleShopBranchResult.setPhoneNumber("0901234567");
    sampleShopBranchResult.setCreatedAt(LocalDateTime.now());
    sampleShopBranchResult.setUpdatedAt(LocalDateTime.now());
    sampleShopBranchResult.setIsDeleted(false);
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

    List<ShopBranchResult> expectedItems = Collections.singletonList(sampleShopBranchResult);

    when(getShopBranchMapper.countShopBranchesFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getShopBranchMapper.getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<ShopBranchResult> response =
        getShopBranchService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get shop branches successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());

    ShopBranchResult branchResult = response.getItems().getFirst();
    assertEquals("Coffee Central Branch", branchResult.getShopName());
    assertEquals("123 Le Loi, District 1, HCMC", branchResult.getAddress());
    assertEquals("0901234567", branchResult.getPhoneNumber());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getShopBranchMapper, times(1))
        .countShopBranchesFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopBranchMapper, times(1))
        .getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    validRequest.setSearch("NonExistentShop");
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getShopBranchMapper.countShopBranchesFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getShopBranchMapper.getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<ShopBranchResult> response =
        getShopBranchService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getShopBranchMapper, times(1))
        .countShopBranchesFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopBranchMapper, times(1))
        .getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    // Arrange
    validRequest.setSearch("   ");
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 3L;

    when(getShopBranchMapper.countShopBranchesFiltered(
            isNull(), eq(currentUserRoleName), eq(currentUserId)))
        .thenReturn(totalElements);

    when(getShopBranchMapper.getShopBranchesFiltered(
            isNull(),
            eq(sortBy),
            eq(sortDirection),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopBranchResult));

    // Act
    PageResponse<ShopBranchResult> response =
        getShopBranchService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(3L, response.getPagination().getTotalElements());
    assertEquals("Coffee Central Branch", response.getItems().getFirst().getShopName());

    verify(getShopBranchMapper, times(1))
        .countShopBranchesFiltered(isNull(), eq(currentUserRoleName), eq(currentUserId));
    verify(getShopBranchMapper, times(1))
        .getShopBranchesFiltered(
            isNull(),
            eq(sortBy),
            eq(sortDirection),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId));
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

    when(getShopBranchMapper.countShopBranchesFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    // Mapper returns null instead of empty list
    when(getShopBranchMapper.getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(null);

    // Act
    PageResponse<ShopBranchResult> response =
        getShopBranchService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());

    verify(getShopBranchMapper, times(1))
        .countShopBranchesFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopBranchMapper, times(1))
        .getShopBranchesFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_DefaultSortDirectionWhenNull_TC005() {
    // Arrange
    validRequest.setSortDirection(null); // When sort direction is not specified
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();

    when(getShopBranchMapper.countShopBranchesFiltered(any(), any(), any())).thenReturn(1L);

    when(getShopBranchMapper.getShopBranchesFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopBranchResult));

    // Act
    PageResponse<ShopBranchResult> response =
        getShopBranchService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    verify(getShopBranchMapper, times(1))
        .getShopBranchesFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId));
  }
}
