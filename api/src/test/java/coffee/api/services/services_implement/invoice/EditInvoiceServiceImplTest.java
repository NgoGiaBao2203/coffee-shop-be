package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.invoice.EditInvoiceRequest;
import coffee.api.dto.request.invoice.InvoiceItemRequest;
import coffee.api.dto.result.DrinkDetailPriceResult;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.UpdateInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
class EditInvoiceServiceImplTest {

  @Mock private UpdateInvoiceMapper updateMapper;
  @Mock private CreateInvoiceMapper createMapper;
  @Mock private CommonMapper commonMapper;

  private EditInvoiceServiceImpl service;
  private Validator validator;

  private EditInvoiceRequest request;
  private InvoiceResult invoice;
  private UUID shopId;
  private UUID userId;
  private UUID tableId;
  private String ownerRole;
  private String managerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    service =
        new EditInvoiceServiceImpl(
            updateMapper, createMapper, new InvoiceValidator(createMapper, commonMapper));

    shopId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    userId = UUID.fromString("00000000-0000-0000-0000-000000000202");
    tableId = UUID.fromString("00000000-0000-0000-0000-000000000203");

    ownerRole = Roles.OWNER.getValue();
    managerRole = Roles.MANAGER.getValue();
    staffRole = Roles.STAFF.getValue();

    var item = new InvoiceItemRequest();
    item.setDrinkDetailId(UUID.fromString("00000000-0000-0000-0000-000000000204"));
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("25000.00"));
    item.setNote("Ít đá");

    request = new EditInvoiceRequest();
    request.setInvoiceId(UUID.fromString("00000000-0000-0000-0000-000000000205"));
    request.setShopId(shopId);
    request.setTableId(tableId);
    request.setItems(List.of(item));

    invoice = new InvoiceResult();
    invoice.setInvoiceId(request.getInvoiceId());
    invoice.setShopId(shopId);
    invoice.setTableId(tableId);
    invoice.setProfileId(userId);
    invoice.setStatus("0");
  }

  // =========================================================================
  // HELPER STUB & VERIFY METHODS
  // =========================================================================

  private void stubInvoice(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (!ownerRole.equalsIgnoreCase(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    }
    when(updateMapper.findInvoiceById(request.getInvoiceId())).thenReturn(invoice);
  }

  private void stubPrices(String value) {
    var price = new DrinkDetailPriceResult();
    price.setDrinkDetailId(request.getItems().getFirst().getDrinkDetailId());
    price.setPrice(new BigDecimal(value));
    when(createMapper.getSellableDrinkPrices(anySet(), eq(shopId))).thenReturn(List.of(price));
  }

  private void stubValidEdit(String role) {
    stubInvoice(role);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    stubPrices("25000.00");
  }

  private void stubSave() {
    when(updateMapper.updateInvoice(
            request.getInvoiceId(), request.getTableId(), new BigDecimal("50000.00"), shopId))
        .thenReturn(1);
    when(createMapper.insertInvoiceDetails(request.getInvoiceId(), request.getItems()))
        .thenReturn(1);
  }

  private void process(String role) {
    service.process(request, role, shopId, userId);
  }

  private void verifyNoSave() {
    verify(updateMapper, never()).updateInvoice(any(), any(), any(), any());
    verify(updateMapper, never()).deleteInvoiceDetails(any());
    verify(createMapper, never()).insertInvoiceDetails(any(), any());
    verify(updateMapper, never()).releaseTable(any(), any());
  }

  private void assertRequestValid() {
    Set<ConstraintViolation<EditInvoiceRequest>> violations = validator.validate(request);
    assertEquals(0, violations.size());
  }

  private void assertSingleViolation(String property, String expectedMessage) {
    Set<ConstraintViolation<EditInvoiceRequest>> violations = validator.validate(request);
    assertEquals(1, violations.size(), violations.toString());
    ConstraintViolation<EditInvoiceRequest> violation = violations.iterator().next();
    assertEquals(property, violation.getPropertyPath().toString());
    assertEquals(expectedMessage, violation.getMessage());
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_Success_WhenUpdatingUnpaidInvoiceKeepingOccupiedTable_TC001(String role) {
    stubValidEdit(role);
    stubSave();

    assertDoesNotThrow(() -> process(role));

    InOrder order = inOrder(updateMapper, createMapper);
    order
        .verify(updateMapper)
        .updateInvoice(request.getInvoiceId(), tableId, new BigDecimal("50000.00"), shopId);
    order.verify(updateMapper).deleteInvoiceDetails(request.getInvoiceId());
    order.verify(createMapper).insertInvoiceDetails(request.getInvoiceId(), request.getItems());
    verify(createMapper, never()).occupyTable(any(), any());
    verify(updateMapper, never()).releaseTable(any(), any());
    assertEquals("0", invoice.getStatus());
  }

  @Test
  void process_Success_WhenMovingToAvailableTableAndReleasesPreviousTable_TC002() {
    stubValidEdit(ownerRole);
    UUID destination = UUID.fromString("00000000-0000-0000-0000-000000000206");
    request.setTableId(destination);
    when(createMapper.checkTableAvailableInShop(destination, shopId)).thenReturn(true);
    when(createMapper.occupyTable(destination, shopId)).thenReturn(1);
    when(updateMapper.releaseTable(tableId, shopId)).thenReturn(1);
    stubSave();

    assertDoesNotThrow(() -> process(ownerRole));

    InOrder order = inOrder(createMapper, updateMapper);
    order.verify(createMapper).occupyTable(destination, shopId);
    order
        .verify(updateMapper)
        .updateInvoice(request.getInvoiceId(), destination, new BigDecimal("50000.00"), shopId);
    order.verify(updateMapper).deleteInvoiceDetails(request.getInvoiceId());
    order.verify(createMapper).insertInvoiceDetails(request.getInvoiceId(), request.getItems());
    order.verify(updateMapper).releaseTable(tableId, shopId);
  }

  @Test
  void process_Success_WhenManagerEditsAnotherStaffInvoice_TC003() {
    stubValidEdit(managerRole);
    invoice.setProfileId(UUID.fromString("00000000-0000-0000-0000-000000000207"));
    stubSave();

    assertDoesNotThrow(() -> process(managerRole));

    verify(updateMapper).deleteInvoiceDetails(request.getInvoiceId());
  }

  @ParameterizedTest
  @ValueSource(strings = {"owner", "manager", "staff"})
  void process_Success_WhenRoleProvidedInDifferentCasing_TC004(String role) {
    stubValidEdit(role.toUpperCase(java.util.Locale.ROOT));
    stubSave();

    assertDoesNotThrow(() -> process(role));

    verify(updateMapper).deleteInvoiceDetails(request.getInvoiceId());
  }

  @Test
  void process_Success_WhenOwnerEditsWithoutAssignedShop_TC005() {
    stubValidEdit(ownerRole);
    invoice.setProfileId(UUID.fromString("00000000-0000-0000-0000-000000000208"));
    stubSave();

    assertDoesNotThrow(() -> service.process(request, ownerRole, null, userId));

    verify(updateMapper).deleteInvoiceDetails(request.getInvoiceId());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_Success_WhenItemPriceHasDifferentScale_TC006() {
    stubValidEdit(ownerRole);
    request.getItems().getFirst().setUnitPrice(new BigDecimal("25000"));
    stubSave();

    assertDoesNotThrow(() -> process(ownerRole));

    assertEquals(new BigDecimal("25000"), request.getItems().getFirst().getUnitPrice());
    verify(updateMapper)
        .updateInvoice(request.getInvoiceId(), tableId, new BigDecimal("50000.00"), shopId);
  }

  @Test
  void process_Success_WhenTotalAmountIsAtMaximumBoundary_TC007() {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    request.getItems().getFirst().setQuantity(1);
    request.getItems().getFirst().setUnitPrice(new BigDecimal("9999999999999999.99"));
    stubPrices("9999999999999999.99");
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);
    when(createMapper.insertInvoiceDetails(any(), any())).thenReturn(1);

    assertDoesNotThrow(() -> process(ownerRole));

    verify(updateMapper)
        .updateInvoice(
            request.getInvoiceId(), tableId, new BigDecimal("9999999999999999.99"), shopId);
  }

  @Test
  void process_Success_WhenMultipleItemsCalculatedAndPriceLookupsDeduplicated_TC008() {
    stubValidEdit(ownerRole);
    var second = new InvoiceItemRequest();
    second.setDrinkDetailId(request.getItems().getFirst().getDrinkDetailId());
    second.setQuantity(1);
    second.setUnitPrice(new BigDecimal("25000.00"));
    second.setNote("No ice");
    request.setItems(List.of(request.getItems().getFirst(), second));
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);
    when(createMapper.insertInvoiceDetails(any(), any())).thenReturn(2);

    assertDoesNotThrow(() -> process(ownerRole));

    verify(createMapper).getSellableDrinkPrices(Set.of(second.getDrinkDetailId()), shopId);
    verify(updateMapper)
        .updateInvoice(request.getInvoiceId(), tableId, new BigDecimal("75000.00"), shopId);
    verify(createMapper).insertInvoiceDetails(request.getInvoiceId(), request.getItems());
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / STATUS & PERMISSION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNullInRequest_TC009() {
    request.setShopId(null);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(updateMapper, createMapper, commonMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_ThrowsInvalidRequestException_WhenNonOwnerAssignedShopIsNull_TC010(String role) {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, role, null, userId));

    assertEquals("User is not assigned to any shop", exception.getMessage());
    verifyNoInteractions(updateMapper, createMapper, commonMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenAssignedShopMismatchesRequestShop_TC011() {
    UUID otherShopId = UUID.fromString("00000000-0000-0000-0000-000000000209");
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(request, staffRole, otherShopId, userId));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInteractions(updateMapper, createMapper, commonMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ADMIN", ""})
  @NullSource
  void process_ThrowsInvalidRequestException_WhenUserRoleIsUnsupported_TC012(String role) {
    var error = assertThrows(InvalidRequestException.class, () -> process(role));
    assertEquals("You do not have permission to create invoices", error.getMessage());
    verifyNoInteractions(updateMapper, createMapper, commonMapper);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExistOrReturnsNull_TC013(
      boolean nullResult) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(nullResult ? null : false);

    DataNotFoundException exception =
        assertThrows(DataNotFoundException.class, () -> process(ownerRole));

    assertEquals(shopId, exception.getId());
    verifyNoInteractions(updateMapper, createMapper);
  }

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_ThrowsInvalidRequestException_WhenCurrentUserIdIsNull_TC014(String role) {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> service.process(request, role, shopId, null));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
    verifyNoInteractions(updateMapper, createMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopMembershipDoesNotExist_TC015() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    var error = assertThrows(InvalidRequestException.class, () -> process(managerRole));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInteractions(updateMapper, createMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopMembershipReturnsNull_TC016() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(null);

    var error = assertThrows(InvalidRequestException.class, () -> process(staffRole));
    assertEquals("You do not have permission to access this shop", error.getMessage());

    verifyNoInteractions(updateMapper, createMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenInvoiceNotFound_TC017() {
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    var error = assertThrows(DataNotFoundException.class, () -> process(ownerRole));
    assertEquals("Invoice not found", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInvoiceBelongsToAnotherShop_TC018() {
    stubInvoice(ownerRole);
    invoice.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000210"));

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Invoice does not belong to this shop", error.getMessage());

    verifyNoSave();
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "2", "3"})
  @NullSource
  void process_ThrowsInvalidRequestException_WhenInvoiceStatusIsNotUnpaid_TC019(String status) {
    stubInvoice(ownerRole);
    invoice.setStatus(status);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals(
        "Cannot edit an invoice that is already completed or cancelled", exception.getMessage());
    verifyNoSave();
    verifyNoInteractions(createMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffEditsInvoiceCreatedByAnotherUser_TC020() {
    stubInvoice(staffRole);
    invoice.setProfileId(UUID.fromString("00000000-0000-0000-0000-000000000211"));

    var error = assertThrows(InvalidRequestException.class, () -> process(staffRole));
    assertEquals("You do not have permission to edit this invoice", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStaffInvoiceHasNullCreator_TC021() {
    stubInvoice(staffRole);
    invoice.setProfileId(null);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(staffRole));

    assertEquals("You do not have permission to edit this invoice", exception.getMessage());
    verifyNoSave();
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / TABLE & DRINK PRICE CASES
  // =========================================================================

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void process_ThrowsInvalidRequestException_WhenCurrentTableMissingOrUnavailable_TC022(
      boolean missingId) {
    stubInvoice(ownerRole);
    if (missingId) invoice.setTableId(null);

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals(
        "Current table is unavailable or does not belong to this shop", error.getMessage());

    verifyNoSave();
    verifyNoInteractions(createMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTargetTableIsOccupiedOrUnavailable_TC023() {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    request.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000212"));

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Table is unavailable or does not exist in this shop", error.getMessage());

    verifyNoSave();
    verify(createMapper, never()).getSellableDrinkPrices(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTargetTableOccupyFails_TC024() {
    stubValidEdit(ownerRole);
    request.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000213"));
    when(createMapper.checkTableAvailableInShop(request.getTableId(), shopId)).thenReturn(true);

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Table is unavailable or does not exist in this shop", error.getMessage());

    verifyNoSave();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1.00", "24999.99", "25000.01"})
  void process_ThrowsInvalidRequestException_WhenItemPriceMismatchesOrInvalid_TC025(String price) {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    stubPrices(price);

    assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkDetailIsDeletedOrUnavailable_TC026() {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Drink is unavailable or does not belong to this shop", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenOneOfMultipleItemsPriceMissing_TC027() {
    stubValidEdit(ownerRole);
    var second = new InvoiceItemRequest();
    second.setDrinkDetailId(UUID.fromString("00000000-0000-0000-0000-000000000214"));
    second.setQuantity(1);
    second.setUnitPrice(new BigDecimal("25000.00"));
    request.setItems(List.of(request.getItems().getFirst(), second));

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals("Drink is unavailable or does not belong to this shop", exception.getMessage());
    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTotalAmountExceedsMaximum_TC028() {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    request.getItems().getFirst().setUnitPrice(new BigDecimal("9999999999999999.99"));
    stubPrices("9999999999999999.99");

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Invoice total exceeds allowed amount", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInvoiceUpdateAffectsZeroRows_TC029() {
    stubValidEdit(ownerRole);

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Invoice cannot be edited. Please reload and try again", error.getMessage());

    verify(updateMapper, never()).deleteInvoiceDetails(any());
    verify(createMapper, never()).insertInvoiceDetails(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInsertInvoiceDetailsFails_TC030() {
    stubValidEdit(ownerRole);
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);

    var error = assertThrows(InvalidRequestException.class, () -> process(ownerRole));
    assertEquals("Invoice details could not be saved", error.getMessage());

    verify(updateMapper, never()).releaseTable(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenInsertInvoiceDetailsPartiallySaved_TC031() {
    stubValidEdit(ownerRole);
    var second = new InvoiceItemRequest();
    second.setDrinkDetailId(request.getItems().getFirst().getDrinkDetailId());
    second.setQuantity(1);
    second.setUnitPrice(new BigDecimal("25000.00"));
    request.setItems(List.of(request.getItems().getFirst(), second));
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);
    when(createMapper.insertInvoiceDetails(any(), any())).thenReturn(1);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> process(ownerRole));

    assertEquals("Invoice details could not be saved", exception.getMessage());
    verify(updateMapper, never()).releaseTable(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPreviousTableReleaseFails_TC032() {
    stubValidEdit(ownerRole);
    request.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000215"));
    when(createMapper.checkTableAvailableInShop(request.getTableId(), shopId)).thenReturn(true);
    when(createMapper.occupyTable(request.getTableId(), shopId)).thenReturn(1);
    stubSave();

    assertThrows(InvalidRequestException.class, () -> process(ownerRole));
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / DATABASE ERRORS
  // =========================================================================

  @Test
  void process_ThrowsDataAccessException_WhenUpdatingInvoiceFails_TC033() {
    stubValidEdit(ownerRole);
    when(updateMapper.updateInvoice(any(), any(), any(), any()))
        .thenThrow(new DataAccessException("Database error") {});

    assertThrows(DataAccessException.class, () -> process(ownerRole));

    verify(updateMapper, never()).deleteInvoiceDetails(any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenPriceQueryFails_TC034() {
    stubInvoice(ownerRole);
    when(updateMapper.lockOccupiedTable(tableId, shopId)).thenReturn(tableId);
    when(createMapper.getSellableDrinkPrices(any(), any()))
        .thenThrow(new DataAccessException("Price query failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));
    assertEquals("Price query failed", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsDataAccessException_WhenTableOccupyFails_TC035() {
    stubValidEdit(ownerRole);
    request.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000216"));
    when(createMapper.checkTableAvailableInShop(request.getTableId(), shopId)).thenReturn(true);
    when(createMapper.occupyTable(request.getTableId(), shopId))
        .thenThrow(new DataAccessException("Table claim failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));
    assertEquals("Table claim failed", error.getMessage());

    verifyNoSave();
  }

  @Test
  void process_ThrowsDataAccessException_WhenDeletingInvoiceDetailsFails_TC036() {
    stubValidEdit(ownerRole);
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);
    when(updateMapper.deleteInvoiceDetails(any()))
        .thenThrow(new DataAccessException("Delete details failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));
    assertEquals("Delete details failed", error.getMessage());

    verify(createMapper, never()).insertInvoiceDetails(any(), any());
    verify(updateMapper, never()).releaseTable(any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenInsertingInvoiceDetailsFails_TC037() {
    stubValidEdit(ownerRole);
    when(updateMapper.updateInvoice(any(), any(), any(), any())).thenReturn(1);
    when(createMapper.insertInvoiceDetails(any(), any()))
        .thenThrow(new DataAccessException("Insert details failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));
    assertEquals("Insert details failed", error.getMessage());

    verify(updateMapper, never()).releaseTable(any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenReleasingTableFails_TC038() {
    stubValidEdit(ownerRole);
    request.setTableId(UUID.fromString("00000000-0000-0000-0000-000000000217"));
    when(createMapper.checkTableAvailableInShop(request.getTableId(), shopId)).thenReturn(true);
    when(createMapper.occupyTable(request.getTableId(), shopId)).thenReturn(1);
    stubSave();
    when(updateMapper.releaseTable(tableId, shopId))
        .thenThrow(new DataAccessException("Release table failed") {});

    var error = assertThrows(DataAccessException.class, () -> process(ownerRole));
    assertEquals("Release table failed", error.getMessage());

    verify(createMapper).insertInvoiceDetails(request.getInvoiceId(), request.getItems());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void requestValidation_Success_WhenAllFieldsAreValid_TC039() {
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenNoteIsNull_TC040() {
    request.getItems().getFirst().setNote(null);
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenNoteIsAtMaxLength255_TC041() {
    request.getItems().getFirst().setNote("A".repeat(255));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenQuantityIsAtMinOrMaxBoundary_TC042() {
    request.getItems().getFirst().setQuantity(1);
    assertRequestValid();

    request.getItems().getFirst().setQuantity(999);
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenUnitPriceIsAtMinOrMaxBoundary_TC043() {
    request.getItems().getFirst().setUnitPrice(new BigDecimal("0.01"));
    assertRequestValid();

    request.getItems().getFirst().setUnitPrice(new BigDecimal("9999999999999999.99"));
    assertRequestValid();
  }

  @Test
  void requestValidation_Success_WhenItemsContainSingleEntry_TC044() {
    request.setItems(List.of(request.getItems().getFirst()));
    assertRequestValid();
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: ROOT REQUEST FIELDS
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenInvoiceIdIsNull_TC045() {
    request.setInvoiceId(null);
    assertSingleViolation("invoiceId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenShopIdIsNull_TC046() {
    request.setShopId(null);
    assertSingleViolation("shopId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenTableIdIsNull_TC047() {
    request.setTableId(null);
    assertSingleViolation("tableId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenItemsListIsNull_TC048() {
    request.setItems(null);
    assertSingleViolation("items", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenItemsListIsEmpty_TC049() {
    request.setItems(List.of());
    assertSingleViolation("items", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenAnItemElementIsNull_TC050() {
    request.setItems(new ArrayList<>());
    request.getItems().add(null);
    assertSingleViolation("items[0].<list element>", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenAllTopLevelFieldsAreNull_TC051() {
    EditInvoiceRequest empty = new EditInvoiceRequest();
    Set<ConstraintViolation<EditInvoiceRequest>> violations = validator.validate(empty);
    assertEquals(4, violations.size());
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION - ABNORMAL CASES: ITEM FIELDS
  // =========================================================================

  @Test
  void requestValidation_Fails_WhenDrinkDetailIdIsNull_TC052() {
    request.getItems().getFirst().setDrinkDetailId(null);
    assertSingleViolation("items[0].drinkDetailId", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenQuantityIsNull_TC053() {
    request.getItems().getFirst().setQuantity(null);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenQuantityIsZeroOrNegative_TC054() {
    request.getItems().getFirst().setQuantity(0);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MIN);

    request.getItems().getFirst().setQuantity(-1);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenQuantityExceedsMax_TC055() {
    request.getItems().getFirst().setQuantity(1000);
    assertSingleViolation("items[0].quantity", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIsNull_TC056() {
    request.getItems().getFirst().setUnitPrice(null);
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.FIELD_REQUIRED);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIsZeroOrNegative_TC057() {
    request.getItems().getFirst().setUnitPrice(BigDecimal.ZERO);
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.PRICE_MIN);

    request.getItems().getFirst().setUnitPrice(new BigDecimal("-1.00"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.PRICE_MIN);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceFractionExceedsTwoDecimals_TC058() {
    request.getItems().getFirst().setUnitPrice(new BigDecimal("25000.123"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenUnitPriceIntegerDigitsExceed16_TC059() {
    request.getItems().getFirst().setUnitPrice(new BigDecimal("10000000000000000.00"));
    assertSingleViolation("items[0].unitPrice", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenItemNoteExceeds255Characters_TC060() {
    request.getItems().getFirst().setNote("A".repeat(256));
    assertSingleViolation("items[0].note", ValidationMessage.Msg.SIZE_MAX);
  }

  @Test
  void requestValidation_Fails_WhenAllItemRequiredFieldsAreNull_TC061() {
    request.setItems(List.of(new InvoiceItemRequest()));
    Set<ConstraintViolation<EditInvoiceRequest>> violations = validator.validate(request);
    assertEquals(3, violations.size());
  }
}
