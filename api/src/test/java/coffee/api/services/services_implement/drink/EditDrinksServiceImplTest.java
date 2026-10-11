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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
  private UUID drinkShopId;
  private UUID drinkId;
  private UUID drinkCategoryId;

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    currentUserId = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    drinkShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    drinkId = UUID.fromString("150e940d-8363-4dff-9b2e-3e8291dab04f");
    drinkCategoryId = UUID.fromString("11111111-0000-0000-0000-000000000001");

    validRequest = new EditDrinksRequest();
    validRequest.setDrinkId(drinkId);
    validRequest.setDrinkCategoryId(drinkCategoryId);
    validRequest.setDrinkName("Cà Phê Muối Đặc Biệt");
    validRequest.setStatus(1);
    validRequest.setImageUrl("https://example.com/drinks/old_image.png");

    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void tearDown() {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  // =========================================================================
  // SERVICE PROCESS - NORMAL & BUSINESS CASES
  // =========================================================================

  @Test
  void process_Success_WhenOwnerAndNoNewImageProvided_TC001() {
    UUID distinctDrinkShopId = UUID.fromString("c2000000-0000-0000-0000-000000000002");

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(distinctDrinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, distinctDrinkShopId))
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
            eq(distinctDrinkShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenManagerAndValidShopMembership_TC002() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.MANAGER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
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
            eq(drinkShopId),
            eq(Roles.MANAGER.getValue()));
  }

  @Test
  void process_Success_WhenNewImageUploaded_DeletesOldImageAfterCommit_TC003() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String oldImageUrl = "https://example.com/drinks/old_image.png";
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
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
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(newUploadedUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(drinkShopId),
            eq(Roles.OWNER.getValue()));

    // Old image is not deleted before transaction commit
    verify(fileStorageService, never()).deleteDrinkImage(oldImageUrl);

    // Trigger post-commit synchronization callbacks
    List<TransactionSynchronization> syncs =
        TransactionSynchronizationManager.getSynchronizations();
    assertFalse(syncs.isEmpty());
    syncs.forEach(TransactionSynchronization::afterCommit);

    // Old image is deleted only after transaction commit
    verify(fileStorageService).deleteDrinkImage(oldImageUrl);
  }

  @Test
  void
      process_Success_WhenNewImageUploadedAndOldImageIsBlank_DoesNotDeleteOldImageOnCommit_TC003_BlankOldImage() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
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

    List<TransactionSynchronization> syncs =
        TransactionSynchronizationManager.getSynchronizations();
    assertFalse(syncs.isEmpty());
    syncs.forEach(TransactionSynchronization::afterCommit);

    verify(fileStorageService, never()).deleteDrinkImage(anyString());
  }

  @Test
  void process_Success_WhenNewImageUploadedAndRolledBack_CleansUpNewImage_TC003A() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String oldImageUrl = "https://example.com/drinks/old_image.png";
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
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

    // Simulate rollback completion
    List<TransactionSynchronization> syncs =
        TransactionSynchronizationManager.getSynchronizations();
    assertFalse(syncs.isEmpty());
    syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    // Old image is safe, uploaded new image is deleted
    verify(fileStorageService, never()).deleteDrinkImage(oldImageUrl);
    verify(fileStorageService).deleteDrinkImage(newUploadedUrl);
  }

  @Test
  void process_Success_WhenNewImageUploadedAndCommittedStatus_DoesNotCleanUpNewImage_TC003B() {
    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String oldImageUrl = "https://example.com/drinks/old_image.png";
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
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

    // Simulate committed status (not rolled back)
    List<TransactionSynchronization> syncs =
        TransactionSynchronizationManager.getSynchronizations();
    assertFalse(syncs.isEmpty());
    syncs.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

    verify(fileStorageService, never()).deleteDrinkImage(newUploadedUrl);
  }

  @Test
  void process_Success_WhenNewImageUploadedAndSynchronizationInactive_TC003C() {
    TransactionSynchronizationManager.clearSynchronization();

    MockMultipartFile newImageFile =
        new MockMultipartFile("image", "new_drink.jpg", "image/jpeg", "fake-image".getBytes());
    String oldImageUrl = "https://example.com/drinks/old_image.png";
    String newUploadedUrl = "https://example.com/drinks/new_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
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
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
  }

  @Test
  void process_Success_WhenImageFileProvidedButEmpty_RetainsProvidedImageUrl_TC004() {
    MockMultipartFile emptyImageFile =
        new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
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
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq("https://example.com/drinks/old_image.png"),
            eq(1),
            eq(drinkCategoryId),
            eq(drinkShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenNoNewImageAndImageUrlIsBlank_RetrievesCurrentImageUrl_TC005() {
    validRequest.setImageUrl("   ");
    String currentDbImageUrl = "https://example.com/drinks/current_db_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
        .thenReturn(currentDbImageUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    verify(editDrinkMapper).getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(currentDbImageUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(drinkShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenNoNewImageAndImageUrlIsNull_RetrievesCurrentImageUrl_TC005A() {
    validRequest.setImageUrl(null);
    String currentDbImageUrl = "https://example.com/drinks/current_db_image.png";

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
    when(editDrinkMapper.checkDrinkNameExistedForEdit(drinkId, "Cà Phê Muối Đặc Biệt"))
        .thenReturn(false);
    when(editDrinkMapper.getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId))
        .thenReturn(currentDbImageUrl);

    assertDoesNotThrow(
        () ->
            editDrinksService.process(
                validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    verify(editDrinkMapper).getDrinkImageUrl(drinkId, Roles.OWNER.getValue(), drinkShopId);
    verify(editDrinkMapper)
        .updateDrink(
            eq(drinkId),
            eq("Cà Phê Muối Đặc Biệt"),
            eq(currentDbImageUrl),
            eq(1),
            eq(drinkCategoryId),
            eq(drinkShopId),
            eq(Roles.OWNER.getValue()));
  }

  @Test
  void process_Success_WhenDrinkNameIsNull_DefaultsToEmptyString_TC006() {
    validRequest.setDrinkName(null);

    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
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
            eq(drinkShopId),
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
  void process_ThrowsDataNotFoundException_WhenDrinkNotFound_TC009() {
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
    verify(editDrinkMapper, never()).getShopIdByDrinkId(any());
    verifyNoInteractions(editDrinkMapper);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkShopIdNotFound_TC010() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(null);

    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(drinkId, exception.getId());
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerHasNoAssignedShop_TC011() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.MANAGER.getValue(), null)).thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.MANAGER.getValue(), null));

    assertEquals("Manager is not assigned to any shop", exception.getMessage());
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
    verify(editDrinkMapper, never()).updateDrink(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerEditsDrinkOfDifferentShop_TC012() {
    UUID differentShopId = UUID.fromString("c3000000-0000-0000-0000-000000000003");

    when(commonMapper.checkDrinkExisted(drinkId, Roles.MANAGER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(differentShopId);

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

    assertEquals(
        "You do not have permission to edit a drink from another shop", exception.getMessage());
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopMembershipInactive_TC013() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.MANAGER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
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
    verify(commonMapper, never()).checkCategoryExistsInShop(any(), any());
    verifyNoInteractions(fileStorageService);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenCategoryDoesNotBelongToDrinkShop_TC014() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(false);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserId, Roles.OWNER.getValue(), currentUserShopId));

    assertEquals("Category does not belong to the drink's shop", exception.getMessage());
    verify(editDrinkMapper, never()).checkDrinkNameExistedForEdit(any(), anyString());
    verify(editDrinkMapper, never()).updateDrink(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenDrinkNameAlreadyExists_TC015() {
    when(commonMapper.checkDrinkExisted(drinkId, Roles.OWNER.getValue(), currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getShopIdByDrinkId(drinkId)).thenReturn(drinkShopId);
    when(commonMapper.checkCategoryExistsInShop(drinkCategoryId, drinkShopId)).thenReturn(true);
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
  void request_ValidationSuccess_WhenValidRequest_TC016() {
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);
    assertTrue(violations.isEmpty());
  }

  @Test
  void request_ValidationFails_WhenDrinkNameContainsDisallowedCharacters_TC017() {
    validRequest.setDrinkName("Drink @ 2026!");
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(
        violations.stream()
            .anyMatch(v -> ValidationMessage.Msg.SPECIAL_CHARACTERS.equals(v.getMessage())));
  }

  @Test
  void request_ValidationFails_WhenDrinkNameExceeds100Characters_TC018() {
    validRequest.setDrinkName("A".repeat(101));
    Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

    assertTrue(
        violations.stream().anyMatch(v -> ValidationMessage.Msg.SIZE_MAX.equals(v.getMessage())));
  }

  @Test
  void request_ValidationFails_WhenStatusIsInvalid_TC019() {
    for (Integer invalidStatus : new Integer[] {-1, 2}) {
      validRequest.setStatus(invalidStatus);
      Set<ConstraintViolation<EditDrinksRequest>> violations = validator.validate(validRequest);

      assertTrue(
          violations.stream()
              .anyMatch(v -> ValidationMessage.Msg.STATUS_INVALID.equals(v.getMessage())));
    }
  }
}
