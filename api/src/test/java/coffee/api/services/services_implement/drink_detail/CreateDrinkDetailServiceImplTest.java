package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class CreateDrinkDetailServiceImplTest {
  private static final UUID DRINK_ID = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
  private static final UUID DETAIL_ID = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");
  private static final UUID USER_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
  private static final UUID SHOP_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

  @Mock private CreateDrinkDetailMapper createMapper;
  @Mock private GetDrinkDetailMapper getMapper;
  @Mock private CommonMapper commonMapper;
  private CreateDrinkDetailServiceImpl service;
  private CreateDrinkDetailRequest request;
  private static ValidatorFactory validatorFactory;
  private static Validator requestValidator;

  @BeforeAll
  static void initValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    requestValidator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @BeforeEach
  void setUp() {
    service =
        new CreateDrinkDetailServiceImpl(
            createMapper, new DrinkDetailValidator(createMapper, getMapper, commonMapper));
    request = new CreateDrinkDetailRequest();
    request.setDrinkId(DRINK_ID);
    request.setSize("M");
    request.setPrice(new BigDecimal("30000.00"));
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_CreatesDetailForAccessibleDrink_TC001(String role) {
    stubAccessibleDrink(role, SHOP_ID);
    when(createMapper.insertDrinkDetail(
            any(UUID.class), eq(DRINK_ID), eq("M"), eq(request.getPrice())))
        .thenReturn(1);

    service.process(request, USER_ID, role, SHOP_ID);

    var parent = org.mockito.ArgumentCaptor.forClass(UUID.class);
    var size = org.mockito.ArgumentCaptor.forClass(String.class);
    var price = org.mockito.ArgumentCaptor.forClass(BigDecimal.class);
    verify(createMapper)
        .insertDrinkDetail(any(UUID.class), parent.capture(), size.capture(), price.capture());
    assertEquals(DRINK_ID, parent.getValue());
    assertEquals("M", size.getValue());
    assertEquals(new BigDecimal("30000.00"), price.getValue());
    verify(commonMapper).checkDrinkDetailSizeExisted(DRINK_ID, "M", null);
  }

  @Test
  void process_AllowsOwnerWithoutAssignedShop_TC002() {
    stubAccessibleDrink("OWNER", null);
    when(createMapper.insertDrinkDetail(
            any(UUID.class), eq(DRINK_ID), eq("M"), eq(request.getPrice())))
        .thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createMapper)
        .insertDrinkDetail(any(UUID.class), eq(DRINK_ID), eq("M"), eq(request.getPrice()));
  }

  @ParameterizedTest
  @CsvSource({"s, S", "' m ', M", "l, L"})
  void process_NormalizesSizeBeforeDuplicateCheckAndInsert_TC003(
      String input, String expectedSize) {
    request.setSize(input);
    stubAccessibleDrink("OWNER", null);
    when(createMapper.insertDrinkDetail(
            any(UUID.class), eq(DRINK_ID), eq(expectedSize), eq(request.getPrice())))
        .thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    var order = inOrder(createMapper, commonMapper);
    order.verify(createMapper).checkDrinkAccessible(DRINK_ID, "OWNER", null);
    order.verify(commonMapper).checkDrinkDetailSizeExisted(DRINK_ID, expectedSize, null);
    order
        .verify(createMapper)
        .insertDrinkDetail(any(UUID.class), eq(DRINK_ID), eq(expectedSize), eq(request.getPrice()));
    assertEquals(input, request.getSize());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.01", "9999999999999999.99"})
  void process_PreservesPriceAtAllowedBoundaries_TC004(String value) {
    BigDecimal price = new BigDecimal(value);
    request.setPrice(price);
    stubAccessibleDrink("OWNER", null);
    when(createMapper.insertDrinkDetail(any(UUID.class), eq(DRINK_ID), eq("M"), eq(price)))
        .thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    verify(createMapper).insertDrinkDetail(any(UUID.class), eq(DRINK_ID), eq("M"), eq(price));
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / REQUEST & PERMISSION CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void process_RejectsMissingRequestOrDrinkId_TC005(boolean missingRequest) {
    request.setDrinkId(null);
    CreateDrinkDetailRequest input = missingRequest ? null : request;

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(input, USER_ID, "OWNER", null));

    assertEquals("Drink ID is required", error.getMessage());
    verifyNoInteractions(createMapper, getMapper, commonMapper);
  }

  @Test
  void process_RejectsManagerWithoutAssignedShop_TC006() {
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", null));

    assertEquals("User is not assigned to any shop", error.getMessage());
    verifyNoInteractions(createMapper, getMapper, commonMapper);
  }

  @Test
  void process_RejectsManagerWithoutActiveMembership_TC007() {
    when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenReturn(false);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", SHOP_ID));

    assertEquals("You do not have permission to access this shop", error.getMessage());
    verifyNoInteractions(createMapper, getMapper);
  }

  @Test
  void process_RejectsInaccessibleOrDeletedDrink_TC008() {
    when(createMapper.checkDrinkAccessible(DRINK_ID, "OWNER", null)).thenReturn(false);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink not found", error.getMessage());
    assertEquals(DRINK_ID, error.getId());
    verifyNoInteractions(commonMapper);
    verifyNoInsert();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "-0.01"})
  void process_RejectsMissingOrNonPositivePrice_TC009(String value) {
    request.setPrice(value == null ? null : new BigDecimal(value));
    stubAccessibleDrink("OWNER", null);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail price must be greater than zero", error.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInsert();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "XL", "XXL", "XXXL"})
  void process_RejectsUnsupportedSize_TC010(String size) {
    request.setSize(size);
    stubAccessibleDrink("OWNER", null);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail size is invalid", error.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInsert();
  }

  @Test
  void process_RejectsDuplicateActiveSize_TC011() {
    stubAccessibleDrink("OWNER", null);
    when(commonMapper.checkDrinkDetailSizeExisted(DRINK_ID, "M", null)).thenReturn(true);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail size already exists", error.getMessage());
    verifyNoInsert();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2, -1})
  void process_RejectsUnexpectedInsertCount_TC012(int rows) {
    stubAccessibleDrink("OWNER", null);
    when(createMapper.insertDrinkDetail(
            any(UUID.class), eq(DRINK_ID), eq("M"), eq(request.getPrice())))
        .thenReturn(rows);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Unable to create drink detail", error.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @ParameterizedTest
  @CsvSource({"0, 0", "1, 0", "2, 0", "3, 1"})
  void process_PropagatesFailureAtEachDatabaseStage_TC013(int stage, long expectedInsertCalls) {
    String role = stage == 0 ? "MANAGER" : "OWNER";
    DataAccessException failure = new DataAccessException("Database failure") {};
    if (stage > 1)
      when(createMapper.checkDrinkAccessible(DRINK_ID, role, SHOP_ID)).thenReturn(true);
    switch (stage) {
      case 0 -> when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenThrow(failure);
      case 1 -> when(createMapper.checkDrinkAccessible(DRINK_ID, role, SHOP_ID)).thenThrow(failure);
      case 2 ->
          when(commonMapper.checkDrinkDetailSizeExisted(DRINK_ID, "M", null)).thenThrow(failure);
      case 3 -> when(createMapper.insertDrinkDetail(any(), any(), any(), any())).thenThrow(failure);
      default -> throw new IllegalArgumentException("Unexpected database stage");
    }

    var error =
        assertThrows(
            DataAccessException.class, () -> service.process(request, USER_ID, role, SHOP_ID));

    assertEquals("Database failure", error.getMessage());
    assertEquals(
        expectedInsertCalls,
        mockingDetails(createMapper).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("insertDrinkDetail"))
            .count());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"S", "M", "L"})
  void requestValidation_AcceptsEverySupportedSize_TC014(String size) {
    request.setSize(size);

    assertViolations(request, Map.of());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.01", "1.00", "30000.50", "9999999999999999.99"})
  void requestValidation_AcceptsPositivePricesAtAllowedPrecision_TC015(String price) {
    request.setPrice(new BigDecimal(price));

    assertViolations(request, Map.of());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_RejectsMissingId_TC016() {
    request.setDrinkId(null);

    assertViolations(request, Map.of("drinkId", Set.of(ValidationMessage.Msg.FIELD_REQUIRED)));
  }

  @ParameterizedTest
  @MethodSource("invalidSizeCases")
  void requestValidation_RejectsInvalidSizes_TC017(String size, Set<String> messages) {
    request.setSize(size);

    assertViolations(request, Map.of("size", messages));
  }

  @ParameterizedTest
  @MethodSource("invalidPriceCases")
  void requestValidation_RejectsInvalidPrices_TC018(BigDecimal price, String message) {
    request.setPrice(price);

    assertViolations(request, Map.of("price", Set.of(message)));
  }

  @Test
  void requestValidation_ReportsEveryMissingField_TC019() {
    assertViolations(
        new CreateDrinkDetailRequest(),
        Map.of(
            "drinkId", Set.of(ValidationMessage.Msg.FIELD_REQUIRED),
            "size", Set.of(ValidationMessage.Msg.FIELD_REQUIRED),
            "price", Set.of(ValidationMessage.Msg.FIELD_REQUIRED)));
  }

  // =========================================================================
  // HELPER STUB, VALIDATION & TEST DATA METHODS
  // =========================================================================

  private void stubAccessibleDrink(String role, UUID assignedShop) {
    if ("MANAGER".equals(role))
      when(commonMapper.checkShopIdIsExisted(USER_ID, assignedShop)).thenReturn(true);
    when(createMapper.checkDrinkAccessible(DRINK_ID, role, assignedShop)).thenReturn(true);
  }

  private void verifyNoInsert() {
    verify(createMapper, never()).insertDrinkDetail(any(), any(), any(), any());
  }

  private void assertViolations(Object request, Map<String, Set<String>> expected) {
    var violations = requestValidator.validate(request);
    assertEquals(expected.values().stream().mapToInt(Set::size).sum(), violations.size());
    assertEquals(
        expected,
        violations.stream()
            .collect(
                Collectors.groupingBy(
                    violation -> violation.getPropertyPath().toString(),
                    Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet()))));
  }

  static Stream<Arguments> invalidSizeCases() {
    return Stream.of(
        Arguments.of((String) null, Set.of(ValidationMessage.Msg.FIELD_REQUIRED)),
        Arguments.of(
            "",
            Set.of(ValidationMessage.Msg.FIELD_REQUIRED, ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of(
            "   ",
            Set.of(ValidationMessage.Msg.FIELD_REQUIRED, ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of("m", Set.of(ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of(" M ", Set.of(ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of("XL", Set.of(ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of("XXL", Set.of(ValidationMessage.Msg.SPECIAL_CHARACTERS)),
        Arguments.of("XXXL", Set.of(ValidationMessage.Msg.SPECIAL_CHARACTERS)));
  }

  static Stream<Arguments> invalidPriceCases() {
    return Stream.of(
        Arguments.of((BigDecimal) null, ValidationMessage.Msg.FIELD_REQUIRED),
        Arguments.of(BigDecimal.ZERO, ValidationMessage.Msg.PRICE_MIN),
        Arguments.of(new BigDecimal("-0.01"), ValidationMessage.Msg.PRICE_MIN),
        Arguments.of(new BigDecimal("30000.123"), ValidationMessage.Msg.SIZE_MAX),
        Arguments.of(new BigDecimal("12345678901234567"), ValidationMessage.Msg.SIZE_MAX));
  }
}
