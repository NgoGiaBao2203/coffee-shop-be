package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
import coffee.api.utils.DrinkImageHelper;
import coffee.api.utils.DrinkValidationHelper;
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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      UUID currentShopId,
      String currentUserRoleName) {

    validateRequestShop(request);
    DrinkValidationHelper.validateManagerShopPermission(
        currentUserRoleName,
        currentShopId,
        request.getShopId(),
        currentUserId,
        "You do not have permission to access this shop",
        commonMapper);
    DrinkValidationHelper.validateCategoryBelongsToShopForCreate(
        request.getDrinkCategoryId(), request.getShopId(), commonMapper);

    String trimmedDrinkName = DrinkValidationHelper.validateAndFormatName(request.getDrinkName());
    request.setDrinkName(trimmedDrinkName);
    validateDrinkNameNotDuplicated(trimmedDrinkName, request.getShopId());

    String size = DrinkValidationHelper.validateAndFormatSize(request.getSize());
    request.setSize(size);

    String storedImageUrl =
        DrinkImageHelper.resolveImageForCreate(
            imageFile, request.getImageUrl(), fileStorageService);

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
