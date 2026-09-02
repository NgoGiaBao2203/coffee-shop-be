package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.SearchTablesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.TableResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetTablesMapper;
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
public class GetTablesServiceImplTest {

  @Mock private GetTablesMapper getTablesMapper;

  @InjectMocks private GetTablesServiceImpl getTablesService;

  private SearchTablesRequest validRequest;
  private TableResult sampleTableResult;
  private UUID currentUserId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    // Initialize standard valid request with pagination and filters
    validRequest = new SearchTablesRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("1");
    validRequest.setStatus(1); // Available
    validRequest.setSortBy("tableNumber");
    validRequest.setSortDirection(SortDirection.ASC);

    // Context user configurations
    currentUserId = UUID.randomUUID();
    currentUserRoleName = "MANAGER";

    // Build mock single returned item
    sampleTableResult = new TableResult();
    sampleTableResult.setTableId(UUID.randomUUID());
    sampleTableResult.setTableNumber(1);
    sampleTableResult.setDescription("Table near window");
    sampleTableResult.setStatus(1); // Available
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<TableResult> expectedItems = Collections.singletonList(sampleTableResult);

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get tables successfully", response.getMessage());
    assertEquals(1, response.getItems().size());
    assertEquals("Available", response.getItems().get(0).getStatusName());
    assertEquals(1, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());

    verify(getTablesMapper, times(1))
        .countTablesFiltered(search, status, currentUserRoleName, currentUserId);
    verify(getTablesMapper, times(1))
        .getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId);
  }

  @Test
  void process_SuccessWithoutItems_TC002() {
    // Arrange
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    List<TableResult> expectedItems = Collections.emptyList();

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get tables successfully", response.getMessage());
    assertEquals(0, response.getItems().size());
    assertEquals(0, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());
  }

  @Test
  void process_SuccessWithMultipleItems_TC003() {
    // Arrange
    validRequest.setPage(1);
    validRequest.setSize(20);

    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    List<TableResult> expectedItems =
        List.of(
            createTableResult(1, "Table 1", 1),
            createTableResult(2, "Table 2", 1),
            createTableResult(3, "Table 3", 2),
            createTableResult(4, "Table 4", 2),
            createTableResult(5, "Table 5", 3));

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(5, response.getItems().size());
    assertEquals("Available", response.getItems().get(0).getStatusName());
    assertEquals("Occupied", response.getItems().get(2).getStatusName());
    assertEquals("Reserved", response.getItems().get(4).getStatusName());
    assertEquals(5, response.getPagination().getTotalElements());
  }

  @Test
  void process_WithoutStatusFilter_TC004() {
    // Arrange
    validRequest.setStatus(null); // No status filter

    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 2L;

    List<TableResult> expectedItems =
        List.of(createTableResult(1, "Table 1", 1), createTableResult(2, "Table 2", 2));

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(2, response.getItems().size());
    assertEquals(2, response.getPagination().getTotalElements());
  }

  @Test
  void process_WithPagination_TC005() {
    // Arrange
    validRequest.setPage(2);
    validRequest.setSize(5);

    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset(); // (2-1) * 5 = 5

    long totalElements = 12L;

    List<TableResult> expectedItems =
        List.of(
            createTableResult(6, "Table 6", 1),
            createTableResult(7, "Table 7", 1),
            createTableResult(8, "Table 8", 2),
            createTableResult(9, "Table 9", 2),
            createTableResult(10, "Table 10", 3));

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(5, response.getItems().size());
    assertEquals(2, response.getPagination().getPage());
    assertEquals(5, response.getPagination().getSize());
    assertEquals(12, response.getPagination().getTotalElements());
    assertEquals(3, response.getPagination().getTotalPages());
  }

  @Test
  void process_WithDifferentSortBy_TC006() {
    // Arrange
    validRequest.setSortBy("status");
    validRequest.setSortDirection(SortDirection.DESC);

    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<TableResult> expectedItems = Collections.singletonList(sampleTableResult);

    when(getTablesMapper.countTablesFiltered(search, status, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search,
            status,
            sortBy,
            sortDirection,
            size,
            offset,
            currentUserRoleName,
            currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("DESC", sortDirection);
    verify(getTablesMapper, times(1))
        .getTablesFiltered(
            search, status, "status", "DESC", size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_OwnerAccessAllTables_TC007() {
    // Arrange - OWNER role should access tables from all shops
    String ownerRoleName = "OWNER";
    String search = validRequest.trimmedSearch();
    Integer status = validRequest.getStatus();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 8L; // Tables from multiple shops

    List<TableResult> expectedItems =
        List.of(
            createTableResult(1, "Shop A - Table 1", 1),
            createTableResult(2, "Shop A - Table 2", 2),
            createTableResult(3, "Shop B - Table 1", 1),
            createTableResult(4, "Shop B - Table 2", 3),
            createTableResult(5, "Shop C - Table 1", 1),
            createTableResult(6, "Shop C - Table 2", 2),
            createTableResult(7, "Shop D - Table 1", 1),
            createTableResult(8, "Shop D - Table 2", 2));

    when(getTablesMapper.countTablesFiltered(search, status, ownerRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getTablesMapper.getTablesFiltered(
            search, status, sortBy, sortDirection, size, offset, ownerRoleName, currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<TableResult> response =
        getTablesService.process(validRequest, ownerRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get tables successfully", response.getMessage());
    assertEquals(8, response.getItems().size());
    assertEquals(8, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    // Verify that countTablesFiltered was called with OWNER role
    verify(getTablesMapper, times(1))
        .countTablesFiltered(search, status, ownerRoleName, currentUserId);

    // Verify that getTablesFiltered was called with OWNER role
    verify(getTablesMapper, times(1))
        .getTablesFiltered(
            search, status, sortBy, sortDirection, size, offset, ownerRoleName, currentUserId);
  }

  // Helper method to create TableResult
  private TableResult createTableResult(int tableNumber, String description, int status) {
    TableResult result = new TableResult();
    result.setTableId(UUID.randomUUID());
    result.setTableNumber(tableNumber);
    result.setDescription(description);
    result.setStatus(status);
    return result;
  }
}
