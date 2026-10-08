package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CreateDrinkServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private CreateDrinkMapper createDrinkMapper;
  @Mock private IFileStorageService fileStorageService;

  @InjectMocks private CreateDrinkServiceImpl createDrinkService;

  private Validator validator;
  private CreateDrinksRequest validRequest;
  private UUID currentUserId;
  private UUID currentShopId;
  private UUID drinkCategoryId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    drinkCategoryId = UUID.fromString("c1000000-0000-0000-0000-000000000001");

    validRequest = new CreateDrinksRequest();
    validRequest.setDrinkName("Trà Sữa Oolong Lài Kem Mặn");
    validRequest.setPrice(new BigDecimal("52000.00"));
    validRequest.setSize("L");
    validRequest.setImageUrl("https://example.com/images/oolonglaikemman.png");
    validRequest.setStatus(1);
    validRequest.setShopId(currentShopId);
    validRequest.setDrinkCategoryId(drinkCategoryId);
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenManagerAndValidInputs_TC001() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    verify(commonMapper).checkShopExisted(currentShopId);
    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentShopId);
    verify(commonMapper).checkCategoryExistsInShop(drinkCategoryId, currentShopId);
    verify(commonMapper)
        .checkDrinkNameExisted(isNull(), eq(validRequest.getDrinkName()), eq(currentShopId));
    verify(createDrinkMapper, times(1))
        .insertDrink(
            any(UUID.class),
            eq(drinkCategoryId),
            eq(currentShopId),
            eq(validRequest.getDrinkName()),
            eq(validRequest.getImageUrl()),
            eq(1));
    verify(createDrinkMapper, times(1))
        .insertDrinkDetail(any(UUID.class), eq("L"), eq(new BigDecimal("52000.00")));
    verify(fileStorageService, never()).storeDrinkImage(any());
  }

  @Test
  void process_Success_WhenOwnerSkipsManagerChecks_TC002() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, null, currentUserId, currentShopId, Roles.OWNER.getValue()));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(createDrinkMapper).insertDrink(any(UUID.class), any(), any(), any(), any(), any());
    verify(createDrinkMapper).insertDrinkDetail(any(UUID.class), any(), any());
  }

  @Test
  void process_Success_WhenImageProvided_TC003() {
    MockMultipartFile mockFile =
        new MockMultipartFile("image", "drink.png", "image/png", "sample content".getBytes());
    String uploadedUrl = "/uploads/drinks/saved-uuid.png";

    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);
    when(fileStorageService.storeDrinkImage(mockFile)).thenReturn(uploadedUrl);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, mockFile, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    verify(fileStorageService, times(1)).storeDrinkImage(mockFile);
    verify(createDrinkMapper)
        .insertDrink(
            any(UUID.class),
            eq(drinkCategoryId),
            eq(currentShopId),
            eq(validRequest.getDrinkName()),
            eq(uploadedUrl),
            eq(1));
  }

  @Test
  void process_Success_NormalizesTrimmedNameAndUppercaseSize_TC004() {
    validRequest.setDrinkName("  Trà Sữa Matcha  ");
    validRequest.setSize(" xl ");

    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(isNull(), eq("Trà Sữa Matcha"), eq(currentShopId)))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Trà Sữa Matcha", validRequest.getDrinkName());
    assertEquals("XL", validRequest.getSize());
    verify(createDrinkMapper).insertDrinkDetail(any(UUID.class), eq("XL"), any(BigDecimal.class));
  }

  @Test
  void process_Success_WhenDrinkNameIsNull_UsesEmptyNameForDuplicateCheck_TC004_1() {
    validRequest.setDrinkName(null);

    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(isNull(), eq(""), eq(currentShopId))).thenReturn(false);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("", validRequest.getDrinkName());
    verify(commonMapper).checkDrinkNameExisted(isNull(), eq(""), eq(currentShopId));
    verify(createDrinkMapper).insertDrink(any(UUID.class), any(), any(), eq(""), any(), any());
  }

  @Test
  void process_Success_WhenImageFileIsEmpty_SkipsStorage_TC004_2() {
    MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/png", new byte[0]);

    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            createDrinkService.process(
                validRequest, emptyFile, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    verify(fileStorageService, never()).storeDrinkImage(any());
    verify(createDrinkMapper)
        .insertDrink(
            any(UUID.class),
            eq(drinkCategoryId),
            eq(currentShopId),
            eq(validRequest.getDrinkName()),
            eq(validRequest.getImageUrl()),
            eq(1));
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC005() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    null, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenShopIdIsNull_TC006() {
    validRequest.setShopId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Shop ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(createDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopNotFound_TC007() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(currentShopId, exception.getId());
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
    verifyNoInteractions(createDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerHasNoShop_TC008() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, null, Roles.MANAGER.getValue()));

    assertEquals("Manager is not assigned to any shop", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMismatch_TC009() {
    UUID otherShopId = UUID.randomUUID();
    validRequest.setShopId(otherShopId);
    when(commonMapper.checkShopExisted(otherShopId)).thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerNotMemberOfShop_TC010() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryNotInShop_TC011() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(drinkCategoryId, exception.getId());
    verify(commonMapper, never()).checkDrinkNameExisted(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkNameAlreadyExists_TC012() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Drink name is existed", exception.getMessage());
    verifyNoInteractions(createDrinkMapper);
    verify(fileStorageService, never()).storeDrinkImage(any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenSizeBlankAfterTrim_TC013() {
    validRequest.setSize("   ");
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Drink size is required", exception.getMessage());
    verifyNoInteractions(createDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenSizeIsNull_TC013_1() {
    validRequest.setSize(null);
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("Drink size is required", exception.getMessage());
    verifyNoInteractions(createDrinkMapper);
  }

  @Test
  void process_ThrowsDataAccessException_WhenInsertDrinkFails_TC014() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);
    doThrow(new DataAccessException("insert drinks failed") {})
        .when(createDrinkMapper)
        .insertDrink(any(UUID.class), any(), any(), any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("insert drinks failed", exception.getMessage());
    verify(createDrinkMapper, never()).insertDrinkDetail(any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenInsertDrinkDetailFails_TC015() {
    when(commonMapper.checkShopExisted(currentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, currentShopId)).thenReturn(true);
    when(commonMapper.checkDrinkNameExisted(
            isNull(), eq(validRequest.getDrinkName()), eq(currentShopId)))
        .thenReturn(false);
    doThrow(new DataAccessException("insert drink detail failed") {})
        .when(createDrinkMapper)
        .insertDrinkDetail(any(UUID.class), anyString(), any(BigDecimal.class));

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () ->
                createDrinkService.process(
                    validRequest, null, currentUserId, currentShopId, Roles.MANAGER.getValue()));

    assertEquals("insert drink detail failed", exception.getMessage());
    verify(createDrinkMapper, times(1))
        .insertDrink(any(UUID.class), any(), any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST VALIDATION CASES
  // =========================================================================

  @Test
  void request_ValidationSuccess_WhenDrinkNameContainsAllowedChars_TC015() {
    validRequest.setDrinkName("Cà-phê & Trà (Nóng/Lạnh), số 1. 'Espresso'");
    Set<ConstraintViolation<CreateDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationFails_WhenDrinkNameContainsDisallowedChars_TC016() {
    validRequest.setDrinkName("Drink @ 2026!");
    Set<ConstraintViolation<CreateDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(
        ValidationMessage.Msg.SPECIAL_CHARACTERS, violations.iterator().next().getMessage());
  }

  @Test
  void request_ValidationFails_WhenDrinkNameExceeds100Chars_TC017() {
    validRequest.setDrinkName("A".repeat(101));
    Set<ConstraintViolation<CreateDrinksRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.SIZE_MAX, violations.iterator().next().getMessage());
  }

  @Test
  void request_ValidationFails_WhenDrinkNameIsNull_TC018() {
    validRequest.setDrinkName(null);
    Set<ConstraintViolation<CreateDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.FIELD_REQUIRED.equals(v.getMessage())));
  }

  @Test
  void request_ValidationSuccess_WhenPriceIsGreaterThanOne_TC020() {
    validRequest.setPrice(new BigDecimal("1.01"));
    assertTrue(validator.validate(validRequest).isEmpty());
  }

  @Test
  void request_ValidationFails_WhenPriceIsOneZeroOrNegative_TC021() {
    for (BigDecimal price : new BigDecimal[] {new BigDecimal("0.0"), new BigDecimal("-1.0")}) {
      validRequest.setPrice(price);
      Set<ConstraintViolation<CreateDrinksRequest>> violations = validator.validate(validRequest);

      assertTrue(
          violations.stream()
              .anyMatch(v -> ValidationMessage.Msg.PRICE_MIN.equals(v.getMessage())));
    }
  }
}
