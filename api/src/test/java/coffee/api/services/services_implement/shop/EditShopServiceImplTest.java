package coffee.api.services.services_implement.shop;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop.EditShopRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.UpdateShopMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
public class EditShopServiceImplTest {

  @Mock private UpdateShopMapper updateShopMapper;

  @InjectMocks private EditShopServiceImpl editShopBranchService;

  private Validator validator;
  private EditShopRequest validRequest;
  private UUID shopId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    shopId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    validRequest = new EditShopRequest();
    validRequest.setShopName("Coffee Shop - Chi nhánh 1");
    validRequest.setAddress("123 Đường 3/2, Quận Ninh Kiều, Cần Thơ");
    validRequest.setPhoneNumber("02923888999");
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_TC001() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(true);
    when(updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopMapper.checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editShopBranchService.process(shopId, validRequest));

    // Assert
    verify(updateShopMapper, times(1)).checkShopExistedById(shopId);
    verify(updateShopMapper, times(1))
        .checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName());
    verify(updateShopMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress());
    verify(updateShopMapper, times(1))
        .updateShop(
            eq(shopId),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()));
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC005() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class, () -> editShopBranchService.process(shopId, validRequest));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(shopId, exception.getId());
    verify(updateShopMapper, times(1)).checkShopExistedById(shopId);
    verify(updateShopMapper, never())
        .checkShopExistedByNameExceptCurrent(any(UUID.class), anyString());
    verify(updateShopMapper, never()).updateShop(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenShopNameExistsInAnotherBranch_TC006() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(true);
    when(updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(
            UserExistException.class, () -> editShopBranchService.process(shopId, validRequest));

    assertEquals("Shop name is existed", exception.getMessage());
    verify(updateShopMapper, times(1)).checkShopExistedById(shopId);
    verify(updateShopMapper, times(1))
        .checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName());
    verify(updateShopMapper, never())
        .checkShopExistedByAddressExceptCurrent(any(UUID.class), anyString());
    verify(updateShopMapper, never()).updateShop(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenAddressExistsInAnotherBranch_TC007() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(true);
    when(updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopMapper.checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(
            UserExistException.class, () -> editShopBranchService.process(shopId, validRequest));

    assertEquals("Address is existed", exception.getMessage());
    verify(updateShopMapper, times(1)).checkShopExistedById(shopId);
    verify(updateShopMapper, times(1))
        .checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName());
    verify(updateShopMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress());
    verify(updateShopMapper, never()).updateShop(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC009() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(true);
    when(updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopMapper.checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database update error") {})
        .when(updateShopMapper)
        .updateShop(any(), any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> editShopBranchService.process(shopId, validRequest));

    assertEquals("Database update error", exception.getMessage());
    verify(updateShopMapper, times(1))
        .updateShop(
            eq(shopId),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()));
  }

  @Test
  void process_ThrowsException_WhenCheckShopExistedByIdFails_TC010() {
    // Arrange
    when(updateShopMapper.checkShopExistedById(any()))
        .thenThrow(new RuntimeException("Database connection timeout"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> editShopBranchService.process(shopId, validRequest));

    assertEquals("Database connection timeout", exception.getMessage());
    verify(updateShopMapper, never()).updateShop(any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST VALIDATION - NORMAL CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenAllFieldsAreValid_TC011() {
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsNull_TC012() {
    validRequest.setPhoneNumber(null);
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsEmptyString_TC013() {
    validRequest.setPhoneNumber("");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsWhitespace_TC014() {
    validRequest.setPhoneNumber("     ");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs10Digits_TC015() {
    validRequest.setPhoneNumber("0912345678");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationSuccess_WhenPhoneNumberIs11Digits_TC016() {
    validRequest.setPhoneNumber("02923999111");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: SHOP NAME
  // =========================================================================

  @Test
  void process_ValidationFails_WhenShopNameIsNull_TC018() {
    validRequest.setShopName(null);
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameIsBlank_TC019() {
    validRequest.setShopName("   ");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenShopNameExceeds100Characters_TC020() {
    validRequest.setShopName("A".repeat(101));
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenShopNameHas100Characters_TC020A() {
    validRequest.setShopName("A".repeat(100));

    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenShopNameContainsSpecialCharacters_TC021() {
    validRequest.setShopName("Hoa Yên Coffee @ 2026!");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: ADDRESS
  // =========================================================================

  @Test
  void process_ValidationFails_WhenAddressIsNull_TC022() {
    validRequest.setAddress(null);
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressIsBlank_TC023() {
    validRequest.setAddress(" ");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenAddressExceeds255Characters_TC024() {
    validRequest.setAddress("A".repeat(256));
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationSuccess_WhenAddressHas255Characters_TC024A() {
    validRequest.setAddress("A".repeat(255));

    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void process_ValidationFails_WhenAddressContainsInvalidSpecialCharacters_TC025() {
    validRequest.setAddress("123 Đường 30/4 $ Phường Hưng Lợi * Ninh Kiều");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  // =========================================================================
  // REQUEST VALIDATION - ABNORMAL CASES: PHONE NUMBER
  // =========================================================================

  @Test
  void process_ValidationFails_WhenPhoneNumberContainsLettersOrSpecialChars_TC026() {
    validRequest.setPhoneNumber("091234abcd");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberDoesNotStartWithZero_TC027() {
    validRequest.setPhoneNumber("1912345678");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_NUMBER_INVALID, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberIsUnder10Digits_TC028() {
    validRequest.setPhoneNumber("091234567");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }

  @Test
  void process_ValidationFails_WhenPhoneNumberExceeds11Digits_TC029() {
    validRequest.setPhoneNumber("091234567890");
    Set<ConstraintViolation<EditShopRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.PHONE_INVALID_LENGTH, violations.iterator().next().getMessage());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenExistenceCheckReturnsNull_TC030() {
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(null);

    assertThrows(
        DataNotFoundException.class, () -> editShopBranchService.process(shopId, validRequest));

    verify(updateShopMapper, never())
        .checkShopExistedByNameExceptCurrent(any(UUID.class), anyString());
    verify(updateShopMapper, never()).updateShop(any(), any(), any(), any());
  }

  @Test
  void process_Success_WhenDuplicateChecksReturnNull_TC031() {
    when(updateShopMapper.checkShopExistedById(shopId)).thenReturn(true);
    when(updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, validRequest.getShopName()))
        .thenReturn(null);
    when(updateShopMapper.checkShopExistedByAddressExceptCurrent(shopId, validRequest.getAddress()))
        .thenReturn(null);

    assertDoesNotThrow(() -> editShopBranchService.process(shopId, validRequest));

    verify(updateShopMapper)
        .updateShop(
            shopId,
            validRequest.getShopName(),
            validRequest.getAddress(),
            validRequest.getPhoneNumber());
  }
}
