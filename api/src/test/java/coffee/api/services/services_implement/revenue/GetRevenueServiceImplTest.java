package coffee.api.services.services_implement.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.RevenueResult;
import coffee.api.enums.Roles;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetRevenueMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetRevenueServiceImplTest {

  @Mock
  private GetRevenueMapper getRevenueMapper;

  @InjectMocks
  private GetRevenueServiceImpl getRevenueService;

  private SearchRevenueRequest validRequest;
  private RevenueResult sampleRevenueResult;
  private UUID currentUserShopId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    // Initialize standard valid search request
    validRequest = new SearchRevenueRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Cà Phê");
    validRequest.setStartDate(LocalDate.of(2026, 8, 1));
    validRequest.setEndDate(LocalDate.of(2026, 8, 31));
    validRequest.setYear(2026);
    validRequest.setMonth(8);
    validRequest.setSortBy("createdAt");
    validRequest.setSortDirection(SortDirection.DESC);

    // Context user configurations
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    currentUserRoleName = Roles.MANAGER.getValue();

    // Build mock single returned revenue item
    sampleRevenueResult = new RevenueResult();
    sampleRevenueResult.setInvoiceId(UUID.fromString("e1000000-0000-0000-0000-000000000001"));
    sampleRevenueResult.setShopId(currentUserShopId);
    sampleRevenueResult.setShopName("Coffee Shop - Chi nhánh 1");
    sampleRevenueResult.setFullName("Employee One");
    sampleRevenueResult.setDrinkName("Cà Phê Sữa");
    sampleRevenueResult.setCreatedAt(LocalDateTime.of(2026, 8, 4, 10, 15, 30));
    sampleRevenueResult.setTotalAmount(new BigDecimal("99000.00"));
    sampleRevenueResult.setTableNumber(5);
    sampleRevenueResult.setSize("M");
    sampleRevenueResult.setQuantity(1);
    sampleRevenueResult.setPrice(new BigDecimal("29000.00"));
    sampleRevenueResult.setNote("Ít đường");
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    LocalDateTime startDateTime = validRequest.getStartDate().atStartOfDay();
    LocalDateTime endDateTime = validRequest.getEndDate().atTime(LocalTime.MAX);
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<RevenueResult> expectedItems = Collections.singletonList(sampleRevenueResult);

    when(getRevenueMapper.countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    )).thenReturn(totalElements);

    when(getRevenueMapper.getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    )).thenReturn(expectedItems);

    // Act
    PageResponse<RevenueResult> response = getRevenueService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals("Get revenue statistics successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());
    assertEquals("Cà Phê Sữa", response.getItems().getFirst().getDrinkName());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getRevenueMapper, times(1)).countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    );
    verify(getRevenueMapper, times(1)).getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    );
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    validRequest.setSearch("NonExistentItem");
    String search = validRequest.trimmedSearch();
    LocalDateTime startDateTime = validRequest.getStartDate().atStartOfDay();
    LocalDateTime endDateTime = validRequest.getEndDate().atTime(LocalTime.MAX);
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getRevenueMapper.countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    )).thenReturn(totalElements);

    when(getRevenueMapper.getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    )).thenReturn(Collections.emptyList());

    // Act
    PageResponse<RevenueResult> response = getRevenueService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals("No revenue records found", response.getMessage());
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getRevenueMapper, times(1)).countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    );
    verify(getRevenueMapper, times(1)).getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    );
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    // Arrange
    validRequest.setSearch("   "); // Whitespace input
    String search = validRequest.trimmedSearch(); // Evaluates to null
    LocalDateTime startDateTime = validRequest.getStartDate().atStartOfDay();
    LocalDateTime endDateTime = validRequest.getEndDate().atTime(LocalTime.MAX);
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getRevenueMapper.countRevenueFiltered(
      null, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    )).thenReturn(totalElements);

    when(getRevenueMapper.getRevenueFiltered(
      null, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    )).thenReturn(Collections.singletonList(sampleRevenueResult));

    // Act
    PageResponse<RevenueResult> response = getRevenueService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNull(search);
    assertNotNull(response);
    assertEquals("Get revenue statistics successfully", response.getMessage());
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Sữa", response.getItems().getFirst().getDrinkName());

    verify(getRevenueMapper, times(1)).countRevenueFiltered(
      null, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    );
    verify(getRevenueMapper, times(1)).getRevenueFiltered(
      null, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    );
  }

  @Test
  void process_SuccessWhenMapperReturnsNullList_TC004() {
    // Arrange
    String search = validRequest.trimmedSearch();
    LocalDateTime startDateTime = validRequest.getStartDate().atStartOfDay();
    LocalDateTime endDateTime = validRequest.getEndDate().atTime(LocalTime.MAX);
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getRevenueMapper.countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    )).thenReturn(totalElements);

    when(getRevenueMapper.getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    )).thenReturn(null);

    // Act
    PageResponse<RevenueResult> response = getRevenueService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals("No revenue records found", response.getMessage());
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());

    verify(getRevenueMapper, times(1)).countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    );
    verify(getRevenueMapper, times(1)).getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      startDateTime, endDateTime, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    );
  }

  @Test
  void process_SuccessWhenStartAndEndDateAreNull_TC005() {
    validRequest.setStartDate(null);
    validRequest.setEndDate(null);

    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<RevenueResult> expectedItems = Collections.singletonList(sampleRevenueResult);

    when(getRevenueMapper.countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      null, null, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    )).thenReturn(totalElements);

    when(getRevenueMapper.getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      null, null, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    )).thenReturn(expectedItems);

    // Act
    PageResponse<RevenueResult> response = getRevenueService.process(
      validRequest, currentUserRoleName, currentUserShopId
    );

    // Assert
    assertNotNull(response);
    assertEquals("Get revenue statistics successfully", response.getMessage());
    assertEquals(1, response.getItems().size());

    verify(getRevenueMapper, times(1)).countRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      null, null, validRequest.getYear(), validRequest.getMonth(),
      currentUserRoleName, currentUserShopId
    );
    verify(getRevenueMapper, times(1)).getRevenueFiltered(
      search, validRequest.getShopId(), validRequest.getInvoiceId(),
      null, null, validRequest.getYear(), validRequest.getMonth(),
      sortBy, sortDirection, size, offset,
      currentUserRoleName, currentUserShopId
    );
  }
}