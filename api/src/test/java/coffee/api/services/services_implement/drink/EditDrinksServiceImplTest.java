package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class EditDrinksServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private UpdateDrinkMapper editDrinkMapper;

  @Mock private IFileStorageService fileStorageService;

  @InjectMocks private EditDrinksServiceImpl editDrinksService;

  private EditDrinksRequest validRequest;
  private String currentUserRoleName;
  private UUID currentUserShopId;

  @BeforeEach
  void setUp() {
    validRequest = new EditDrinksRequest();
    validRequest.setDrinkId(UUID.fromString("d1000000-0000-0000-0000-000000000001"));
    validRequest.setDrinkName("Cà Phê Sữa");
    validRequest.setImageUrl("https://example.com/cf-sua.jpg");
    validRequest.setStatus(1);
    validRequest.setDrinkCategoryId(UUID.fromString("c1000000-0000-0000-0000-000000000001"));
    validRequest.setPrice(25000.0f);
    validRequest.setSize("M");

    currentUserRoleName = "OWNER";
    currentUserShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    // Act
    editDrinksService.process(validRequest, null, currentUserRoleName, currentUserShopId);

    // Assert
    verify(commonMapper, times(1))
        .checkDrinkExisted(validRequest.getDrinkId(), currentUserRoleName, currentUserShopId);

    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            validRequest.getImageUrl(),
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenCheckDrinkExistedReturnsNull_TC002() {
    // Arrange: Boolean.FALSE.equals(null) is false, so it moves to updateDrink
    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(null);

    // Act
    editDrinksService.process(validRequest, null, currentUserRoleName, currentUserShopId);

    // Assert
    verify(editDrinkMapper, times(1))
        .updateDrink(
            eq(validRequest.getDrinkId()),
            eq(validRequest.getDrinkName()),
            eq(validRequest.getImageUrl()),
            eq(validRequest.getStatus()),
            eq(validRequest.getDrinkCategoryId()),
            eq(validRequest.getPrice()),
            eq(validRequest.getSize()),
            eq(currentUserShopId),
            eq(currentUserRoleName));
  }

  @Test
  void process_Success_WithDifferentSizeAndPriceValues_TC003() {
    // Arrange
    validRequest.setSize("L");
    validRequest.setPrice(35000.0f);
    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    // Act
    editDrinksService.process(validRequest, null, currentUserRoleName, currentUserShopId);

    // Assert
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            validRequest.getImageUrl(),
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            35000.0f,
            "L",
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenImageFileProvidedAndOldImageExists_TC004() {
    // Arrange
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);

    String oldImageUrl = "https://example.com/old-drink.jpg";
    String newStoredUrl = "https://example.com/new-drink.jpg";

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getDrinkImageUrl(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(oldImageUrl);
    when(fileStorageService.storeDrinkImage(mockFile)).thenReturn(newStoredUrl);

    // Act
    editDrinksService.process(validRequest, mockFile, currentUserRoleName, currentUserShopId);

    // Assert
    verify(fileStorageService, times(1)).storeDrinkImage(mockFile);
    verify(fileStorageService, times(1)).deleteDrinkImage(oldImageUrl);
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            newStoredUrl,
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenImageFileProvidedAndOldImageIsNull_TC005() {
    // Arrange
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);

    String newStoredUrl = "https://example.com/new-drink.jpg";

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getDrinkImageUrl(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(null);
    when(fileStorageService.storeDrinkImage(mockFile)).thenReturn(newStoredUrl);

    // Act
    editDrinksService.process(validRequest, mockFile, currentUserRoleName, currentUserShopId);

    // Assert
    verify(fileStorageService, times(1)).storeDrinkImage(mockFile);
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            newStoredUrl,
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenImageFileProvidedAndOldImageIsBlank_TC006() {
    // Arrange
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(false);

    String newStoredUrl = "https://example.com/new-drink.jpg";

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getDrinkImageUrl(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn("   ");
    when(fileStorageService.storeDrinkImage(mockFile)).thenReturn(newStoredUrl);

    // Act
    editDrinksService.process(validRequest, mockFile, currentUserRoleName, currentUserShopId);

    // Assert
    verify(fileStorageService, times(1)).storeDrinkImage(mockFile);
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
  }

  @Test
  void process_Success_WhenImageFileIsEmpty_TC007() {
    // Arrange: imageFile != null but imageFile.isEmpty() is true
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.isEmpty()).thenReturn(true);

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    // Act
    editDrinksService.process(validRequest, mockFile, currentUserRoleName, currentUserShopId);

    // Assert
    verify(fileStorageService, never()).storeDrinkImage(any());
    verify(fileStorageService, never()).deleteDrinkImage(anyString());
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            validRequest.getImageUrl(),
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenNoImageFileAndRequestImageUrlIsNull_TC008() {
    // Arrange: Fallback to existing image in database when request has no imageUrl
    validRequest.setImageUrl(null);
    String existingImageUrl = "https://example.com/existing-image.jpg";

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getDrinkImageUrl(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(existingImageUrl);

    // Act
    editDrinksService.process(validRequest, null, currentUserRoleName, currentUserShopId);

    // Assert
    verify(editDrinkMapper, times(1))
        .getDrinkImageUrl(validRequest.getDrinkId(), currentUserRoleName, currentUserShopId);
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            existingImageUrl,
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  @Test
  void process_Success_WhenNoImageFileAndRequestImageUrlIsBlank_TC009() {
    // Arrange: Triggers imageUrlToUpdate.trim().isEmpty()
    validRequest.setImageUrl("   ");
    String existingImageUrl = "https://example.com/existing-image.jpg";

    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);
    when(editDrinkMapper.getDrinkImageUrl(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(existingImageUrl);

    // Act
    editDrinksService.process(validRequest, null, currentUserRoleName, currentUserShopId);

    // Assert
    verify(editDrinkMapper, times(1))
        .getDrinkImageUrl(validRequest.getDrinkId(), currentUserRoleName, currentUserShopId);
    verify(editDrinkMapper, times(1))
        .updateDrink(
            validRequest.getDrinkId(),
            validRequest.getDrinkName(),
            existingImageUrl,
            validRequest.getStatus(),
            validRequest.getDrinkCategoryId(),
            validRequest.getPrice(),
            validRequest.getSize(),
            currentUserShopId,
            currentUserRoleName);
  }

  // =========================================================================
  // ABNORMAL CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC010() {
    // Arrange: Boolean.FALSE explicitly triggers DataNotFoundException
    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserRoleName, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getDrinkId(), exception.getId());

    verify(commonMapper, times(1))
        .checkDrinkExisted(validRequest.getDrinkId(), currentUserRoleName, currentUserShopId);
    verify(editDrinkMapper, never())
        .updateDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataIntegrityViolationException_WhenUpdateDrinkMapperFails_TC011() {
    // Arrange
    when(commonMapper.checkDrinkExisted(
            validRequest.getDrinkId(), currentUserRoleName, currentUserShopId))
        .thenReturn(true);

    doThrow(new DataIntegrityViolationException("Database constraint violation"))
        .when(editDrinkMapper)
        .updateDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());

    // Act & Assert
    DataIntegrityViolationException exception =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserRoleName, currentUserShopId));

    assertEquals("Database constraint violation", exception.getMessage());
    verify(editDrinkMapper, times(1))
        .updateDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsException_WhenCommonMapperCheckFails_TC012() {
    // Arrange
    when(commonMapper.checkDrinkExisted(any(), anyString(), any()))
        .thenThrow(new RuntimeException("Database connection timeout"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                editDrinksService.process(
                    validRequest, null, currentUserRoleName, currentUserShopId));

    assertEquals("Database connection timeout", exception.getMessage());
    verify(editDrinkMapper, never())
        .updateDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
