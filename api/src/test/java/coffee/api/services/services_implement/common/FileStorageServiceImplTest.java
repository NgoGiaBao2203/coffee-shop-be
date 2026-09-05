package coffee.api.services.services_implement.common;

import static org.junit.jupiter.api.Assertions.*;

import coffee.api.exceptions.InvalidRequestException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class FileStorageServiceImplTest {

  private FileStorageServiceImpl fileStorageService;
  private final Path testUploadDir = Paths.get("uploads/drinks");

  private void cleanDirectory() throws IOException {
    if (Files.exists(testUploadDir)) {
      try (var stream = Files.walk(testUploadDir)) {
        stream
            .sorted(Comparator.reverseOrder())
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ignored) {
                  }
                });
      }
    }
  }

  @BeforeEach
  void setUp() throws IOException {
    cleanDirectory();
    fileStorageService = new FileStorageServiceImpl();
  }

  @AfterEach
  void tearDown() throws IOException {
    cleanDirectory();
  }

  @Test
  void process_ReturnsNull_WhenFileIsNull_TC001() {
    String result = fileStorageService.storeDrinkImage(null);
    assertNull(result);
  }

  @Test
  void process_ReturnsNull_WhenFileIsNotNullButEmpty_TC002() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
    String result = fileStorageService.storeDrinkImage(emptyFile);
    assertNull(result);
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenOriginalFilenameIsNull_TC003() {
    MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
    org.mockito.Mockito.when(mockFile.isEmpty()).thenReturn(false);
    org.mockito.Mockito.when(mockFile.getOriginalFilename()).thenReturn(null);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(mockFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenOriginalFilenameHasNoDot_TC004() {
    MockMultipartFile noDotFile =
        new MockMultipartFile(
            "image", "sample_image_without_dot", "image/png", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(noDotFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenExtensionIsInvalid_TC005() {
    MockMultipartFile invalidExtFile =
        new MockMultipartFile("image", "doc.pdf", "application/pdf", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> fileStorageService.storeDrinkImage(invalidExtFile));

    assertEquals("Only JPG and PNG image files are allowed", exception.getMessage());
  }

  @Test
  void process_Success_WhenDirectoryDoesNotExist_TC006() {
    MockMultipartFile validFile =
        new MockMultipartFile(
            "image", "coffee_test.png", "image/png", "sample image content".getBytes());

    String result = fileStorageService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertEquals("/uploads/drinks/", result.substring(0, 16));
    assertEquals(".png", result.substring(result.lastIndexOf(".")));

    Path savedPath = Paths.get(result.substring(1));
    assertTrue(Files.exists(savedPath));
  }

  @Test
  void process_Success_WhenDirectoryAlreadyExists_TC007() throws IOException {
    Files.createDirectories(testUploadDir);

    MockMultipartFile validFile =
        new MockMultipartFile(
            "image", "coffee_test2.jpg", "image/jpeg", "sample image content 2".getBytes());

    String result = fileStorageService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertEquals("/uploads/drinks/", result.substring(0, 16));
    assertEquals(".jpg", result.substring(result.lastIndexOf(".")));
  }

  @Test
  void process_ThrowsRuntimeException_WhenInputStreamThrowsIOException_TC008() throws IOException {
    MultipartFile brokenFile = org.mockito.Mockito.mock(MultipartFile.class);

    org.mockito.Mockito.when(brokenFile.isEmpty()).thenReturn(false);
    org.mockito.Mockito.when(brokenFile.getOriginalFilename()).thenReturn("drink.png");
    org.mockito.Mockito.when(brokenFile.getInputStream())
        .thenThrow(new IOException("Simulated disk error"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fileStorageService.storeDrinkImage(brokenFile));

    assertEquals("Could not store image file. Please try again!", exception.getMessage());
  }

  @Test
  void process_Success_WhenExtensionIsUppercase_TC009() {
    MockMultipartFile uppercaseExtFile =
        new MockMultipartFile("image", "espresso.PNG", "image/png", "sample content".getBytes());

    String result = fileStorageService.storeDrinkImage(uppercaseExtFile);

    assertNotNull(result);
    assertTrue(result.endsWith(".png"));
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenImageUrlIsNull_TC010() {
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(null));
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenImageUrlIsEmptyOrWhitespace_TC011() {
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(""));
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage("   "));
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenFileDoesNotExist_TC012() {
    String nonExistentUrl = "/uploads/drinks/non_existent_drink.png";
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(nonExistentUrl));
  }

  @Test
  void deleteDrinkImage_Success_WhenFileExists_TC013() throws IOException {
    Files.createDirectories(testUploadDir);
    Path testFile = testUploadDir.resolve("latte_test.png");
    Files.write(testFile, "test image content".getBytes());
    assertTrue(Files.exists(testFile));

    String imageUrl = "/uploads/drinks/latte_test.png";
    fileStorageService.deleteDrinkImage(imageUrl);

    assertFalse(Files.exists(testFile));
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenPathIsDirectory_TC014()
      throws IOException {
    // Files.delete(path) throws DirectoryNotEmptyException (a subclass of IOException) if path is a
    // non-empty directory
    Files.createDirectories(testUploadDir.resolve("subfolder"));
    Files.write(testUploadDir.resolve("subfolder/sample.txt"), "data".getBytes());

    String directoryUrl = "/uploads/drinks/subfolder";

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.deleteDrinkImage(directoryUrl));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  @Test
  void extractBaseFileName_ReturnsOriginalFilename_WhenNoDotPresent_TC015() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    String filenameWithoutDot = "espresso_image";
    String result = (String) method.invoke(fileStorageService, filenameWithoutDot);

    assertEquals("espresso_image", result);
  }

  @Test
  void extractBaseFileName_ThrowsInvalidRequestException_WhenFilenameIsNull_TC016()
      throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    java.lang.reflect.InvocationTargetException targetException =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(fileStorageService, (String) null));

    assertInstanceOf(InvalidRequestException.class, targetException.getCause());
    assertEquals("Invalid file name", targetException.getCause().getMessage());
  }
}
