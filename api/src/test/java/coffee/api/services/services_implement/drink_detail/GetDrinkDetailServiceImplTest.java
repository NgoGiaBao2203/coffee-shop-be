package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.result.DrinkResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class GetDrinkDetailServiceImplTest {
  private static final UUID DRINK_ID = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
  private static final UUID DETAIL_ID = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");
  private static final UUID USER_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
  private static final UUID SHOP_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

  @Mock private CreateDrinkDetailMapper createMapper;
  @Mock private GetDrinkDetailMapper getMapper;
  @Mock private CommonMapper commonMapper;
  private GetDrinkDetailServiceImpl service;
  private DrinkResult drink;

  @BeforeEach
  void setUp() {
    service =
        new GetDrinkDetailServiceImpl(
            getMapper, new DrinkDetailValidator(createMapper, getMapper, commonMapper));
    drink = new DrinkResult();
    drink.setDrinkId(DRINK_ID);
    drink.setShopId(SHOP_ID);
    drink.setDrinkName("Cà Phê Sữa Đá");
    drink.setCategoryName("Cà phê");
    drink.setImageUrl("/images/coffee.png");
    drink.setIsDeleted(false);
    drink.setStatus("1");
    drink.setVariants(List.of(variant(DETAIL_ID, "M", "30000.00")));
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_ReturnsAccessibleDrinkAndPreservesVariantData_TC001(String role) {
    if ("MANAGER".equals(role))
      when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenReturn(true);
    when(getMapper.getDrinkDetailById(DRINK_ID, SHOP_ID, role)).thenReturn(drink);

    DrinkResult result = service.process(DRINK_ID, USER_ID, role, SHOP_ID);

    assertEquals(DRINK_ID, result.getDrinkId());
    assertEquals(SHOP_ID, result.getShopId());
    assertEquals("Cà Phê Sữa Đá", result.getDrinkName());
    assertEquals("Cà phê", result.getCategoryName());
    assertEquals("/images/coffee.png", result.getImageUrl());
    assertEquals(Boolean.FALSE, result.getIsDeleted());
    assertEquals("Đang bán", result.getStatus());
    assertEquals(1, result.getVariants().size());
    assertEquals(DETAIL_ID, result.getVariants().getFirst().getDrinkDetailId());
    assertEquals("M", result.getVariants().getFirst().getSize());
    assertEquals(new BigDecimal("30000.00"), result.getVariants().getFirst().getPrice());
    verify(getMapper).getDrinkDetailById(DRINK_ID, SHOP_ID, role);
  }

  @Test
  void process_AllowsOwnerWithoutAssignedShop_TC002() {
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(drink);

    var result = service.process(DRINK_ID, USER_ID, "OWNER", null);

    assertEquals(DRINK_ID, result.getDrinkId());
    assertEquals("Đang bán", result.getStatus());
    verifyNoInteractions(commonMapper);
  }

  @ParameterizedTest
  @MethodSource("statusCases")
  void process_NormalizesAllSupportedAndUnknownStatuses_TC003(
      String storedStatus, String expectedStatus) {
    drink.setStatus(storedStatus);
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(drink);

    var result = service.process(DRINK_ID, USER_ID, "OWNER", null);

    assertEquals(expectedStatus, result.getStatus());
  }

  @Test
  void process_PreservesNullVariantList_TC004() {
    drink.setVariants(null);
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(drink);

    var result = service.process(DRINK_ID, USER_ID, "OWNER", null);

    assertNull(result.getVariants());
  }

  @Test
  void process_PreservesEmptyVariantList_TC005() {
    drink.setVariants(List.of());
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(drink);

    var result = service.process(DRINK_ID, USER_ID, "OWNER", null);

    assertEquals(List.of(), result.getVariants());
  }

  @Test
  void process_PreservesMultipleVariantIdsSizesAndPrices_TC006() {
    UUID secondId = UUID.fromString("b8861994-e947-425c-a6bb-686f28d7ba64");
    drink.setVariants(
        List.of(variant(DETAIL_ID, "M", "30000.00"), variant(secondId, "L", "35000.00")));
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(drink);

    var result = service.process(DRINK_ID, USER_ID, "OWNER", null);

    assertEquals(
        List.of(DETAIL_ID, secondId),
        result.getVariants().stream()
            .map(DrinkResult.DrinkVariantResult::getDrinkDetailId)
            .toList());
    assertEquals(
        List.of("M", "L"),
        result.getVariants().stream().map(DrinkResult.DrinkVariantResult::getSize).toList());
    assertEquals(
        List.of(new BigDecimal("30000.00"), new BigDecimal("35000.00")),
        result.getVariants().stream().map(DrinkResult.DrinkVariantResult::getPrice).toList());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / REQUEST & PERMISSION CASES
  // =========================================================================

  @Test
  void process_RejectsMissingDrinkIdBeforeQueryingDatabase_TC007() {
    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(null, USER_ID, "OWNER", null));

    assertEquals("Drink ID is required", error.getMessage());
    verifyNoInteractions(commonMapper, getMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_RejectsNonOwnerWithoutAssignedShop_TC008(String role) {
    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(DRINK_ID, USER_ID, role, null));

    assertEquals("User is not assigned to any shop", error.getMessage());
    verifyNoInteractions(commonMapper, getMapper);
  }

  @Test
  void process_RejectsManagerWithoutActiveMembership_TC009() {
    when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenReturn(false);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(DRINK_ID, USER_ID, "MANAGER", SHOP_ID));

    assertEquals("You do not have permission to access this shop", error.getMessage());
    verifyNoInteractions(getMapper);
  }

  @Test
  void process_RejectsMissingOrInaccessibleDrink_TC010() {
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER")).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(DRINK_ID, USER_ID, "OWNER", null));

    assertEquals("Drink not found with id: " + DRINK_ID, error.getMessage());
    assertEquals(DRINK_ID, error.getId());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_PropagatesMembershipQueryFailureBeforeFetchingDrink_TC011() {
    when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID))
        .thenThrow(new DataAccessException("Membership query failed") {});

    var error =
        assertThrows(
            DataAccessException.class,
            () -> service.process(DRINK_ID, USER_ID, "MANAGER", SHOP_ID));

    assertEquals("Membership query failed", error.getMessage());
    verifyNoInteractions(getMapper);
  }

  @Test
  void process_PropagatesDrinkQueryFailure_TC012() {
    when(getMapper.getDrinkDetailById(DRINK_ID, null, "OWNER"))
        .thenThrow(new DataAccessException("Drink query failed") {});

    var error =
        assertThrows(
            DataAccessException.class, () -> service.process(DRINK_ID, USER_ID, "OWNER", null));

    assertEquals("Drink query failed", error.getMessage());
  }

  // =========================================================================
  // HELPER STUB & TEST DATA METHODS
  // =========================================================================

  private DrinkResult.DrinkVariantResult variant(UUID id, String size, String price) {
    var variant = new DrinkResult.DrinkVariantResult();
    variant.setDrinkDetailId(id);
    variant.setSize(size);
    variant.setPrice(new BigDecimal(price));
    return variant;
  }

  static Stream<Arguments> statusCases() {
    return Stream.of(
        Arguments.of("1", "Đang bán"),
        Arguments.of("ACTIVE", "Đang bán"),
        Arguments.of("0", "Ngừng bán"),
        Arguments.of("INACTIVE", "Ngừng bán"),
        Arguments.of((String) null, "UNKNOWN"),
        Arguments.of("OTHER", "Không xác định"),
        Arguments.of("", "Không xác định"));
  }
}
