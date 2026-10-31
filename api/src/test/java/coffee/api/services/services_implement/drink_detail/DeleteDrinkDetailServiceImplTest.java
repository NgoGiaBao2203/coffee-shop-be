package coffee.api.services.services_implement.drink_detail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.DeleteDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class DeleteDrinkDetailServiceImplTest {
  private static final UUID DRINK_ID = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
  private static final UUID DETAIL_ID = UUID.fromString("a8861994-e947-425c-a6bb-686f28d7ba63");
  private static final UUID USER_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
  private static final UUID SHOP_ID = UUID.fromString("b1000000-0000-0000-0000-000000000001");

  @Mock private CreateDrinkDetailMapper createMapper;
  @Mock private GetDrinkDetailMapper getMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private DeleteDrinkDetailMapper deleteMapper;
  private DeleteDrinkDetailServiceImpl service;
  private DeleteDrinkDetailRequest request;
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
        new DeleteDrinkDetailServiceImpl(
            deleteMapper, new DrinkDetailValidator(createMapper, getMapper, commonMapper));
    request = new DeleteDrinkDetailRequest();
    request.setDrinkDetailId(DETAIL_ID);
    existing = new DrinkDetailResult();
    existing.setDrinkDetailId(DETAIL_ID);
    existing.setDrinkId(DRINK_ID);
    existing.setSize("M");
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER"})
  void process_DeletesAccessibleDetailWhileOtherSizesRemain_TC001(String role) {
    stubAccessibleDetail(role, SHOP_ID);
    when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(2);
    when(deleteMapper.softDeleteDrinkDetail(DETAIL_ID, USER_ID, role, SHOP_ID)).thenReturn(1);

    service.process(request, USER_ID, role, SHOP_ID);

    var order = inOrder(getMapper, deleteMapper);
    order.verify(getMapper).getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role);
    order.verify(deleteMapper).countActiveDrinkDetails(DRINK_ID);
    order.verify(deleteMapper).softDeleteDrinkDetail(DETAIL_ID, USER_ID, role, SHOP_ID);
  }

  @ParameterizedTest
  @ValueSource(ints = {2, 3})
  void process_AllowsOwnerWithoutShopWhenAtLeastTwoSizesExist_TC002(int activeSizes) {
    stubAccessibleDetail("OWNER", null);
    when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(activeSizes);
    when(deleteMapper.softDeleteDrinkDetail(DETAIL_ID, USER_ID, "OWNER", null)).thenReturn(1);

    service.process(request, USER_ID, "OWNER", null);

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteMapper).softDeleteDrinkDetail(DETAIL_ID, USER_ID, "OWNER", null);
  }

  @Test
  void process_CurrentBehaviorAcceptsDeleteCountGreaterThanOne_TC003() {
    stubAccessibleDetail("OWNER", null);
    when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(2);
    when(deleteMapper.softDeleteDrinkDetail(DETAIL_ID, USER_ID, "OWNER", null)).thenReturn(2);

    service.process(request, USER_ID, "OWNER", null);

    verify(deleteMapper).softDeleteDrinkDetail(DETAIL_ID, USER_ID, "OWNER", null);
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / REQUEST & PERMISSION CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void process_RejectsMissingRequestOrDetailId_TC004(boolean missingRequest) {
    request.setDrinkDetailId(null);
    DeleteDrinkDetailRequest input = missingRequest ? null : request;

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(input, USER_ID, "OWNER", null));

    assertEquals("Drink detail ID is required", error.getMessage());
    verifyNoInteractions(commonMapper, getMapper, deleteMapper);
  }

  @Test
  void process_RejectsManagerWithoutAssignedShop_TC005() {
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", null));

    assertEquals("User is not assigned to any shop", error.getMessage());
    verifyNoInteractions(commonMapper, getMapper, deleteMapper);
  }

  @Test
  void process_RejectsInactiveMembership_TC006() {
    when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenReturn(false);

    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, USER_ID, "MANAGER", SHOP_ID));

    assertEquals("You do not have permission to access this shop", error.getMessage());
    verifyNoInteractions(getMapper, deleteMapper);
  }

  @Test
  void process_RejectsMissingOrInaccessibleDetail_TC007() {
    when(getMapper.getDrinkDetailVariantById(DETAIL_ID, null, "OWNER")).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("Drink detail not found", error.getMessage());
    assertEquals(DETAIL_ID, error.getId());
    verifyNoInteractions(deleteMapper);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 1})
  void process_RejectsRemovingTheLastActiveSize_TC008(int activeSizes) {
    stubAccessibleDetail("OWNER", null);
    when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(activeSizes);

    var error =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, USER_ID, "OWNER", null));

    assertEquals("A drink must have at least one active size", error.getMessage());
    verify(deleteMapper, never()).softDeleteDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_RejectsConcurrentDeletionOrModification_TC009() {
    stubAccessibleDetail("OWNER", null);
    when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(2);
    when(deleteMapper.softDeleteDrinkDetail(DETAIL_ID, USER_ID, "OWNER", null)).thenReturn(0);

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
  @CsvSource({"0, 0", "1, 0", "2, 0", "3, 1"})
  void process_PropagatesFailureAtEachDatabaseStage_TC010(int stage, long expectedDeleteCalls) {
    String role = stage == 0 ? "MANAGER" : "OWNER";
    DataAccessException failure = new DataAccessException("Database failure") {};
    if (stage > 1)
      when(getMapper.getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role)).thenReturn(existing);
    if (stage > 2) when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenReturn(2);
    switch (stage) {
      case 0 -> when(commonMapper.checkShopIdIsExisted(USER_ID, SHOP_ID)).thenThrow(failure);
      case 1 ->
          when(getMapper.getDrinkDetailVariantById(DETAIL_ID, SHOP_ID, role)).thenThrow(failure);
      case 2 -> when(deleteMapper.countActiveDrinkDetails(DRINK_ID)).thenThrow(failure);
      case 3 ->
          when(deleteMapper.softDeleteDrinkDetail(any(), any(), any(), any())).thenThrow(failure);
      default -> throw new IllegalArgumentException("Unexpected database stage");
    }

    var error =
        assertThrows(
            DataAccessException.class, () -> service.process(request, USER_ID, role, SHOP_ID));

    assertEquals("Database failure", error.getMessage());
    assertEquals(
        expectedDeleteCalls,
        mockingDetails(deleteMapper).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("softDeleteDrinkDetail"))
            .count());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL & ABNORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_AcceptsValidDetailId_TC011() {
    assertViolations(request, Map.of());
  }

  @Test
  void requestValidation_RejectsMissingDetailId_TC012() {
    request.setDrinkDetailId(null);

    assertViolations(
        request, Map.of("drinkDetailId", Set.of(ValidationMessage.Msg.FIELD_REQUIRED)));
  }

  // =========================================================================
  // HELPER STUB & VALIDATION METHODS
  // =========================================================================

  private void stubAccessibleDetail(String role, UUID assignedShop) {
    if ("MANAGER".equals(role))
      when(commonMapper.checkShopIdIsExisted(USER_ID, assignedShop)).thenReturn(true);
    when(getMapper.getDrinkDetailVariantById(DETAIL_ID, assignedShop, role)).thenReturn(existing);
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
}
