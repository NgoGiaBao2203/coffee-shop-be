package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkMapper;
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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      UUID currentShopId,
      String currentUserRoleName) {

    // 1. Verify request and shop ID
    if (request == null || request.getShopId() == null) {
      throw new InvalidRequestException("Shop ID is required");
    }

    // 2. Verify shop exists
    boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    // 3. Verify authorization and active assignment for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop");
      }

      if (!currentShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }
    }

    // 4. Verify category exists and belongs to the shop
    boolean isCategoryBelongsToShop =
        commonMapper.checkCategoryExistsInShop(request.getDrinkCategoryId(), request.getShopId());
    if (!isCategoryBelongsToShop) {
      throw new DataNotFoundException("Data not found", request.getDrinkCategoryId());
    }

    // 5. Trim drink name and verify uniqueness in shop
    String trimmedDrinkName = request.getDrinkName() != null ? request.getDrinkName().trim() : "";
    request.setDrinkName(trimmedDrinkName);

    boolean isDrinkExisted =
        commonMapper.checkDrinkNameExisted(null, trimmedDrinkName, request.getShopId());
    if (isDrinkExisted) {
      throw new InvalidRequestException("Drink name is existed");
    }

    // 6. Normalize and verify drink size
    String size = request.getSize() == null ? "" : request.getSize().trim().toUpperCase();
    if (size.isEmpty()) {
      throw new InvalidRequestException("Drink size is required");
    }
    request.setSize(size);

    // 7. Handle image storage if uploaded
    String storedImageUrl = request.getImageUrl();
    if (imageFile != null && !imageFile.isEmpty()) {
      storedImageUrl = fileStorageService.storeDrinkImage(imageFile);
    }

    // 8. Insert new drink and its variant detail
    UUID drinkId = UUID.randomUUID();
    createDrinksMapper.insertDrink(
        drinkId,
        request.getDrinkCategoryId(),
        request.getShopId(),
        trimmedDrinkName,
        storedImageUrl,
        request.getStatus());

    createDrinksMapper.insertDrinkDetail(drinkId, size, request.getPrice());
  }
}
