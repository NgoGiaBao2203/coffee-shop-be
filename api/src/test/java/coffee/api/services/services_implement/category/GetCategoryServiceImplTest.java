package coffee.api.services.services_implement.category;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.category.SearchCategoriesRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.Roles;
import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetCategoriesMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
public class GetCategoryServiceImplTest {

  @Mock private GetCategoriesMapper getCategoriesMapper;

  @InjectMocks private GetCategoryServiceImpl getCategoryService;

  private Validator validator;
  private SearchCategoriesRequest validRequest;
  private CategoryResult sampleCategoryResult;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    // Context user configurations
    currentUserShopId = UUID.randomUUID();
    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();

    // Initialize standard valid search request
    validRequest = new SearchCategoriesRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setBranchShopId(UUID.randomUUID());
    validRequest.setSearch("Espresso");
    validRequest.setSortBy("categoryName");
    validRequest.setSortDirection(SortDirection.ASC);

    // Build mock single returned item
    sampleCategoryResult = new CategoryResult();
    sampleCategoryResult.setCategoryId(UUID.randomUUID());
    sampleCategoryResult.setShopId(currentUserShopId);
    sampleCategoryResult.setCategoryName("Espresso");
    sampleCategoryResult.setShopName("Hoa Yên Coffee");
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL CASES
  // =========================================================================

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
            search, managerRole, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals("Get categories successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());
    assertEquals("Espresso", response.getItems().getFirst().getCategoryName());
    assertEquals("Hoa Yên Coffee", response.getItems().getFirst().getShopName());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, managerRole, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNull_TC002() {
    // Arrange: Owner does not belong to a specific shop (currentUserShopId is null)
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getCategoriesMapper.countCategoriesFiltered(search, ownerRole, null, branchShopId))
        .thenReturn(totalElements);
    when(getCategoriesMapper.getCategoriesFiltered(
            search, sortBy, sortDirection, size, offset, ownerRole, null, branchShopId))
        .thenReturn(Collections.singletonList(sampleCategoryResult));

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, ownerRole, null);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, ownerRole, null, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search, sortBy, sortDirection, size, offset, ownerRole, null, branchShopId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC003() {
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
            search, managerRole, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, managerRole, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC004() {
    // Arrange
    validRequest.setSearch("   ");
    UUID branchShopId = validRequest.getBranchShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getCategoriesMapper.countCategoriesFiltered(
            null, managerRole, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            null,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId))
        .thenReturn(Collections.singletonList(sampleCategoryResult));

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Espresso", response.getItems().getFirst().getCategoryName());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(null, managerRole, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            null,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWhenMapperReturnsNullList_TC005() {
    // Arrange
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getCategoriesMapper.countCategoriesFiltered(
            search, managerRole, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId))
        .thenReturn(null);

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, managerRole, currentUserShopId, branchShopId);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search,
            sortBy,
            sortDirection,
            size,
            offset,
            managerRole,
            currentUserShopId,
            branchShopId);
  }

  @Test
  void process_SuccessWhenBranchShopIdIsNull_TC006() {
    // Arrange
    validRequest.setBranchShopId(null);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().toString();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 3L;

    when(getCategoriesMapper.countCategoriesFiltered(search, managerRole, currentUserShopId, null))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, null))
        .thenReturn(Collections.singletonList(sampleCategoryResult));

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    assertEquals(3L, response.getPagination().getTotalElements());

    verify(getCategoriesMapper, times(1))
        .countCategoriesFiltered(search, managerRole, currentUserShopId, null);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, null);
  }

  @Test
  void process_SuccessWhenSortDirectionIsNull_DefaultsToAsc_TC007() {
    // Arrange
    validRequest.setSortDirection(null);
    UUID branchShopId = validRequest.getBranchShopId();
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getCategoriesMapper.countCategoriesFiltered(
            search, managerRole, currentUserShopId, branchShopId))
        .thenReturn(totalElements);

    when(getCategoriesMapper.getCategoriesFiltered(
            search, sortBy, "ASC", size, offset, managerRole, currentUserShopId, branchShopId))
        .thenReturn(Collections.singletonList(sampleCategoryResult));

    // Act
    PageResponse<CategoryResult> response =
        getCategoryService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    verify(getCategoriesMapper, times(1))
        .getCategoriesFiltered(
            search, sortBy, "ASC", size, offset, managerRole, currentUserShopId, branchShopId);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdIsNull_TC008() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getCategoryService.process(validRequest, managerRole, null));

    assertEquals("User is not assigned to any shop branch", exception.getMessage());
    verify(getCategoriesMapper, never()).countCategoriesFiltered(any(), any(), any(), any());
    verify(getCategoriesMapper, never())
        .getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIdIsNull_TC009() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getCategoryService.process(validRequest, staffRole, null));

    assertEquals("User is not assigned to any shop branch", exception.getMessage());
    verify(getCategoriesMapper, never()).countCategoriesFiltered(any(), any(), any(), any());
    verify(getCategoriesMapper, never())
        .getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC010() {
    // Arrange
    when(getCategoriesMapper.countCategoriesFiltered(
            any(), eq(managerRole), eq(currentUserShopId), any()))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getCategoryService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database query error", exception.getMessage());
    verify(getCategoriesMapper, never())
        .getCategoriesFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC011() {
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsNull_TC012() {
    validRequest.setSearch(null);
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchContainsVietnameseAndAllowedCharacters_TC013() {
    validRequest.setSearch("Cà-phê Trà 30");
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES (PAGE, SIZE, SEARCH, SORTBY)
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPageIsLessThanOne_TC014() {
    validRequest.setPage(0);
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.PAGE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeIsLessThanOne_TC015() {
    validRequest.setSize(0);
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeExceeds100_TC016() {
    validRequest.setSize(101);
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSearchExceeds100Characters_TC017() {
    validRequest.setSearch("A".repeat(101));
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSearchContainsDisallowedSpecialCharacters_TC018() {
    validRequest.setSearch("Espresso @ 2026!");
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSortByIsInvalid_TC019() {
    validRequest.setSortBy("unsupportedColumn");
    Set<ConstraintViolation<SearchCategoriesRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SORT_BY_INVALID, violations.iterator().next().getMessage());
  }
}
