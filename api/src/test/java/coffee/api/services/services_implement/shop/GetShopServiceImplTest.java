package coffee.api.services.services_implement.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop.SearchShopRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ShopResult;
import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import coffee.api.mapper.GetShopMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class GetShopServiceImplTest {

  @Mock private GetShopMapper getShopMapper;

  @InjectMocks private GetShopServiceImpl getShopService;

  private Validator validator;
  private SearchShopRequest validRequest;
  private ShopResult sampleShopResult;
  private UUID currentUserId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    currentUserRoleName = "OWNER";

    validRequest = new SearchShopRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("District 1");
    validRequest.setSortBy("createdAt");
    validRequest.setSortDirection(SortDirection.DESC);

    sampleShopResult = new ShopResult();
    sampleShopResult.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    sampleShopResult.setShopName("Coffee Central Branch");
    sampleShopResult.setAddress("123 Le Loi, District 1, HCMC");
    sampleShopResult.setPhoneNumber("0901234567");
    sampleShopResult.setCreatedAt(LocalDateTime.now());
    sampleShopResult.setUpdatedAt(LocalDateTime.now());
    sampleShopResult.setIsDeleted(false);
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<ShopResult> expectedItems = Collections.singletonList(sampleShopResult);

    when(getShopMapper.countShopsFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getShopMapper.getShopsFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get shops successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());

    ShopResult shopResult = response.getItems().getFirst();
    assertEquals("Coffee Central Branch", shopResult.getShopName());
    assertEquals("123 Le Loi, District 1, HCMC", shopResult.getAddress());
    assertEquals("0901234567", shopResult.getPhoneNumber());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getShopMapper, times(1)).countShopsFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopMapper, times(1))
        .getShopsFiltered(
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

    when(getShopMapper.countShopsFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getShopMapper.getShopsFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getShopMapper, times(1)).countShopsFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopMapper, times(1))
        .getShopsFiltered(
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

    when(getShopMapper.countShopsFiltered(isNull(), eq(currentUserRoleName), eq(currentUserId)))
        .thenReturn(totalElements);

    when(getShopMapper.getShopsFiltered(
            isNull(),
            eq(sortBy),
            eq(sortDirection),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopResult));

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(3L, response.getPagination().getTotalElements());
    assertEquals("Coffee Central Branch", response.getItems().getFirst().getShopName());

    verify(getShopMapper, times(1))
        .countShopsFiltered(isNull(), eq(currentUserRoleName), eq(currentUserId));
    verify(getShopMapper, times(1))
        .getShopsFiltered(
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

    when(getShopMapper.countShopsFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    // Mapper returns null instead of empty list
    when(getShopMapper.getShopsFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(null);

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());

    verify(getShopMapper, times(1)).countShopsFiltered(search, currentUserRoleName, currentUserId);
    verify(getShopMapper, times(1))
        .getShopsFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_Success_WhenSortDirectionIsNull_TC005() {
    // Arrange
    validRequest.setSortDirection(null);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();

    when(getShopMapper.countShopsFiltered(any(), any(), any())).thenReturn(1L);

    when(getShopMapper.getShopsFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopResult));

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    verify(getShopMapper, times(1))
        .getShopsFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId));
  }

  @Test
  void process_Success_WhenSortDirectionIsAsc_TC006() {
    // Arrange
    validRequest.setSortDirection(SortDirection.ASC);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();

    when(getShopMapper.countShopsFiltered(any(), any(), any())).thenReturn(1L);

    when(getShopMapper.getShopsFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopResult));

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    verify(getShopMapper, times(1))
        .getShopsFiltered(
            eq(search),
            eq(sortBy),
            eq("ASC"),
            eq(size),
            eq(offset),
            eq(currentUserRoleName),
            eq(currentUserId));
  }

  @Test
  void process_Success_WhenRoleIsManager_TC007() {
    // Arrange
    String managerRole = "MANAGER";
    when(getShopMapper.countShopsFiltered(any(), eq(managerRole), eq(currentUserId)))
        .thenReturn(1L);
    when(getShopMapper.getShopsFiltered(
            any(), any(), any(), anyInt(), anyInt(), eq(managerRole), eq(currentUserId)))
        .thenReturn(Collections.singletonList(sampleShopResult));

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, managerRole, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    verify(getShopMapper, times(1)).countShopsFiltered(any(), eq(managerRole), eq(currentUserId));
  }

  @Test
  void process_Success_WhenPaginationOnSecondPage_TC008() {
    // Arrange
    validRequest.setPage(2);
    validRequest.setSize(5);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = 5;
    int offset = 5; // (2 - 1) * 5
    long totalElements = 12L;

    when(getShopMapper.countShopsFiltered(search, currentUserRoleName, currentUserId))
        .thenReturn(totalElements);

    when(getShopMapper.getShopsFiltered(
            search, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId))
        .thenReturn(Collections.singletonList(sampleShopResult));

    // Act
    PageResponse<ShopResult> response =
        getShopService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals(2, response.getPagination().getPage());
    assertEquals(5, response.getPagination().getSize());
    assertEquals(12L, response.getPagination().getTotalElements());
    assertEquals(3, response.getPagination().getTotalPages()); // ceil(12 / 5) = 3
  }

  // =========================================================================
  // ABNORMAL / DATABASE EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenCountQueryFails_TC009() {
    // Arrange
    when(getShopMapper.countShopsFiltered(any(), any(), any()))
        .thenThrow(new DataAccessException("Database connection timeout") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getShopService.process(validRequest, currentUserRoleName, currentUserId));

    assertEquals("Database connection timeout", exception.getMessage());
    verify(getShopMapper, never())
        .getShopsFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenSelectQueryFails_TC010() {
    // Arrange
    when(getShopMapper.countShopsFiltered(any(), any(), any())).thenReturn(5L);
    when(getShopMapper.getShopsFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any()))
        .thenThrow(new DataAccessException("Syntax error in query execution") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getShopService.process(validRequest, currentUserRoleName, currentUserId));

    assertEquals("Syntax error in query execution", exception.getMessage());
  }

  // =========================================================================
  // DTO HELPER METHODS TESTS (calcOffset, totalPages, trimmedSearch)
  // =========================================================================

  @Test
  void process_DtoHelpers_CalculationAndTrimmingCoverage_TC011() {
    SearchShopRequest req = new SearchShopRequest();
    req.setPage(3);
    req.setSize(15);
    assertEquals(30, req.calcOffset());

    // totalPages cases
    assertEquals(0, req.totalPages(0));
    assertEquals(1, req.totalPages(10));
    assertEquals(1, req.totalPages(15));
    assertEquals(2, req.totalPages(16));

    // trimmedSearch cases
    req.setSearch(null);
    assertNull(req.trimmedSearch());

    req.setSearch("");
    assertNull(req.trimmedSearch());

    req.setSearch("   ");
    assertNull(req.trimmedSearch());

    req.setSearch("  Hưng Lợi  ");
    assertEquals("Hưng Lợi", req.trimmedSearch());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenRequestIsValid_TC012() {
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsNull_TC013() {
    validRequest.setSearch(null);
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsEmptyOrWhitespace_TC014() {
    validRequest.setSearch("   ");
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenPageIsLessThanOne_TC015() {
    validRequest.setPage(0);
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.PAGE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeIsLessThanOne_TC016() {
    validRequest.setSize(0);
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeExceedsMax_TC017() {
    validRequest.setSize(101);
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenSizeIsOne_TC017A() {
    validRequest.setSize(1);

    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationSuccess_WhenSizeIs100_TC017B() {
    validRequest.setSize(100);

    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenSearchExceeds100Characters_TC018() {
    validRequest.setSearch("A".repeat(101));
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenSearchHas100Characters_TC018A() {
    validRequest.setSearch("A".repeat(100));

    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenSearchContainsProhibitedCharacters_TC019() {
    validRequest.setSearch("SELECT * FROM shops; <script>");
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSortByIsInvalidColumn_TC020() {
    validRequest.setSortBy("passwordHash");
    Set<ConstraintViolation<SearchShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SORT_BY_INVALID, violations.iterator().next().getMessage());
  }
}
