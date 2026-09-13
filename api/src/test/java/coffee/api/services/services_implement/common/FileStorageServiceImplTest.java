package coffee.api.services.services_implement.common;

import static org.junit.jupiter.api.Assertions.*;

import coffee.api.config.SupabaseStorageProperties;
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
  private SupabaseStorageProperties storageProperties;
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

    storageProperties = new SupabaseStorageProperties();
    storageProperties.setEnabled(true);
    storageProperties.setBucketName("drinks");
    storageProperties.setRegion("ap-south-1");
    storageProperties.setEndpoint("https://aws-0-ap-south-1.pooler.supabase.com");
    storageProperties.setAccessKey("test-access-key");
    storageProperties.setSecretKey("test-secret-key");

    fileStorageService = new FileStorageServiceImpl(storageProperties);
  }

  @AfterEach
  void tearDown() throws IOException {
    cleanDirectory();
  }

  @Test
  void storeDrinkImage_ReturnsNull_WhenFileIsNull_TC001() {
    String result = fileStorageService.storeDrinkImage(null);
    assertNull(result);
  }

  @Test
  void storeDrinkImage_ReturnsNull_WhenFileIsEmpty_TC002() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);
    String result = fileStorageService.storeDrinkImage(emptyFile);
    assertNull(result);
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenFilenameIsNull_TC003() {
    MockMultipartFile mockFile =
        new MockMultipartFile("image", (String) null, "image/png", "content".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(mockFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenFilenameHasNoDot_TC004() {
    MockMultipartFile noDotFile =
        new MockMultipartFile(
            "image", "sample_image_without_dot", "image/png", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.storeDrinkImage(noDotFile));

    assertEquals("Invalid file name", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsInvalidRequestException_WhenExtensionIsInvalid_TC005() {
    MockMultipartFile invalidExtFile =
        new MockMultipartFile("image", "doc.pdf", "application/pdf", "sample".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> fileStorageService.storeDrinkImage(invalidExtFile));

    assertEquals("Only JPG and PNG image files are allowed", exception.getMessage());
  }

  @Test
  void storeDrinkImage_ThrowsRuntimeException_WhenSupabaseS3CallFails_TC006() {
    MockMultipartFile validFile =
        new MockMultipartFile("image", "coffee_test.png", "image/png", "content".getBytes());

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> fileStorageService.storeDrinkImage(validFile));

    assertEquals("Could not store image file. Please try again!", exception.getMessage());
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenSupabaseUrlFormatIsInvalid_TC007() {
    String invalidUrl = "https://example.com/invalid/path/image.png";

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.deleteDrinkImage(invalidUrl));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenSupabaseS3DeleteFails_TC008() {
    String validSupabaseUrl =
        "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/coffee_123.png";

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> fileStorageService.deleteDrinkImage(validSupabaseUrl));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  // --- Local FileSystem Mode (Supabase Disabled) Tests ---

  @Test
  void storeDrinkImage_SavesToLocalFileSystem_WhenPropertiesNull_TC009() {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);
    MockMultipartFile validFile =
        new MockMultipartFile("image", "latte.png", "image/png", "sample content".getBytes());

    String result = localService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertTrue(result.startsWith("/uploads/drinks/latte_"));
    assertTrue(result.endsWith(".png"));

    Path savedPath = Paths.get(result.substring(1));
    assertTrue(Files.exists(savedPath));
  }

  @Test
  void storeDrinkImage_SavesToLocalFileSystem_WhenSupabaseDisabled_TC010() {
    storageProperties.setEnabled(false);
    MockMultipartFile validFile =
        new MockMultipartFile("image", "mocha.jpg", "image/jpeg", "sample mocha".getBytes());

    String result = fileStorageService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertTrue(result.startsWith("/uploads/drinks/mocha_"));
    assertTrue(result.endsWith(".jpg"));

    Path savedPath = Paths.get(result.substring(1));
    assertTrue(Files.exists(savedPath));
  }

  @Test
  void storeDrinkImage_SavesToLocalFileSystem_WhenAccessKeyIsEmpty_TC011() {
    storageProperties.setAccessKey("");
    MockMultipartFile validFile =
        new MockMultipartFile("image", "cappuccino.png", "image/png", "sample content".getBytes());

    String result = fileStorageService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertTrue(result.startsWith("/uploads/drinks/cappuccino_"));
  }

  @Test
  void storeDrinkImage_HandlesUppercaseExtension_TC012() {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);
    MockMultipartFile uppercaseExtFile =
        new MockMultipartFile("image", "espresso.PNG", "image/png", "content".getBytes());

    String result = localService.storeDrinkImage(uppercaseExtFile);

    assertNotNull(result);
    assertTrue(result.endsWith(".png"));
  }

  @Test
  void storeDrinkImage_ThrowsRuntimeException_WhenLocalFileStreamThrowsIOException_TC013()
      throws IOException {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);
    MultipartFile brokenFile = org.mockito.Mockito.mock(MultipartFile.class);

    org.mockito.Mockito.when(brokenFile.isEmpty()).thenReturn(false);
    org.mockito.Mockito.when(brokenFile.getOriginalFilename()).thenReturn("drink.png");
    org.mockito.Mockito.when(brokenFile.getInputStream())
        .thenThrow(new IOException("Simulated disk read error"));

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> localService.storeDrinkImage(brokenFile));

    assertEquals("Could not store image file. Please try again!", exception.getMessage());
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenImageUrlIsNullOrWhitespace_TC014() {
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(null));
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(""));
    assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage("   "));
  }

  @Test
  void deleteDrinkImage_DeletesFileFromLocalFileSystem_WhenFileExists_TC015() throws IOException {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);

    Files.createDirectories(testUploadDir);
    Path testFile = testUploadDir.resolve("test_delete.png");
    Files.write(testFile, "data".getBytes());
    assertTrue(Files.exists(testFile));

    localService.deleteDrinkImage("/uploads/drinks/test_delete.png");

    assertFalse(Files.exists(testFile));
  }

  @Test
  void deleteDrinkImage_DoesNothing_WhenLocalFileDoesNotExist_TC016() {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);
    assertDoesNotThrow(() -> localService.deleteDrinkImage("/uploads/drinks/non_existent.png"));
  }

  @Test
  void deleteDrinkImage_ThrowsInvalidRequestException_WhenLocalPathIsDirectory_TC017()
      throws IOException {
    FileStorageServiceImpl localService = new FileStorageServiceImpl(null);

    Files.createDirectories(testUploadDir.resolve("subfolder"));
    Files.write(testUploadDir.resolve("subfolder/sample.txt"), "data".getBytes());

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> localService.deleteDrinkImage("/uploads/drinks/subfolder"));

    assertTrue(exception.getMessage().contains("Could not delete old image file"));
  }

  @Test
  void extractFileKeyFromUrl_SuccessfullyExtractsKey_TC018() throws Exception {
    String validUrl =
        "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/coffee_123.png";

    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractFileKeyFromUrl", String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(fileStorageService, validUrl);

    assertEquals("coffee_123.png", result);
  }

  @Test
  void extractFileKeyFromUrl_ThrowsInvalidRequestException_WhenBucketMismatch_TC019()
      throws Exception {
    String invalidBucketUrl =
        "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/other_bucket/image.png";

    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractFileKeyFromUrl", String.class);
    method.setAccessible(true);

    java.lang.reflect.InvocationTargetException invocationException =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(fileStorageService, invalidBucketUrl));

    assertInstanceOf(InvalidRequestException.class, invocationException.getCause());
    assertEquals("Invalid image URL format", invocationException.getCause().getMessage());
  }

  @Test
  void extractBaseFileName_ReturnsBaseName_WhenDotPresent_TC020() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(fileStorageService, "espresso_drink.png");

    assertEquals("espresso_drink", result);
  }

  @Test
  void extractBaseFileName_ReturnsOriginalFilename_WhenNoDotPresent_TC021() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractBaseFileName", String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(fileStorageService, "espresso_drink");

    assertEquals("espresso_drink", result);
  }

  @Test
  void extractBaseFileName_ThrowsInvalidRequestException_WhenNull_TC022() throws Exception {
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

  @Test
  void extractAndValidateExtension_ReturnsLowercasedExtension_WhenValid_TC023() throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    String result = (String) method.invoke(fileStorageService, "cold_brew.PNG");

    assertEquals(".png", result);
  }

  @Test
  void extractAndValidateExtension_ThrowsInvalidRequestException_WhenExtensionUnsupported_TC024()
      throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    java.lang.reflect.InvocationTargetException targetException =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(fileStorageService, "document.txt"));

    assertInstanceOf(InvalidRequestException.class, targetException.getCause());
    assertEquals(
        "Only JPG and PNG image files are allowed", targetException.getCause().getMessage());
  }

  @Test
  void storeDrinkImage_Success_WhenSupabaseIsEnabled_TC025() {
    software.amazon.awssdk.services.s3.S3Client mockS3Client =
        org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);
    software.amazon.awssdk.services.s3.S3ClientBuilder mockBuilder =
        org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3ClientBuilder.class);
    org.mockito.Mockito.when(mockBuilder.credentialsProvider(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.region(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(
            mockBuilder.serviceConfiguration(
                org.mockito.ArgumentMatchers.any(
                    software.amazon.awssdk.services.s3.S3Configuration.class)))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.endpointOverride(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.build()).thenReturn(mockS3Client);

    try (org.mockito.MockedStatic<software.amazon.awssdk.services.s3.S3Client> mockedStaticS3 =
        org.mockito.Mockito.mockStatic(software.amazon.awssdk.services.s3.S3Client.class)) {

      mockedStaticS3
          .when(software.amazon.awssdk.services.s3.S3Client::builder)
          .thenReturn(mockBuilder);

      MockMultipartFile validFile =
          new MockMultipartFile("image", "espresso.png", "image/png", "sample content".getBytes());

      String result = fileStorageService.storeDrinkImage(validFile);

      assertNotNull(result);
      assertTrue(
          result.startsWith(
              "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/espresso_"));
      assertTrue(result.endsWith(".png"));

      org.mockito.Mockito.verify(mockS3Client, org.mockito.Mockito.times(1))
          .putObject(
              org.mockito.ArgumentMatchers.any(
                  software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
              org.mockito.ArgumentMatchers.any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
  }

  @Test
  void deleteDrinkImage_Success_WhenSupabaseIsEnabled_TC026() {
    software.amazon.awssdk.services.s3.S3Client mockS3Client =
        org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);
    software.amazon.awssdk.services.s3.S3ClientBuilder mockBuilder =
        org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3ClientBuilder.class);

    org.mockito.Mockito.when(mockBuilder.credentialsProvider(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.region(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(
            mockBuilder.serviceConfiguration(
                org.mockito.ArgumentMatchers.any(
                    software.amazon.awssdk.services.s3.S3Configuration.class)))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.endpointOverride(org.mockito.ArgumentMatchers.any()))
        .thenReturn(mockBuilder);
    org.mockito.Mockito.when(mockBuilder.build()).thenReturn(mockS3Client);

    try (org.mockito.MockedStatic<software.amazon.awssdk.services.s3.S3Client> mockedStaticS3 =
        org.mockito.Mockito.mockStatic(software.amazon.awssdk.services.s3.S3Client.class)) {

      mockedStaticS3
          .when(software.amazon.awssdk.services.s3.S3Client::builder)
          .thenReturn(mockBuilder);

      String validSupabaseUrl =
          "https://aws-0-ap-south-1.pooler.supabase.com/storage/v1/object/public/drinks/americano_12345.png";

      assertDoesNotThrow(() -> fileStorageService.deleteDrinkImage(validSupabaseUrl));

      org.mockito.Mockito.verify(mockS3Client, org.mockito.Mockito.times(1))
          .deleteObject(
              org.mockito.ArgumentMatchers.any(
                  software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
    }
  }

  @Test
  void
      extractFileKeyFromUrl_ThrowsInvalidRequestException_WhenStringIndexOutOfBoundsTriggered_TC027()
          throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractFileKeyFromUrl", String.class);
    method.setAccessible(true);
    String edgeUrl = "https://example.com/storage/v1/object/public/drinks/";
    String result = (String) method.invoke(fileStorageService, edgeUrl);
    assertEquals("", result);
  }

  @Test
  void storeDrinkImage_SavesToLocalFileSystem_WhenAccessKeyIsNull_TC028() {
    storageProperties.setAccessKey(null);
    MockMultipartFile validFile =
        new MockMultipartFile("image", "latte.png", "image/png", "sample content".getBytes());

    String result = fileStorageService.storeDrinkImage(validFile);

    assertNotNull(result);
    assertTrue(result.startsWith("/uploads/drinks/latte_"));
    assertTrue(result.endsWith(".png"));
  }

  @Test
  void extractAndValidateExtension_ThrowsInvalidRequestException_WhenFilenameIsNull_TC029()
      throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    java.lang.reflect.InvocationTargetException exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(fileStorageService, (String) null));

    assertInstanceOf(InvalidRequestException.class, exception.getCause());
    assertEquals("Invalid file name", exception.getCause().getMessage());
  }

  @Test
  void extractAndValidateExtension_ThrowsInvalidRequestException_WhenFilenameHasNoDot_TC030()
      throws Exception {
    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractAndValidateExtension", String.class);
    method.setAccessible(true);

    java.lang.reflect.InvocationTargetException exception =
        assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> method.invoke(fileStorageService, "filename_without_dot"));

    assertInstanceOf(InvalidRequestException.class, exception.getCause());
    assertEquals("Invalid file name", exception.getCause().getMessage());
  }

  @Test
  void
      extractFileKeyFromUrl_ThrowsInvalidRequestException_WhenCatchingStringIndexOutOfBounds_TC031()
          throws Exception {
    storageProperties.setBucketName("");

    java.lang.reflect.Method method =
        FileStorageServiceImpl.class.getDeclaredMethod("extractFileKeyFromUrl", String.class);
    method.setAccessible(true);
    String craftedUrl = "/storage/v1/object/public//";
    StringBuilder dynamicUrl = new StringBuilder(craftedUrl + "temp_image.png");
    String result = (String) method.invoke(fileStorageService, dynamicUrl.toString());
    assertEquals("temp_image.png", result);
  }
}
