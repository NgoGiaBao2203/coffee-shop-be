package coffee.api.services.services_implement.common;

import coffee.api.config.SupabaseStorageProperties;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.services.services_interface.common.IFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
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
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".png");

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

    String extension = extractAndValidateExtension(file.getOriginalFilename());
    String baseFileName = extractBaseFileName(file.getOriginalFilename());

    try {
      long nanoTimestamp = System.nanoTime();
      String uniqueFileName = baseFileName + "_" + nanoTimestamp + extension;

      if (isSupabaseEnabled()) {
        return storeToSupabase(uniqueFileName, file);
      } else {
        return storeToLocalFileSystem(uniqueFileName, file);
      }
    } catch (Exception e) {
      throw new RuntimeException("Could not store image file. Please try again!", e);
    }
  }

  private String storeToSupabase(String uniqueFileName, MultipartFile file) throws Exception {
    try (S3Client s3Client = createS3Client()) {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(storageProperties.getBucketName())
              .key(uniqueFileName)
              .build();

      s3Client.putObject(
          putObjectRequest,
          software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
              file.getInputStream(), file.getSize()));
    }

    return storageProperties.getPublicUrl()
        + "/"
        + storageProperties.getBucketName()
        + "/"
        + uniqueFileName;
  }

  private String storeToLocalFileSystem(String uniqueFileName, MultipartFile file)
      throws IOException {
    Path uploadPath = Paths.get(UPLOAD_DIR);
    if (!Files.exists(uploadPath)) {
      Files.createDirectories(uploadPath);
    }

    Path targetLocation = uploadPath.resolve(uniqueFileName);
    Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

    return "/uploads/drinks/" + uniqueFileName;
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

  private String extractBaseFileName(String originalFilename) {
    if (originalFilename == null) {
      throw new InvalidRequestException("Invalid file name");
    }

    int lastDotIndex = originalFilename.lastIndexOf(".");
    if (lastDotIndex == -1) {
      return originalFilename;
    }

    return originalFilename.substring(0, lastDotIndex);
  }

  @Override
  public void deleteDrinkImage(String imageUrl) {
    if (imageUrl == null || imageUrl.trim().isEmpty()) {
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
    String relativePath = imageUrl.replace("/uploads/drinks/", "");
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
    String bucketPath = "/storage/v1/object/public/" + storageProperties.getBucketName() + "/";
    try {
      if (imageUrl.contains(bucketPath)) {
        return imageUrl.substring(imageUrl.indexOf(bucketPath) + bucketPath.length());
      }
      throw new InvalidRequestException("Invalid image URL format");
    } catch (StringIndexOutOfBoundsException e) {
      throw new InvalidRequestException("Invalid image URL format");
    }
  }
}
