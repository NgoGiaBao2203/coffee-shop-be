package coffee.api.services.services_implement.invoice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coffee.api.constants.ErrorMessage;
import coffee.api.constants.ValidationMessage;
import coffee.api.dto.request.invoice.PrintInvoiceRequest;
import coffee.api.dto.result.InvoicePrintResult;
import coffee.api.dto.result.InvoiceReceiptResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import coffee.api.mapper.PrintInvoiceMapper;
import coffee.api.services.services_implement.invoice.common.InvoiceReceiptPdfWriter;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrintInvoiceServiceImplTest {

  private static final byte[] PDF = {37, 80, 68, 70};

  @Mock private PrintInvoiceMapper printInvoiceMapper;
  @Mock private CommonMapper commonMapper;
  @Mock private CreateInvoiceMapper createInvoiceMapper;
  @Mock private InvoiceReceiptPdfWriter pdfWriter;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private PrintInvoiceServiceImpl service;
  private PrintInvoiceRequest validRequest;
  private InvoiceReceiptResult receipt;
  private UUID invoiceId;
  private UUID shopId;
  private UUID userId;

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
    service =
        new PrintInvoiceServiceImpl(
            printInvoiceMapper, new InvoiceValidator(createInvoiceMapper, commonMapper), pdfWriter);
    invoiceId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    userId = UUID.fromString("a1000000-0000-0000-0000-000000000001");

    validRequest = new PrintInvoiceRequest();
    validRequest.setInvoiceId(invoiceId);

    receipt = new InvoiceReceiptResult();
    receipt.setInvoiceId(invoiceId);
    receipt.setInvoiceCode("INV-20261109-1A2B3C4D");
    receipt.setShopId(shopId);
    receipt.setStatus(1);
  }

  // =========================================================================
  // HELPERS
  // =========================================================================

  private void stubReceiptAndShop(String role) {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    if (!"OWNER".equals(role)) {
      when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(true);
    }
  }

  private void assertInvalid(String role, UUID assignedShopId, String message) {
    var error =
        assertThrows(
            InvalidRequestException.class,
            () -> service.process(validRequest, role, assignedShopId, userId));
    assertEquals(message, error.getMessage());
    verify(pdfWriter, never()).write(any());
  }

  // =========================================================================
  // SERVICE PROCESS - SUCCESS
  // =========================================================================

  @ParameterizedTest
  @ValueSource(strings = {"OWNER", "MANAGER", "STAFF"})
  void process_ReturnsPdf_WhenInvoiceIsPaid_TC001(String role) {
    stubReceiptAndShop(role);
    when(pdfWriter.write(receipt)).thenReturn(PDF);

    InvoicePrintResult result = service.process(validRequest, role, shopId, userId);

    assertEquals("hoa-don_INV-20261109-1A2B3C4D.pdf", result.getFileName());
    assertArrayEquals(PDF, result.getContent());
    verify(pdfWriter).write(receipt);
  }

  @Test
  void process_ReturnsPdf_WhenOwnerPrintsInvoiceOfAnyShop_TC002() {
    stubReceiptAndShop("OWNER");
    when(pdfWriter.write(receipt)).thenReturn(PDF);

    InvoicePrintResult result = service.process(validRequest, "OWNER", null, userId);

    assertArrayEquals(PDF, result.getContent());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  // =========================================================================
  // SERVICE PROCESS - FAILURES
  // =========================================================================

  @Test
  void process_ThrowsNotFound_WhenInvoiceDoesNotExist_TC003() {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(null);

    var error =
        assertThrows(
            DataNotFoundException.class,
            () -> service.process(validRequest, "OWNER", shopId, userId));

    assertEquals(ErrorMessage.INVOICE_NOT_FOUND, error.getMessage());
    verify(pdfWriter, never()).write(any());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {0, 2})
  void process_ThrowsInvalid_WhenInvoiceIsNotPaid_TC004(Integer status) {
    receipt.setStatus(status);
    stubReceiptAndShop("STAFF");

    assertInvalid("STAFF", shopId, "Only paid invoices can be printed");
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_ThrowsInvalid_WhenUserPrintsInvoiceOfAnotherShop_TC005(String role) {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);

    assertInvalid(
        role,
        UUID.fromString("b1000000-0000-0000-0000-000000000099"),
        ErrorMessage.SHOP_ACCESS_DENIED);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MANAGER", "STAFF"})
  void process_ThrowsInvalid_WhenUserHasNoAssignedShop_TC006(String role) {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);

    assertInvalid(role, null, ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP);
  }

  @Test
  void process_ThrowsInvalid_WhenStaffMembershipIsMissing_TC007() {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(userId, shopId)).thenReturn(false);

    assertInvalid("STAFF", shopId, ErrorMessage.SHOP_ACCESS_DENIED);
  }

  @Test
  void process_ThrowsNotFound_WhenShopIsDeleted_TC008() {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(false);

    assertThrows(
        DataNotFoundException.class, () -> service.process(validRequest, "OWNER", shopId, userId));
    verify(pdfWriter, never()).write(any());
  }

  @Test
  void process_ThrowsInvalid_WhenRoleIsUnknown_TC009() {
    when(printInvoiceMapper.findInvoiceForPrint(invoiceId)).thenReturn(receipt);

    assertInvalid("CASHIER", shopId, "You do not have permission to create invoices");
  }

  // =========================================================================
  // REQUEST VALIDATION
  // =========================================================================

  @Test
  void requestValidation_Success_WhenInvoiceIdIsPresent_TC010() {
    assertEquals(0, validator.validate(validRequest).size());
  }

  @Test
  void requestValidation_Fails_WhenInvoiceIdIsNull_TC011() {
    validRequest.setInvoiceId(null);

    var violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
