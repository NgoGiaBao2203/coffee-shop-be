package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_implement.drink.common.DrinkValidator;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CreateDrinkServiceImpl implements ICreateDrinkService {

  private final CommonMapper commonMapper;
  private final CreateDrinkMapper createDrinksMapper;
  private final IFileStorageService fileStorageService;
  private final DrinkValidator drinkValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      UUID currentShopId,
      String currentUserRoleName) {

    // 1. Verify request and shop existence
    validateRequestShop(request);

    // 2. Verify manager shop permission
    drinkValidator.validateManagerShopPermission(
        currentUserRoleName,
        currentShopId,
        request.getShopId(),
        currentUserId,
        "You do not have permission to access this shop");

    // 3. Verify category belongs to shop for create
    drinkValidator.validateCategoryBelongsToShopForCreate(
        request.getDrinkCategoryId(), request.getShopId());

    // 4. Trim name and verify uniqueness in shop
    String trimmedDrinkName = drinkValidator.validateAndFormatName(request.getDrinkName());
    request.setDrinkName(trimmedDrinkName);
    validateDrinkNameNotDuplicated(trimmedDrinkName, request.getShopId());

    // 5. Validate and format drink size
    String size = drinkValidator.validateAndFormatSize(request.getSize());
    request.setSize(size);

    // 6. Resolve image upload or fallback URL
    String storedImageUrl = resolveImageForCreate(imageFile, request.getImageUrl());

    // 7. Insert new drink and drink detail records
    insertDrinkWithDetail(request, trimmedDrinkName, storedImageUrl, size);
  }

  private void validateRequestShop(CreateDrinksRequest request) {
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }
  }

  private void validateDrinkNameNotDuplicated(String drinkName, UUID shopId) {
    boolean isDrinkExisted = commonMapper.checkDrinkNameExisted(null, drinkName, shopId);
    if (isDrinkExisted) {
      throw new InvalidRequestException("Drink name is existed");
    }
  }

  private String resolveImageForCreate(MultipartFile imageFile, String defaultImageUrl) {
    if (imageFile != null && !imageFile.isEmpty()) {
      return fileStorageService.storeDrinkImage(imageFile);
    }
    return defaultImageUrl;
  }

  private void insertDrinkWithDetail(
      CreateDrinksRequest request, String drinkName, String imageUrl, String size) {
    UUID drinkId = UUID.randomUUID();
    createDrinksMapper.insertDrink(
        drinkId,
        request.getDrinkCategoryId(),
        request.getShopId(),
        drinkName,
        imageUrl,
        request.getStatus());

    createDrinksMapper.insertDrinkDetail(drinkId, size, request.getPrice());
  }
}
