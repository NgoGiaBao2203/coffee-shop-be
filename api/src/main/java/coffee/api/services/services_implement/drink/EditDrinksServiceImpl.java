package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_implement.drink.common.DrinkValidator;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EditDrinksServiceImpl implements IEditDrinksService {

  private final UpdateDrinkMapper updateDrinkMapper;
  private final IFileStorageService fileStorageService;
  private final DrinkValidator drinkValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify request and drink ID
    validateRequest(request);

    // 2. Verify drink existence and role/shop access
    drinkValidator.validateDrinkExistence(
        request.getDrinkId(), currentUserRoleName, currentUserShopId);

    // 3. Resolve drink shop ID
    UUID drinkShopId = resolveDrinkShopId(request.getDrinkId());

    // 4. Verify manager shop permission
    drinkValidator.validateManagerShopPermission(
        currentUserRoleName,
        currentUserShopId,
        drinkShopId,
        currentUserId,
        "You do not have permission to edit a drink from another shop");

    // 5. Verify category belongs to shop for edit
    drinkValidator.validateCategoryBelongsToShopForEdit(request.getDrinkCategoryId(), drinkShopId);

    // 6. Trim name and verify uniqueness for edit
    String trimmedDrinkName = drinkValidator.validateAndFormatName(request.getDrinkName());
    request.setDrinkName(trimmedDrinkName);
    validateDrinkNameNotDuplicatedForEdit(request.getDrinkId(), trimmedDrinkName);

    // 7. Resolve image upload or fallback URL
    String storedImageUrl =
        resolveImageForEdit(
            imageFile,
            request.getImageUrl(),
            request.getDrinkId(),
            currentUserRoleName,
            drinkShopId);

    // 8. Update drink record
    updateDrinkMapper.updateDrink(
        request.getDrinkId(),
        trimmedDrinkName,
        storedImageUrl,
        request.getStatus(),
        request.getDrinkCategoryId(),
        drinkShopId,
        currentUserRoleName);
  }

  private void validateRequest(EditDrinksRequest request) {
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }
  }

  private UUID resolveDrinkShopId(UUID drinkId) {
    UUID drinkShopId = updateDrinkMapper.getShopIdByDrinkId(drinkId);
    if (drinkShopId == null) {
      throw new DataNotFoundException("Data not found", drinkId);
    }
    return drinkShopId;
  }

  private void validateDrinkNameNotDuplicatedForEdit(UUID drinkId, String drinkName) {
    boolean isDrinkNameDuplicated =
        updateDrinkMapper.checkDrinkNameExistedForEdit(drinkId, drinkName);
    if (isDrinkNameDuplicated) {
      throw new InvalidRequestException("Drink name is existed");
    }
  }

  private String resolveImageForEdit(
      MultipartFile imageFile,
      String requestedImageUrl,
      UUID drinkId,
      String currentUserRoleName,
      UUID drinkShopId) {

    if (imageFile != null && !imageFile.isEmpty()) {
      String oldImageUrl =
          updateDrinkMapper.getDrinkImageUrl(drinkId, currentUserRoleName, drinkShopId);
      return handleUploadedImageWithSync(imageFile, oldImageUrl);
    }

    if (requestedImageUrl == null || requestedImageUrl.isBlank()) {
      return updateDrinkMapper.getDrinkImageUrl(drinkId, currentUserRoleName, drinkShopId);
    }

    return requestedImageUrl;
  }

  private String handleUploadedImageWithSync(MultipartFile imageFile, String oldImageUrl) {
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
