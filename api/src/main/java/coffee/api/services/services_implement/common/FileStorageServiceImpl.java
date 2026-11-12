package coffee.api.services.services_implement.common;

import coffee.api.config.SupabaseStorageProperties;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.services.services_interface.common.IFileStorageService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

  private static final String UPLOAD_DIR = "uploads/drinks";
  private static final String LOCAL_URL_PREFIX = "/uploads/drinks/";
  private static final String SUPABASE_PUBLIC_PATH = "/storage/v1/object/public/";
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".png");
  private static final String FILE_NAME_PREFIX = "drink_";
  // Every upload is re-encoded as JPEG so stored files stay in the KB range
  private static final String STORED_EXTENSION = ".jpg";
  private static final String STORED_CONTENT_TYPE = "image/jpeg";
  private static final int MAX_IMAGE_DIMENSION = 800;
  private static final float JPEG_QUALITY = 0.8f;
  private static final String STORE_FAILED_MESSAGE =
      "Could not store image file. Please try again!";

  @Nullable private final SupabaseStorageProperties storageProperties;

  public FileStorageServiceImpl(@Nullable SupabaseStorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  private boolean isSupabaseEnabled() {
    return storageProperties != null
        && storageProperties.isEnabled()
        && storageProperties.getAccessKey() != null
        && !storageProperties.getAccessKey().isEmpty();
  }

  @Override
  public String storeDrinkImage(MultipartFile file) {
    if (file == null) {
      return null;
    }

    if (file.isEmpty()) {
      return null;
    }

    extractAndValidateExtension(file.getOriginalFilename());
    byte[] compressedImage = compressImage(readFileBytes(file));

    try {
      String uniqueFileName = generateFileName(STORED_EXTENSION);

      if (isSupabaseEnabled()) {
        return storeToSupabase(uniqueFileName, compressedImage);
      } else {
        return storeToLocalFileSystem(uniqueFileName, compressedImage);
      }
    } catch (Exception e) {
      throw new RuntimeException(STORE_FAILED_MESSAGE, e);
    }
  }

  private byte[] readFileBytes(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream()) {
      return inputStream.readAllBytes();
    } catch (IOException e) {
      throw new RuntimeException(STORE_FAILED_MESSAGE, e);
    }
  }

  /** Downscale to MAX_IMAGE_DIMENSION and re-encode as JPEG; reject files that are not images. */
  private byte[] compressImage(byte[] originalBytes) {
    BufferedImage source;
    try {
      source = ImageIO.read(new ByteArrayInputStream(originalBytes));
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid image file");
    }
    if (source == null) {
      throw new InvalidRequestException("Invalid image file");
    }

    try {
      return encodeJpeg(resizeOnWhiteBackground(source));
    } catch (IOException e) {
      throw new RuntimeException(STORE_FAILED_MESSAGE, e);
    }
  }

  /** Only shrinks; transparent PNG areas become white because JPEG has no alpha channel. */
  private BufferedImage resizeOnWhiteBackground(BufferedImage source) {
    double scale =
        Math.min(
            1.0, (double) MAX_IMAGE_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
    int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
    int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

    BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.drawImage(source, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private byte[] encodeJpeg(BufferedImage image) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(JPEG_QUALITY);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
      writer.setOutput(imageOutputStream);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
    return outputStream.toByteArray();
  }

  private String storeToSupabase(String uniqueFileName, byte[] imageBytes) {
    try (S3Client s3Client = createS3Client()) {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(storageProperties.getBucketName())
              .key(uniqueFileName)
              .contentType(STORED_CONTENT_TYPE)
              .build();

      s3Client.putObject(
          putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(imageBytes));
    }

    return storageProperties.getPublicUrl()
        + "/"
        + storageProperties.getBucketName()
        + "/"
        + uniqueFileName;
  }

  private String storeToLocalFileSystem(String uniqueFileName, byte[] imageBytes)
      throws IOException {
    Path uploadPath = Paths.get(UPLOAD_DIR);
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }

    Path targetLocation = uploadPath.resolve(uniqueFileName);
    Files.write(targetLocation, imageBytes);

    return LOCAL_URL_PREFIX + uniqueFileName;
  }

  private String extractAndValidateExtension(String originalFilename) {
    if (originalFilename == null) {
      throw new InvalidRequestException("Invalid file name");
    }

    int lastDotIndex = originalFilename.lastIndexOf(".");
    if (lastDotIndex == -1) {
      throw new InvalidRequestException("Invalid file name");
    }

    String extension = originalFilename.substring(lastDotIndex).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new InvalidRequestException("Only JPG and PNG image files are allowed");
    }

    return extension;
  }

  /** Build a numeric file name so accents or spaces in the original name never reach storage. */
  private String generateFileName(String extension) {
    int randomSuffix = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
    return FILE_NAME_PREFIX + System.currentTimeMillis() + "_" + randomSuffix + extension;
  }

  @Override
  public void deleteDrinkImage(String imageUrl) {
    if (imageUrl == null || imageUrl.trim().isEmpty() || !isManagedImageUrl(imageUrl)) {
      return;
    }

    try {
      if (isSupabaseEnabled()) {
        deleteFromSupabase(imageUrl);
      } else {
        deleteFromLocalFileSystem(imageUrl);
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Could not delete old image file: " + imageUrl);
    }
  }

  /**
   * Only files uploaded by this service are deleted; external links such as placeholders are kept.
   */
  private boolean isManagedImageUrl(String imageUrl) {
    if (isSupabaseEnabled()) {
      return imageUrl.contains(supabaseBucketPath());
    }
    return imageUrl.startsWith(LOCAL_URL_PREFIX);
  }

  private String supabaseBucketPath() {
    return SUPABASE_PUBLIC_PATH + storageProperties.getBucketName() + "/";
  }

  private void deleteFromSupabase(String imageUrl) {
    String fileKey = extractFileKeyFromUrl(imageUrl);

    try (S3Client s3Client = createS3Client()) {
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder()
              .bucket(storageProperties.getBucketName())
              .key(fileKey)
              .build();

      s3Client.deleteObject(deleteObjectRequest);
    }
  }

  private void deleteFromLocalFileSystem(String imageUrl) throws IOException {
    String relativePath = imageUrl.replace(LOCAL_URL_PREFIX, "");
    Path filePath = Paths.get(UPLOAD_DIR).resolve(relativePath);

    if (Files.exists(filePath)) {
      Files.delete(filePath);
    }
  }

  private S3Client createS3Client() {
    AwsBasicCredentials awsCredentials =
        AwsBasicCredentials.create(
            storageProperties.getAccessKey(), storageProperties.getSecretKey());

    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
        .region(Region.of(storageProperties.getRegion()))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .endpointOverride(java.net.URI.create(storageProperties.getEndpoint()))
        .build();
  }

  private String extractFileKeyFromUrl(String imageUrl) {
    // Extract the key from URL: https://.../storage/v1/object/public/{bucket}/{key}
    String bucketPath = supabaseBucketPath();
    int bucketPathIndex = imageUrl.indexOf(bucketPath);
    if (bucketPathIndex == -1) {
      throw new InvalidRequestException("Invalid image URL format");
    }
    return imageUrl.substring(bucketPathIndex + bucketPath.length());
  }
}
