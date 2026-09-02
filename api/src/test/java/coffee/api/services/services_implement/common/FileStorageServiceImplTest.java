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

    assertEquals("Only JPG, JPEG, PNG, and WEBP image files are allowed", exception.getMessage());
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
}
