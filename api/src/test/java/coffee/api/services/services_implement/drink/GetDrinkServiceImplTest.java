package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.SearchDrinksRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.DrinkResult;
import coffee.api.enums.Roles;
import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetDrinksMapper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class GetDrinkServiceImplTest {

  @Mock private GetDrinksMapper getDrinksMapper;

  @InjectMocks private GetDrinkServiceImpl getDrinkService;

  private Validator validator;
  private SearchDrinksRequest validRequest;
  private DrinkResult sampleDrinkResult;
  private UUID currentUserShopId;
  private UUID shopId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    // Context user configurations
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000002");
    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();

    // Initialize standard valid search request
    validRequest = new SearchDrinksRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Cà Phê");
    validRequest.setSortBy("drinkName");
    validRequest.setSortDirection(SortDirection.DESC);
    validRequest.setShopId(shopId);

    // Build mock single returned item
    UUID drinkId = UUID.fromString("d1111111-1111-1111-1111-111111111111");
    UUID categoryId = UUID.fromString("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d");

    sampleDrinkResult = new DrinkResult();
    sampleDrinkResult.setDrinkId(drinkId);
    sampleDrinkResult.setShopId(shopId);
    sampleDrinkResult.setDrinkCategoryId(categoryId);
    sampleDrinkResult.setCategoryName("Cà Phê Truyền Thống");
    sampleDrinkResult.setDrinkName("Cà Phê Sữa Đá");
    sampleDrinkResult.setImageUrl("https://example.com/images/cf-sua-da.jpg");
    sampleDrinkResult.setStatus("ACTIVE");
    sampleDrinkResult.setIsDeleted(false);
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL CASES
  // =========================================================================

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<DrinkResult> expectedItems = Collections.singletonList(sampleDrinkResult);

    when(getDrinksMapper.countDrinksFiltered(search, managerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId))
        .thenReturn(expectedItems);

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals("Get drinks successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());

    DrinkResult drinkResult = response.getItems().getFirst();
    assertEquals("Cà Phê Sữa Đá", drinkResult.getDrinkName());
    assertEquals("Đang bán", drinkResult.getStatus());
    assertEquals(shopId, drinkResult.getShopId());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, managerRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNull_TC002() {
    // Arrange: Owner does not belong to a specific shop (currentUserShopId is null)
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, ownerRole, null, shopId))
        .thenReturn(totalElements);
    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, ownerRole, null, shopId))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response = getDrinkService.process(validRequest, ownerRole, null);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());

    verify(getDrinksMapper, times(1)).countDrinksFiltered(search, ownerRole, null, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(search, sortBy, sortDirection, size, offset, ownerRole, null, shopId);
  }

  @Test
  void process_Success_WhenUserIsOwnerAndShopIdIsNotNull_TC003() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, ownerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);
    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, ownerRole, currentUserShopId, shopId))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, ownerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, ownerRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, ownerRole, currentUserShopId, shopId);
  }

  @Test
  void process_Success_WhenUserIsStaff_TC004() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, staffRole, currentUserShopId, shopId))
        .thenReturn(totalElements);
    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, staffRole, currentUserShopId, shopId))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, staffRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, staffRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, staffRole, currentUserShopId, shopId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC005() {
    // Arrange
    validRequest.setSearch("NonExistentDrink");
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getDrinksMapper.countDrinksFiltered(search, managerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId))
        .thenReturn(Collections.emptyList());

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, managerRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC006() {
    // Arrange
    validRequest.setSearch("   ");
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getDrinksMapper.countDrinksFiltered(null, managerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            null, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("Cà Phê Sữa Đá", response.getItems().getFirst().getDrinkName());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(null, managerRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            null, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId);
  }

  @Test
  void process_SuccessWhenMapperReturnsNullList_TC007() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, managerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId))
        .thenReturn(null);

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertNotNull(response.getItems());
    assertTrue(response.getItems().isEmpty());
    assertEquals(1L, response.getPagination().getTotalElements());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, managerRole, currentUserShopId, shopId);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, shopId);
  }

  @Test
  void process_SuccessWhenShopIdIsNull_TC008() {
    // Arrange
    validRequest.setShopId(null);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.getSortDirection().name();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 3L;

    when(getDrinksMapper.countDrinksFiltered(search, managerRole, currentUserShopId, null))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, null))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    assertEquals(1, response.getItems().size());
    assertEquals(3L, response.getPagination().getTotalElements());

    verify(getDrinksMapper, times(1))
        .countDrinksFiltered(search, managerRole, currentUserShopId, null);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, sortDirection, size, offset, managerRole, currentUserShopId, null);
  }

  @Test
  void process_SuccessWhenSortDirectionIsNull_DefaultsToAsc_TC009() {
    // Arrange
    validRequest.setSortDirection(null);
    String search = validRequest.trimmedSearch();
    String sortBy = validRequest.getSortBy();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    when(getDrinksMapper.countDrinksFiltered(search, managerRole, currentUserShopId, shopId))
        .thenReturn(totalElements);

    when(getDrinksMapper.getDrinksFiltered(
            search, sortBy, "ASC", size, offset, managerRole, currentUserShopId, shopId))
        .thenReturn(Collections.singletonList(sampleDrinkResult));

    // Act
    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    // Assert
    assertNotNull(response);
    verify(getDrinksMapper, times(1))
        .getDrinksFiltered(
            search, sortBy, "ASC", size, offset, managerRole, currentUserShopId, shopId);
  }

  @Test
  void process_NormalizeStatus_CoverAllBranchesIndividually_TC010() {
    DrinkResult item1 = new DrinkResult();
    item1.setStatus("1");

    DrinkResult item2 = new DrinkResult();
    item2.setStatus("ACTIVE");

    DrinkResult item3 = new DrinkResult();
    item3.setStatus("0");

    DrinkResult item4 = new DrinkResult();
    item4.setStatus("INACTIVE");

    DrinkResult item5 = new DrinkResult();
    item5.setStatus(null);

    DrinkResult item6 = new DrinkResult();
    item6.setStatus("OTHER");

    DrinkResult item7 = new DrinkResult();
    item7.setStatus("");

    when(getDrinksMapper.countDrinksFiltered(any(), any(), any(), any())).thenReturn(7L);
    when(getDrinksMapper.getDrinksFiltered(
            any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
        .thenReturn(List.of(item1, item2, item3, item4, item5, item6, item7));

    PageResponse<DrinkResult> response =
        getDrinkService.process(validRequest, managerRole, currentUserShopId);

    assertEquals("Đang bán", response.getItems().get(0).getStatus());
    assertEquals("Đang bán", response.getItems().get(1).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(2).getStatus());
    assertEquals("Ngừng bán", response.getItems().get(3).getStatus());
    assertEquals("UNKNOWN", response.getItems().get(4).getStatus());
    assertEquals("Không xác định", response.getItems().get(5).getStatus());
    assertEquals("Không xác định", response.getItems().get(6).getStatus());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdIsNull_TC011() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getDrinkService.process(validRequest, managerRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());
    verify(getDrinksMapper, never()).countDrinksFiltered(any(), any(), any(), any());
    verify(getDrinksMapper, never())
        .getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffShopIdIsNull_TC012() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> getDrinkService.process(validRequest, staffRole, null));

    assertEquals("User is not assigned to any shop", exception.getMessage());
    verify(getDrinksMapper, never()).countDrinksFiltered(any(), any(), any(), any());
    verify(getDrinksMapper, never())
        .getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC013() {
    // Arrange
    when(getDrinksMapper.countDrinksFiltered(any(), eq(managerRole), eq(currentUserShopId), any()))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getDrinkService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database query error", exception.getMessage());
    verify(getDrinksMapper, never())
        .getDrinksFiltered(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenFetchingDrinksFails_TC014() {
    // Arrange
    when(getDrinksMapper.countDrinksFiltered(any(), eq(managerRole), eq(currentUserShopId), any()))
        .thenReturn(1L);
    when(getDrinksMapper.getDrinksFiltered(
            any(), any(), any(), anyInt(), anyInt(), eq(managerRole), eq(currentUserShopId), any()))
        .thenThrow(new DataAccessException("Database query error") {});

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> getDrinkService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Database query error", exception.getMessage());
    verify(getDrinksMapper)
        .countDrinksFiltered(any(), eq(managerRole), eq(currentUserShopId), any());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC015() {
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsNull_TC016() {
    validRequest.setSearch(null);
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchContainsVietnameseAndAllowedCharacters_TC017() {
    // Allows: Unicode letters, numbers, spaces, and punctuation: - & / ( ) , . '
    validRequest.setSearch("Cà-phê & Trà (Nóng/Lạnh), Số 1. 'Espresso'");
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"drinkId", "shopId", "drinkCategoryId", "drinkName", "status", "isDeleted"})
  void process_ValidationSuccess_WhenSortByContainsValidPatternValues_TC018(String validSortBy) {
    validRequest.setSortBy(validSortBy);
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenSearchIsEmptyOrOnlyWhitespaces_TC019() {
    validRequest.setSearch("     ");
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());

    validRequest.setSearch("");
    Set<ConstraintViolation<SearchDrinksRequest>> emptyViolations =
        validator.validate(validRequest);
    assertEquals(0, emptyViolations.size());
  }

  @Test
  void request_DefaultValuesAndGettersSetters_TC020() {
    SearchDrinksRequest defaultReq = new SearchDrinksRequest();
    assertEquals(1, defaultReq.getPage());
    assertEquals(10, defaultReq.getSize());
    assertNull(defaultReq.getShopId());
    assertNull(defaultReq.getSearch());
    assertEquals("drinkId", defaultReq.getSortBy());
    assertEquals(SortDirection.ASC, defaultReq.getSortDirection());

    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(defaultReq);
    assertTrue(violations.isEmpty());
  }

  @Test
  void request_TransformsSearchAndCalculatesPagination_TC021() {
    validRequest.setSearch("  Cà Phê  ");
    validRequest.setPage(3);
    validRequest.setSize(25);

    assertEquals("Cà Phê", validRequest.trimmedSearch());
    assertEquals(50, validRequest.calcOffset());
    assertEquals(3, validRequest.totalPages(60));
    assertEquals(0, validRequest.totalPages(0));
    assertEquals(1, validRequest.totalPages(1));

    validRequest.setSearch(null);
    assertNull(validRequest.trimmedSearch());

    validRequest.setSearch("    ");
    assertNull(validRequest.trimmedSearch());

    validRequest.setSearch("");
    assertNull(validRequest.trimmedSearch());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES (PAGE, SIZE, SEARCH, SORTBY)
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPageIsLessThanOne_TC022() {
    validRequest.setPage(0);
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.PAGE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeIsLessThanOne_TC023() {
    validRequest.setSize(0);
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MIN, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSizeExceeds100_TC024() {
    validRequest.setSize(101);
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSearchExceeds100Characters_TC025() {
    validRequest.setSearch("A".repeat(101));
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSearchContainsDisallowedSpecialCharacters_TC026() {
    validRequest.setSearch("Drink @ 2026! #$%*");
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenSortByIsInvalid_TC027() {
    validRequest.setSortBy("unsupportedColumn");
    Set<ConstraintViolation<SearchDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SORT_BY_INVALID, violations.iterator().next().getMessage());
  }
}
