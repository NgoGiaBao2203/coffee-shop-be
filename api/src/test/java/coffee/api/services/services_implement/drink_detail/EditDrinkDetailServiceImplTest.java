package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.mapper.UpdateDrinkDetailMapper;
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
class EditDrinkDetailServiceImplTest {
  private static final UUID DRINK_ID = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
  private static final UUID DETAIL_ID = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");
  private static final UUID USER_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
  private static final UUID SHOP_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

  @Mock private CreateDrinkDetailMapper createMapper;
  @Mock private GetDrinkDetailMapper getMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private UpdateDrinkDetailMapper updateMapper;
  private EditDrinkDetailServiceImpl service;
  private EditDrinkDetailRequest request;
  private DrinkDetailResult existing;
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
        new EditDrinkDetailServiceImpl(
            updateMapper, new DrinkDetailValidator(createMapper, getMapper, commonMapper));
    request = new EditDrinkDetailRequest();
    request.setDrinkDetailId(DETAIL_ID);
    request.setSize("M");
    request.setPrice(new BigDecimal("35000.00"));
    existing = new DrinkDetailResult();
    existing.setDrinkDetailId(DETAIL_ID);
    existing.setDrinkId(DRINK_ID);
    existing.setSize("S");
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_UpdatesAccessibleDetailAndExcludesItFromDuplicateCheck_TC001(String role) {
    stubAccessibleDetail(role, SHOP_ID);
    when(createMapper.checkDrinkAccessible(DRINK_ID, role, SHOP_ID)).thenReturn(true);
    when(updateMapper.updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), role, SHOP_ID))
        .thenReturn(1);

    service.process(request, USER_ID, role, SHOP_ID);

    var order = inOrder(getMapper, createMapper, commonMapper, updateMapper);
    order.verify(getMapper).getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role);
    order.verify(createMapper).checkDrinkAccessible(DRINK_ID, role, SHOP_ID);
    order.verify(commonMapper).checkDrinkDetailSizeExisted(DRINK_ID, "M", DETAIL_ID);
    order.verify(updateMapper).updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), role, SHOP_ID);
    assertEquals("S", existing.getSize());
  }

  @Test
  void process_AllowsOwnerWithoutAssignedShop_TC002() {
    stubEditableDetail();
    when(updateMapper.updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), "OWNER", null))
        .thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(updateMapper).updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), "OWNER", null);
  }

  @ParameterizedTest
  @CsvSource({"s, S", "' m ', M", "l, L"})
  void process_NormalizesSizeBeforeSaving_TC003(String input, String expectedSize) {
    request.setSize(input);
    stubEditableDetail();
    when(updateMapper.updateDrinkDetail(DETAIL_ID, expectedSize, request.getPrice(), "OWNER", null))
        .thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    verify(commonMapper).checkDrinkDetailSizeExisted(DRINK_ID, expectedSize, DETAIL_ID);
    verify(updateMapper)
        .updateDrinkDetail(DETAIL_ID, expectedSize, request.getPrice(), "OWNER", null);
    assertEquals(input, request.getSize());
  }

  @Test
  void process_CurrentBehaviorAcceptsUpdateCountGreaterThanOne_TC004() {
    stubEditableDetail();
    when(updateMapper.updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), "OWNER", null))
        .thenReturn(2);

    service.process(request, USER_ID, "OWNER", null);

    verify(updateMapper).updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), "OWNER", null);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / REQUEST & PERMISSION CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void process_RejectsMissingRequestOrDetailId_TC005(boolean missingRequest) {
    request.setDrinkDetailId(null);
    EditDrinkDetailRequest input = missingRequest ? null : request;

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(input, USER_ID, "OWNER", null));

    assertEquals("Drink detail ID is required", error.getMessage());
    verifyNoInteractions(createMapper, getMapper, commonMapper, updateMapper);
  }

  @Test
  void process_RejectsManagerWithoutShop_TC006() {
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", null));

    assertEquals("User is not assigned to any shop", error.getMessage());
    verifyNoInteractions(createMapper, getMapper, commonMapper, updateMapper);
  }

  @Test
  void process_RejectsInactiveMembership_TC007() {
    when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenReturn(false);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", SHOP_ID));

    assertEquals("You do not have permission to access this shop", error.getMessage());
    verifyNoInteractions(createMapper, getMapper, updateMapper);
  }

  @Test
  void process_RejectsMissingOrInaccessibleDetail_TC008() {
    when(getMapper.getDrinkDetailVariantById(DETAIL_ID, null, "OWNER")).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail not found", error.getMessage());
    assertEquals(DETAIL_ID, error.getId());
    verifyNoInteractions(createMapper, updateMapper, commonMapper);
  }

  @Test
  void process_RejectsInaccessibleParentDrink_TC009() {
    stubAccessibleDetail("OWNER", null);
    when(createMapper.checkDrinkAccessible(DRINK_ID, "OWNER", null)).thenReturn(false);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink not found", error.getMessage());
    assertEquals(DRINK_ID, error.getId());
    verifyNoInteractions(updateMapper, commonMapper);
  }

  @Test
  void process_RejectsDetailWithoutParentDrinkId_TC010() {
    existing.setDrinkId(null);
    stubAccessibleDetail("OWNER", null);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink ID is required", error.getMessage());
    verifyNoInteractions(createMapper, updateMapper, commonMapper);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"0", "-0.01"})
  void process_RejectsMissingOrNonPositivePrice_TC011(String value) {
    request.setPrice(value == null ? null : new BigDecimal(value));
    stubEditableDetail();

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail price must be greater than zero", error.getMessage());
    verifyNoInteractions(updateMapper, commonMapper);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   ", "XL", "XXL", "XXXL"})
  void process_RejectsUnsupportedSize_TC012(String size) {
    request.setSize(size);
    stubEditableDetail();

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail size is invalid", error.getMessage());
    verifyNoInteractions(updateMapper, commonMapper);
  }

  @Test
  void process_RejectsSizeOwnedByAnotherActiveDetail_TC013() {
    stubEditableDetail();
    when(commonMapper.checkDrinkDetailSizeExisted(DRINK_ID, "M", DETAIL_ID)).thenReturn(true);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail size already exists", error.getMessage());
    verifyNoInteractions(updateMapper);
  }

  @Test
  void process_RejectsConcurrentDeletionOrModification_TC014() {
    stubEditableDetail();
    when(updateMapper.updateDrinkDetail(DETAIL_ID, "M", request.getPrice(), "OWNER", null))
        .thenReturn(0);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail has already been deleted or modified", error.getMessage());
    assertEquals(DETAIL_ID, error.getId());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @ParameterizedTest
  @CsvSource({"0, 0", "1, 0", "2, 0", "3, 0", "4, 1"})
  void process_PropagatesFailureAtEachDatabaseStage_TC015(int stage, long expectedUpdateCalls) {
    String role = stage == 0 ? "MANAGER" : "OWNER";
    DataAccessException failure = new DataAccessException("Database failure") {};
    if (stage > 1)
      when(getMapper.getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role)).thenReturn(existing);
    if (stage > 2)
      when(createMapper.checkDrinkAccessible(DRINK_ID, role, SHOP_ID)).thenReturn(true);
    switch (stage) {
      case 0 -> when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenThrow(failure);
      case 1 ->
          when(getMapper.getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role)).thenThrow(failure);
      case 2 -> when(createMapper.checkDrinkAccessible(DRINK_ID, role, SHOP_ID)).thenThrow(failure);
      case 3 ->
          when(commonMapper.checkDrinkDetailSizeExisted(DRINK_ID, "M", DETAIL_ID))
              .thenThrow(failure);
      case 4 ->
          when(updateMapper.updateDrinkDetail(any(), any(), any(), any(), any()))
              .thenThrow(failure);
      default -> throw new IllegalArgumentException("Unexpected database stage");
    }

    var error =
        assertThrows(
            DataAccessException.class, () -> service.process(request, USER_ID, role, SHOP_ID));

    assertEquals("Database failure", error.getMessage());
    assertEquals(expectedUpdateCalls, mockingDetails(updateMapper).getInvocations().size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"S", "M", "L"})
  void requestValidation_AcceptsEverySupportedSize_TC016(String size) {
    request.setSize(size);

    assertViolations(request, Map.of());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.01", "1.00", "30000.50", "9999999999999999.99"})
  void requestValidation_AcceptsPositivePricesAtAllowedPrecision_TC017(String price) {
    request.setPrice(new BigDecimal(price));

    assertViolations(request, Map.of());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_RejectsMissingId_TC018() {
    request.setDrinkDetailId(null);

    assertViolations(
        request, Map.of("drinkDetailId", Set.of(ValidationMessage.Msg.FIELD_REQUIRED)));
  }

  @ParameterizedTest
  @MethodSource("invalidSizeCases")
  void requestValidation_RejectsInvalidSizes_TC019(String size, Set<String> messages) {
    request.setSize(size);

    assertViolations(request, Map.of("size", messages));
  }

  @ParameterizedTest
  @MethodSource("invalidPriceCases")
  void requestValidation_RejectsInvalidPrices_TC020(BigDecimal price, String message) {
    request.setPrice(price);

    assertViolations(request, Map.of("price", Set.of(message)));
  }

  @Test
  void requestValidation_ReportsEveryMissingField_TC021() {
    assertViolations(
        new EditDrinkDetailRequest(),
        Map.of(
            "drinkDetailId", Set.of(ValidationMessage.Msg.FIELD_REQUIRED),
            "size", Set.of(ValidationMessage.Msg.FIELD_REQUIRED),
            "price", Set.of(ValidationMessage.Msg.FIELD_REQUIRED)));
  }

  // =========================================================================
  // HELPER STUB, VALIDATION & TEST DATA METHODS
  // =========================================================================

  private void stubAccessibleDetail(String role, UUID assignedShop) {
    if ("MANAGER".equals(role))
      when(commonMapper.checkShopIdIsExisted(USER_ID, assignedShop)).thenReturn(true);
    when(getMapper.getDrinkDetailVariantById(DETAIL_ID, assignedShop, role)).thenReturn(existing);
  }

  private void stubEditableDetail() {
    stubAccessibleDetail("OWNER", null);
    when(createMapper.checkDrinkAccessible(DRINK_ID, "OWNER", null)).thenReturn(true);
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
