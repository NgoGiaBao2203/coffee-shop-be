package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
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
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EditDrinksServiceImplTest {

  @Mock private CommonMapper commonMapper;
  @Mock private UpdateDrinkMapper editDrinkMapper;
  @Mock private IFileStorageService fileStorageService;

  @InjectMocks private EditDrinksServiceImpl editDrinksService;

  private Validator validator;
  private EditDrinksRequest validRequest;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private UUID drinkId;
  private UUID drinkCategoryId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    drinkId = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
    drinkCategoryId = UUID.fromString("11111111-0000-0000-0000-000000000001");

    validRequest = new EditDrinksRequest();
    validRequest.setDrinkId(drinkId);
    validRequest.setDrinkCategoryId(drinkCategoryId);
    validRequest.setDrinkName("Cà Phê Muối Đặc Biệt");
    validRequest.setStatus(1);
    validRequest.setImageUrl("https://example.com/drinks/old_image.png");
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenOwnerAndNoNewImageProvided_TC001() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(fileStorageService, never()).storeDrinkImage(any());
    verify(fileStorageService, never()).deleteDrinkImage(any());
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq("https://example.com/drinks/old_image.png"),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenManagerAndValidShopMembership_TC002() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkDrinkExisted(drinkId, Roles.MANAGER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.MANAGER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.MANAGER.getValue(), currentUserShopId));

    verify(commonMapper).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq("https://example.com/drinks/old_image.png"),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.MANAGER.getValue()));
  }

  @Test
  void process_Success_WhenNewImageUploadedAndOldImageExists_TC003() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String oldImageUrl = "https://example.com/drinks/old_image.png";
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(oldImageUrl);
    when(fileStorageService.storeDrinkImage(newImageFile)).thenReturn(newUploadedUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest,
                newImageFile,
                currentUserId,
                Roles.OWNER.getValue(),
                currentUserShopId));

    verify(fileStorageService).storeDrinkImage(newImageFile);
    verify(fileStorageService).deleteDrinkImage(oldImageUrl);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(newUploadedUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenNewImageUploadedAndOldImageIsNull_TC004() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(null);
    when(fileStorageService.storeDrinkImage(newImageFile)).thenReturn(newUploadedUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest,
                newImageFile,
                currentUserId,
                Roles.OWNER.getValue(),
                currentUserShopId));

    verify(fileStorageService).storeDrinkImage(newImageFile);
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
  }

  @Test
  void process_Success_WhenNewImageUploadedAndOldImageIsBlank_TC004A() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn("   ");
    when(fileStorageService.storeDrinkImage(newImageFile)).thenReturn(newUploadedUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest,
                newImageFile,
                currentUserId,
                Roles.OWNER.getValue(),
                currentUserShopId));

    verify(fileStorageService).storeDrinkImage(newImageFile);
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
  }

  @Test
  void process_Success_WhenImageFileProvidedButEmpty_TC004B() {
    MockMultipartFile emptyImageFile =
        new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest,
                emptyImageFile,
                currentUserId,
                Roles.OWNER.getValue(),
                currentUserShopId));

    verify(fileStorageService, never()).storeDrinkImage(any());
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
    verify(editDrinkMapper, never()).getDrinkImageUrl(any(), anyString(), any());
  }

  @Test
  void process_Success_WhenNoNewImageAndImageUrlIsBlank_RetrievesCurrentImageUrl_TC005() {
    validRequest.setImageUrl("   ");
    String currentDbImageUrl = "https://example.com/drinks/current_db_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(currentDbImageUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    verify(editDrinkMapper).getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(currentDbImageUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenNoNewImageAndImageUrlIsNull_RetrievesCurrentImageUrl_TC005A() {
    validRequest.setImageUrl(null);
    String currentDbImageUrl = "https://example.com/drinks/current_db_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(currentDbImageUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    verify(editDrinkMapper).getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), currentUserShopId);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(currentDbImageUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenDrinkNameIsNull_DefaultsToEmptyString_TC006() {
    validRequest.setDrinkName(null);

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "")).thenReturn(false);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("", validRequest.getDrinkName());
    verify(editDrinkMapper).checkDrinkNameExistedForEdit(drinkId, "");
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq(""),
            eq("https://example.com/drinks/old_image.png"),
            eq(1),
            eq(drinkCategoryId),
            eq(currentUserShopId),
            eq(Roles.OWNER.getValue()));
  }

  // =========================================================================
  // SERVICE PROCESS - ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsInvalidRequestException_WhenRequestIsNull_TC007() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    null, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Drink ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkIdIsNull_TC008() {
    validRequest.setDrinkId(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Drink ID is required", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerHasNoAssignedShop_TC009() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.MANAGER.getValue(), null));

    assertEquals("Manager is not assigned to any shop", exception.getMessage());
    verifyNoInteractions(commonMapper);
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC010() {
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest,
                    null,
                    currentUserId,
                    Roles.MANAGER.getValue(),
                    currentUserShopId));

    assertEquals("You do not have permission to access this shop", exception.getMessage());
    verify(commonMapper, never()).checkDrinkExisted(any(), anyString(), any());
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkNotFound_TC011() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(drinkId, exception.getId());
    verify(commonMapper, never()).checkCategoryExisted(any(), anyString(), any());
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenCategoryNotFound_TC012() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(false);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(drinkCategoryId, exception.getId());
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkNameAlreadyExists_TC013() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(commonMapper.checkCategoryExisted(
            drinkCategoryId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(true);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Drink name is existed", exception.getMessage());
    verify(editDrinkMapper, never()).updateDrink(any(), any(), any(), any(), any(), any(), any());
  }

  // =========================================================================
  // REQUEST VALIDATION CASES
  // =========================================================================

  @Test
  void request_ValidationSuccess_WhenValidRequest_TC014() {
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationFails_WhenDrinkNameContainsDisallowedCharacters_TC015() {
    validRequest.setDrinkName("Drink @ 2026!");
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.SPECIAL_CHARACTERS.equals(v.getMessage())));
  }

  @Test
  void request_ValidationFails_WhenDrinkNameExceeds100Characters_TC016() {
    validRequest.setDrinkName("A".repeat(101));
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(
        violations.stream().anyMatch(v -> ValidationMessage.Msg.SIZE_MAX.equals(v.getMessage())));
  }

  @Test
  void request_ValidationFails_WhenStatusIsInvalid_TC017() {
    for (Integer invalidStatus : new Integer[] {-1, 2}) {
      validRequest.setStatus(invalidStatus);
      Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

      assertTrue(
          violations.stream()
              .anyMatch(v -> ValidationMessage.Msg.STATUS_INVALID.equals(v.getMessage())));
    }
  }
}
