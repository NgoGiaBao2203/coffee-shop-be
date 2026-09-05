package coffee.api.services.services_implement.common;

import coffee.api.exceptions.InvalidRequestException;
import coffee.api.services.services_interface.common.IFileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements IFileStorageService {

  private static final String UPLOAD_DIR = "uploads/drinks";
  private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".png");

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
      Path uploadPath = Paths.get(UPLOAD_DIR);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      long nanoTimestamp = System.nanoTime();
      String uniqueFileName = baseFileName + "_" + nanoTimestamp + extension;
      Path targetLocation = uploadPath.resolve(uniqueFileName);

      Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

      return "/uploads/drinks/" + uniqueFileName;
    } catch (IOException e) {
      throw new RuntimeException("Could not store image file. Please try again!", e);
    }
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
      // Remove the leading path prefix and construct the full file path
      String relativePath = imageUrl.replace("/uploads/drinks/", "");
      Path filePath = Paths.get(UPLOAD_DIR).resolve(relativePath);

      if (Files.exists(filePath)) {
        Files.delete(filePath);
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Could not delete old image file: " + imageUrl);
    }
  }
}
