package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import coffee.api.utils.DrinkImageHelper;
import coffee.api.utils.DrinkValidationHelper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EditDrinksServiceImpl implements IEditDrinksService {

  private final CommonMapper commonMapper;
  private final UpdateDrinkMapper updateDrinkMapper;
  private final IFileStorageService fileStorageService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    validateRequest(request);
    validateDrinkExistence(request.getDrinkId(), currentUserRoleName, currentUserShopId);

    UUID drinkShopId = resolveDrinkShopId(request.getDrinkId());
    DrinkValidationHelper.validateManagerShopPermission(
        currentUserRoleName,
        currentUserShopId,
        drinkShopId,
        currentUserId,
        "You do not have permission to edit a drink from another shop",
        commonMapper);
    DrinkValidationHelper.validateCategoryBelongsToShopForEdit(
        request.getDrinkCategoryId(), drinkShopId, commonMapper);

    String trimmedDrinkName = DrinkValidationHelper.validateAndFormatName(request.getDrinkName());
    request.setDrinkName(trimmedDrinkName);
    validateDrinkNameNotDuplicatedForEdit(request.getDrinkId(), trimmedDrinkName);

    String storedImageUrl =
        DrinkImageHelper.resolveImageForEdit(
            imageFile,
            request.getImageUrl(),
            request.getDrinkId(),
            currentUserRoleName,
            drinkShopId,
            updateDrinkMapper,
            fileStorageService);

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

  private void validateDrinkExistence(UUID drinkId, String roleName, UUID shopId) {
    boolean isDrinkExisted = commonMapper.checkDrinkExisted(drinkId, roleName, shopId);
    if (!isDrinkExisted) {
      throw new DataNotFoundException("Data not found", drinkId);
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
}
