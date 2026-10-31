package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.invoice.CreateInvoiceRequest;
import coffee.api.dto.request.invoice.InvoiceItemRequest;
import coffee.api.dto.response.invoice.CreateInvoiceResponse;
import coffee.api.dto.result.DrinkDetailPriceResult;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class CreateInvoiceServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;
  @Captor private ArgumentCaptor<Set<UUID>> idsCaptor;
  private CreateInvoiceServiceImpl createInvoiceService;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private CreateInvoiceRequest validRequest;
  private UUID shopId;
  private UUID userId;
  private UUID tableId;
  private UUID firstDrinkDetailId;
  private UUID secondDrinkDetailId;

  @BeforeAll
  static void initValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    validatorFactory.close();
  }

  @BeforeEach
  void setUp() {
    createInvoiceService =
        new CreateInvoiceServiceImpl(
            createInvoiceMapper,
            new coffee.api.services.services_implement.invoice.common.InvoiceValidator(
                createInvoiceMapper, commonMapper));
    shopId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    tableId = UUID.fromString("00000000-0000-0000-0000-000000000103");
    firstDrinkDetailId = UUID.fromString("00000000-0000-0000-0000-000000000104");
    secondDrinkDetailId = UUID.fromString("00000000-0000-0000-0000-000000000105");

    validRequest = new CreateInvoiceRequest();
    validRequest.setShopId(shopId);
    validRequest.setTableId(tableId);
    validRequest.setItems(
        new ArrayList<>(
            List.of(
                item(firstDrinkDetailId, 2, "25000.00", "Less ice"),
                item(secondDrinkDetailId, 1, "30000.00", null))));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void process_CreatesRequestedPaymentStatus_AndOccupiesTable(int status) {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    validRequest.setStatus(status);
    stubValidOwnerInvoice();

    process("OWNER", null, userId);

    var order = inOrder(createInvoiceMapper);
    order.verify(createInvoiceMapper).occupyTable(tableId, shopId);
    order
        .verify(createInvoiceMapper)
        .insertInvoice(
            any(), eq(shopId), eq(userId), eq(tableId), eq(new BigDecimal("80000.00")), eq(status));
    order.verify(createInvoiceMapper).insertInvoiceDetails(any(), same(validRequest.getItems()));
  }

  @Test
  void process_RejectsTableClaimLostToAnotherRequest_BeforeInsert() {
    stubValidOwnerInvoice();
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(0);

    expectInvalid("OWNER", null, userId, "Table is unavailable or does not exist in this shop");

    verifyNoInsert();
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 2})
  void process_RejectsInvalidInitialStatus(int status) {
    validRequest.setStatus(status);

    expectInvalid("OWNER", null, userId, "Invoice status must be 0 (unpaid) or 1 (paid)");

    verifyNoInteractions(commonMapper, createInvoiceMapper);
    assertSingleViolation("status", ValidationMessage.Msg.STATUS_INVALID);
  }

  @Test
  void process_RejectsNullInitialStatus() {
    validRequest.setStatus(null);

    expectInvalid("OWNER", null, userId, "Invoice status must be 0 (unpaid) or 1 (paid)");

    verifyNoInteractions(commonMapper, createInvoiceMapper);
    assertSingleViolation("status", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  private InvoiceItemRequest item(UUID id, Integer quantity, String price, String note) {
    InvoiceItemRequest result = new InvoiceItemRequest();
    result.setDrinkDetailId(id);
    result.setQuantity(quantity);
    result.setUnitPrice(price == null ? null : new BigDecimal(price));
    result.setNote(note);
    return result;
  }

  private DrinkDetailPriceResult price(UUID id, String value) {
    DrinkDetailPriceResult result = new DrinkDetailPriceResult();
    result.setDrinkDetailId(id);
    result.setPrice(value == null ? null : new BigDecimal(value));
    return result;
  }

  private void stubShopExists() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
  }

  private void stubShopAndMembership() {
    stubShopExists();
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
  }

  private void stubTableAvailable() {
    when(createInvoiceMapper.checkTableAvailableInShop(tableId, shopId)).thenReturn(true);
  }

  private void stubNormalPrices() {
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(
            List.of(price(firstDrinkDetailId, "25000.00"), price(secondDrinkDetailId, "30000.00")));
  }

  private void stubValidOwnerInvoice() {
    stubShopExists();
    stubTableAvailable();
    stubNormalPrices();
  }

  private void stubValidNonOwnerInvoice() {
    stubShopAndMembership();
    stubTableAvailable();
    stubNormalPrices();
  }

  private CreateInvoiceResponse process(String role, UUID assignedShopId, UUID currentUserId) {
    return createInvoiceService.process(validRequest, role, assignedShopId, currentUserId);
  }

  private InvalidRequestException expectInvalid(
      String role, UUID assignedShopId, UUID currentUserId, String message) {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> process(role, assignedShopId, currentUserId));
    assertEquals(message, exception.getMessage());
    return exception;
  }

  private void verifyNoInsert() {
    verify(createInvoiceMapper, never()).insertInvoice(any(), any(), any(), any(), any(), anyInt());
    verify(createInvoiceMapper, never()).insertInvoiceDetails(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenOwnerCreatesInvoice_TC001() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();

    CreateInvoiceResponse response = process(Roles.OWNER.name(), null, userId);

    assertEquals("S0001", response.getCode());
    assertEquals("Create invoice successfully", response.getMessage());
    ArgumentCaptor<UUID> invoiceIdCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(createInvoiceMapper)
        .insertInvoice(
            invoiceIdCaptor.capture(),
            eq(shopId),
            eq(userId),
            eq(tableId),
            eq(new BigDecimal("80000.00")),
            eq(0));
    assertNotNull(invoiceIdCaptor.getValue());
    verify(createInvoiceMapper)
        .insertInvoiceDetails(eq(invoiceIdCaptor.getValue()), same(validRequest.getItems()));
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_Success_WhenManagerCreatesInvoice_TC002() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidNonOwnerInvoice();

    assertEquals("S0001", process(Roles.MANAGER.name(), shopId, userId).getCode());

    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
    verify(createInvoiceMapper)
        .insertInvoice(
            any(), eq(shopId), eq(userId), eq(tableId), eq(new BigDecimal("80000.00")), eq(0));
    verify(createInvoiceMapper).insertInvoiceDetails(any(), same(validRequest.getItems()));
  }

  @Test
  void process_Success_WhenStaffCreatesInvoice_TC003() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidNonOwnerInvoice();

    assertEquals("S0001", process(Roles.STAFF.name(), shopId, userId).getCode());

    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
    verify(createInvoiceMapper).insertInvoiceDetails(any(), same(validRequest.getItems()));
  }

  @Test
  void process_ThrowsInvalid_WhenManagerOmitsShopId_TC004() {
    validRequest.setShopId(null);

    expectInvalid("MANAGER", shopId, userId, "Shop ID is required");

    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenStaffOmitsShopId_TC005() {
    validRequest.setShopId(null);

    expectInvalid("STAFF", shopId, userId, "Shop ID is required");

    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_Success_WhenRoleCaseIsInsensitive_TC006() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();
    assertEquals("S0001", process("owner", null, userId).getCode());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_Success_WhenManagerRoleCaseIsInsensitive_TC007() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidNonOwnerInvoice();
    assertEquals("S0001", process("manager", shopId, userId).getCode());
    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
  }

  @Test
  void process_Success_WhenStaffRoleCaseIsInsensitive_TC008() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidNonOwnerInvoice();
    assertEquals("S0001", process("staff", shopId, userId).getCode());
    verify(createInvoiceMapper).insertInvoiceDetails(any(), any());
  }

  @Test
  void process_Success_WhenPricesHaveDifferentDecimalScale_TC009() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("25000"));
    validRequest.getItems().get(1).setUnitPrice(new BigDecimal("30000.0"));

    assertEquals("S0001", process(Roles.OWNER.name(), null, userId).getCode());
    verify(createInvoiceMapper)
        .insertInvoice(
            any(), eq(shopId), eq(userId), eq(tableId), eq(new BigDecimal("80000.00")), eq(0));
  }

  @Test
  void process_Success_WhenSameDrinkAppearsTwice_TC010() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    validRequest.getItems().get(1).setDrinkDetailId(firstDrinkDetailId);
    validRequest.getItems().get(1).setUnitPrice(new BigDecimal("25000.00"));
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, "25000.00")));

    assertEquals("S0001", process(Roles.OWNER.name(), null, userId).getCode());

    verify(createInvoiceMapper).getSellableDrinkPrices(idsCaptor.capture(), eq(shopId));
    assertEquals(Set.of(firstDrinkDetailId), idsCaptor.getValue());
    verify(createInvoiceMapper)
        .insertInvoice(
            any(), eq(shopId), eq(userId), eq(tableId), eq(new BigDecimal("75000.00")), eq(0));
  }

  @Test
  void process_Success_WhenTotalEqualsMaximum_TC011() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    validRequest.setItems(List.of(item(firstDrinkDetailId, 1, "9999999999999999.99", null)));
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, "9999999999999999.99")));

    assertEquals("S0001", process(Roles.OWNER.name(), null, userId).getCode());

    verify(createInvoiceMapper)
        .insertInvoice(
            any(),
            eq(shopId),
            eq(userId),
            eq(tableId),
            eq(new BigDecimal("9999999999999999.99")),
            eq(0));
  }

  @Test
  void process_Success_WhenDatabaseReturnsZeroRows_CurrentBehavior_TC012() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();
    // The current service ignores the return values of both insert methods.
    // Mockito returns 0 by default, so the service still reports success.
    assertEquals("S0001", process(Roles.OWNER.name(), null, userId).getCode());
    verify(createInvoiceMapper).insertInvoice(any(), any(), any(), any(), any(), anyInt());
    verify(createInvoiceMapper).insertInvoiceDetails(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - ROLE AND SHOP ERRORS
  // =========================================================================

  @Test
  void process_ThrowsInvalid_WhenOwnerShopIdIsNull_TC013() {
    validRequest.setShopId(null);
    expectInvalid("OWNER", shopId, userId, "Shop ID is required");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenRoleIsUnauthorized_TC014() {
    expectInvalid("ADMIN", shopId, userId, "You do not have permission to create invoices");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenRoleIsNull_TC015() {
    expectInvalid(null, shopId, userId, "You do not have permission to create invoices");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenRoleIsEmpty_TC016() {
    expectInvalid("", shopId, userId, "You do not have permission to create invoices");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenManagerHasNoAssignedShop_TC017() {
    expectInvalid("MANAGER", null, userId, "User is not assigned to any shop");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenStaffHasNoAssignedShop_TC018() {
    expectInvalid("STAFF", null, userId, "User is not assigned to any shop");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenManagerUsesAnotherShop_TC019() {
    validRequest.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000106"));
    expectInvalid("MANAGER", shopId, userId, "You do not have permission to access this shop");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsInvalid_WhenStaffUsesAnotherShop_TC020() {
    validRequest.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000107"));
    expectInvalid("STAFF", shopId, userId, "You do not have permission to access this shop");
    verifyNoInteractions(commonMapper, createInvoiceMapper);
  }

  @Test
  void process_ThrowsNotFound_WhenShopDoesNotExist_TC021() {
    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process("OWNER", null, userId));
    assertEquals("Data not found", exception.getMessage());
    assertEquals(shopId, exception.getId());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsNotFound_WhenShopExistsMapperReturnsNull_TC022() {
    // Default Mockito return for a boxed Boolean is false or null depending on configuration.
    // Explicitly test the null-safe Boolean.TRUE.equals branch.
    when(commonMapper.checkShopExisted(shopId)).thenReturn(null);
    assertThrows(DataNotFoundException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenOwnerUserIdIsNull_TC023() {
    stubShopExists();
    expectInvalid("OWNER", null, null, "You do not have permission to access this shop");
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenManagerUserIdIsNull_TC024() {
    stubShopExists();
    expectInvalid("MANAGER", shopId, null, "You do not have permission to access this shop");
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenManagerMembershipMissing_TC025() {
    stubShopExists();
    expectInvalid("MANAGER", shopId, userId, "You do not have permission to access this shop");
    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenStaffMembershipMissing_TC026() {
    stubShopExists();
    expectInvalid("STAFF", shopId, userId, "You do not have permission to access this shop");
    verify(commonMapper).checkShopIdIsExisted(userId, shopId);
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenMembershipMapperReturnsNull_TC027() {
    stubShopExists();
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(null);
    expectInvalid("MANAGER", shopId, userId, "You do not have permission to access this shop");
    verifyNoInsert();
  }

  // =========================================================================
  // SERVICE PROCESS - TABLE AND PRICE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsInvalid_WhenTableIsNotAvailable_TC028() {
    stubShopExists();
    expectInvalid("OWNER", null, userId, "Table is unavailable or does not exist in this shop");
    verify(createInvoiceMapper).checkTableAvailableInShop(tableId, shopId);
    verify(createInvoiceMapper, never()).getSellableDrinkPrices(anySet(), any());
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenTableIdIsNull_TC029() {
    validRequest.setTableId(null);
    stubShopExists();
    expectInvalid("OWNER", null, userId, "Table is unavailable or does not exist in this shop");
    verify(createInvoiceMapper).checkTableAvailableInShop(null, shopId);
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenNoSellablePricesAreReturned_TC030() {
    stubShopExists();
    stubTableAvailable();
    expectInvalid("OWNER", null, userId, "Drink is unavailable or does not belong to this shop");
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenOneDrinkIsMissing_TC031() {
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, "25000.00")));
    expectInvalid("OWNER", null, userId, "Drink is unavailable or does not belong to this shop");
    verifyNoInsert();
  }

  @Test
  void process_ThrowsNullPointer_WhenReturnedPriceIsNull_TC032() {
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, null)));
    // Collectors.toMap rejects null values returned by the mapper.
    assertThrows(NullPointerException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenReturnedPriceIsNegative_TC033() {
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, "-1.00")));
    expectInvalid("OWNER", null, userId, "Drink is unavailable or does not belong to this shop");
    verifyNoInsert();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1.00", "24999.99", "25000.01", "99999.00"})
  void process_ThrowsInvalid_WhenSubmittedPriceChanged_TC034(String submittedPrice) {
    stubValidOwnerInvoice();
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal(submittedPrice));
    expectInvalid("OWNER", null, userId, "Drink price has changed. Please reload and try again");
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenSubmittedPriceIsNull_RuntimeBehavior_TC035() {
    stubValidOwnerInvoice();
    validRequest.getItems().get(0).setUnitPrice(null);
    // Direct service calls do not run Jakarta Bean Validation.
    // BigDecimal.compareTo(null) currently throws NullPointerException.
    assertThrows(NullPointerException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  @Test
  void process_ThrowsInvalid_WhenTotalExceedsMaximum_TC036() {
    validRequest.setItems(List.of(item(firstDrinkDetailId, 2, "9999999999999999.99", null)));
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenReturn(List.of(price(firstDrinkDetailId, "9999999999999999.99")));
    expectInvalid("OWNER", null, userId, "Invoice total exceeds allowed amount");
    verifyNoInsert();
  }

  @Test
  void process_ThrowsNullPointer_WhenItemsAreNull_DirectServiceCall_TC037() {
    validRequest.setItems(null);
    stubShopExists();
    stubTableAvailable();
    assertThrows(NullPointerException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  @Test
  void process_ThrowsNullPointer_WhenAnItemIsNull_DirectServiceCall_TC038() {
    validRequest.setItems(new ArrayList<>());
    validRequest.getItems().add(null);
    stubShopExists();
    stubTableAvailable();
    assertThrows(NullPointerException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  @Test
  void process_ThrowsNullPointer_WhenQuantityIsNull_DirectServiceCall_TC039() {
    stubValidOwnerInvoice();
    validRequest.getItems().get(0).setQuantity(null);
    assertThrows(NullPointerException.class, () -> process("OWNER", null, userId));
    verifyNoInsert();
  }

  // =========================================================================
  // SERVICE PROCESS - DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenInsertInvoiceFails_TC040() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();
    doThrow(new DataAccessException("Database insert invoice error") {})
        .when(createInvoiceMapper)
        .insertInvoice(any(), any(), any(), any(), any(), anyInt());

    DataAccessException exception =
        assertThrows(DataAccessException.class, () -> process("OWNER", null, userId));

    assertEquals("Database insert invoice error", exception.getMessage());
    verify(createInvoiceMapper, never()).insertInvoiceDetails(any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenInsertDetailsFails_TC041() {
    when(createInvoiceMapper.occupyTable(tableId, shopId)).thenReturn(1);
    stubValidOwnerInvoice();
    doThrow(new DataAccessException("Database insert details error") {})
        .when(createInvoiceMapper)
        .insertInvoiceDetails(any(), any());

    DataAccessException exception =
        assertThrows(DataAccessException.class, () -> process("OWNER", null, userId));

    assertEquals("Database insert details error", exception.getMessage());
    verify(createInvoiceMapper).insertInvoice(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  void process_ThrowsDataAccessException_WhenPriceQueryFails_TC042() {
    stubShopExists();
    stubTableAvailable();
    when(createInvoiceMapper.getSellableDrinkPrices(anySet(), eq(shopId)))
        .thenThrow(new DataAccessException("Database price query error") {});

    DataAccessException exception =
        assertThrows(DataAccessException.class, () -> process("OWNER", null, userId));

    assertEquals("Database price query error", exception.getMessage());
    verifyNoInsert();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - SUCCESS CASES
  // =========================================================================

  private void assertRequestValid() {
    assertEquals(0, validator.validate(validRequest).size());
  }

  private void assertSingleViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<CreateInvoiceRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size(), violations.toString());
    ConstraintViolation<CreateInvoiceRequest> violation = violations.iterator().next();
    assertEquals(property, violation.getPropertyPath().toString());
    assertEquals(expectedMessage, violation.getMessage());
  }

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC043() {
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenNoteIsNull_TC044() {
    validRequest.getItems().get(0).setNote(null);
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenNoteHas255Characters_TC045() {
    validRequest.getItems().get(0).setNote("A".repeat(255));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenQuantityIsOne_TC046() {
    validRequest.getItems().get(0).setQuantity(1);
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenQuantityIs999_TC047() {
    validRequest.getItems().get(0).setQuantity(999);
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenPriceHasTwoDecimals_TC048() {
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("0.01"));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenPriceHas16IntegerDigits_TC049() {
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("9999999999999999.99"));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenItemsContainOneEntry_TC050() {
    validRequest.setItems(List.of(validRequest.getItems().get(0)));
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - FAILED CASES
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC051() {
    validRequest.setShopId(null);
    assertSingleViolation("shopId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenTableIdIsNull_TC052() {
    validRequest.setTableId(null);
    assertSingleViolation("tableId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenItemsAreNull_TC053() {
    validRequest.setItems(null);
    assertSingleViolation("items", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenItemsAreEmpty_TC054() {
    validRequest.setItems(List.of());
    assertSingleViolation("items", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenAnItemIsNull_TC055() {
    validRequest.setItems(new ArrayList<>());
    validRequest.getItems().add(null);
    assertSingleViolation("items[0].<list element>", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenDrinkDetailIdIsNull_TC056() {
    validRequest.getItems().get(0).setDrinkDetailId(null);
    assertSingleViolation("items[0].drinkDetailId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenQuantityIsNull_TC057() {
    validRequest.getItems().get(0).setQuantity(null);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenQuantityIsZero_TC058() {
    validRequest.getItems().get(0).setQuantity(0);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenQuantityIsNegative_TC059() {
    validRequest.getItems().get(0).setQuantity(-1);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenQuantityExceeds999_TC060() {
    validRequest.getItems().get(0).setQuantity(1000);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIsNull_TC061() {
    validRequest.getItems().get(0).setUnitPrice(null);
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIsZero_TC062() {
    validRequest.getItems().get(0).setUnitPrice(BigDecimal.ZERO);
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.PRICE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIsNegative_TC063() {
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("-1.00"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.PRICE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceHasThreeDecimals_TC064() {
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("25000.123"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceHas17IntegerDigits_TC065() {
    validRequest.getItems().get(0).setUnitPrice(new BigDecimal("10000000000000000.00"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenNoteExceeds255Characters_TC066() {
    validRequest.getItems().get(0).setNote("A".repeat(256));
    assertSingleViolation("items[0].note", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenAllTopLevelFieldsAreNull_TC067() {
    CreateInvoiceRequest empty = new CreateInvoiceRequest();
    Set<ConstraintViolation<CreateInvoiceRequest>> violations = validator.validate(empty);
    assertEquals(3, violations.size());
  }

  @Test
  void requestValidation_Fails_WhenAllItemRequiredFieldsAreNull_TC068() {
    validRequest.setItems(List.of(new InvoiceItemRequest()));
    Set<ConstraintViolation<CreateInvoiceRequest>> violations = validator.validate(validRequest);
    assertEquals(3, violations.size());
  }
}
