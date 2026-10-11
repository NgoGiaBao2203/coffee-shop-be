package coffee.api.utils;

import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import java.util.UUID;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

public final class DrinkImageHelper {

  private DrinkImageHelper() {
    // Utility class
  }

  public static String resolveImageForCreate(
      MultipartFile imageFile, String defaultImageUrl, IFileStorageService fileStorageService) {
    if (imageFile != null && !imageFile.isEmpty()) {
      return fileStorageService.storeDrinkImage(imageFile);
    }
    return defaultImageUrl;
  }

  public static String resolveImageForEdit(
      MultipartFile imageFile,
      String requestedImageUrl,
      UUID drinkId,
      String currentUserRoleName,
      UUID drinkShopId,
      UpdateDrinkMapper updateDrinkMapper,
      IFileStorageService fileStorageService) {

    if (imageFile != null && !imageFile.isEmpty()) {
      String oldImageUrl =
          updateDrinkMapper.getDrinkImageUrl(drinkId, currentUserRoleName, drinkShopId);
      return handleUploadedImageWithSync(imageFile, oldImageUrl, fileStorageService);
    }

    if (requestedImageUrl == null || requestedImageUrl.isBlank()) {
      return updateDrinkMapper.getDrinkImageUrl(drinkId, currentUserRoleName, drinkShopId);
    }

    return requestedImageUrl;
  }

  private static String handleUploadedImageWithSync(
      MultipartFile imageFile, String oldImageUrl, IFileStorageService fileStorageService) {
    final String newStoredImageUrl = fileStorageService.storeDrinkImage(imageFile);

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                fileStorageService.deleteDrinkImage(oldImageUrl);
              }
            }

            @Override
            public void afterCompletion(int status) {
              if (status == STATUS_ROLLED_BACK) {
                fileStorageService.deleteDrinkImage(newStoredImageUrl);
              }
            }
          });
    }

    return newStoredImageUrl;
  }
}
