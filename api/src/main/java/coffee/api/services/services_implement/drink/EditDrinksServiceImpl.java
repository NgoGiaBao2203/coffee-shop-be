package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.common.IFileStorageService;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EditDrinksServiceImpl implements IEditDrinksService {

  private final CommonMapper commonMapper;
  private final UpdateDrinkMapper editDrinkMapper;
  private final IFileStorageService fileStorageService;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify request and drink ID
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }

    // 2. Verify authorization and active assignment for MANAGER
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }
    }

    // 3. Verify drink exists in role/shop scope
    Boolean isDrinkExisted =
        commonMapper.checkDrinkExisted(
            request.getDrinkId(), currentUserRoleName, currentUserShopId);
    if (Boolean.FALSE.equals(isDrinkExisted)) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    // 4. Verify category exists in role/shop scope
    Boolean isCategoryExisted =
        commonMapper.checkCategoryExisted(
            request.getDrinkCategoryId(), currentUserRoleName, currentUserShopId);
    if (Boolean.FALSE.equals(isCategoryExisted)) {
      throw new DataNotFoundException("Data not found", request.getDrinkCategoryId());
    }

    // 5. Validate and normalize input fields
    String trimmedDrinkName = request.getDrinkName() != null ? request.getDrinkName().trim() : "";
    request.setDrinkName(trimmedDrinkName);

    boolean isDrinkNameExisted =
        editDrinkMapper.checkDrinkNameExistedForEdit(request.getDrinkId(), trimmedDrinkName);
    if (isDrinkNameExisted) {
      throw new InvalidRequestException("Drink name is existed");
    }

    // 6. Determine the image URL to use
    String imageUrlToUpdate = request.getImageUrl();

    // 7. If a new image file is uploaded, store it and handle old image deletion
    if (imageFile != null && !imageFile.isEmpty()) {
      String oldImageUrl =
          editDrinkMapper.getDrinkImageUrl(
              request.getDrinkId(), currentUserRoleName, currentUserShopId);

      imageUrlToUpdate = fileStorageService.storeDrinkImage(imageFile);

      if (oldImageUrl != null && !oldImageUrl.trim().isEmpty()) {
        fileStorageService.deleteDrinkImage(oldImageUrl);
      }
    } else {
      if (imageUrlToUpdate == null || imageUrlToUpdate.trim().isEmpty()) {
        String currentImageUrl =
            editDrinkMapper.getDrinkImageUrl(
                request.getDrinkId(), currentUserRoleName, currentUserShopId);
        imageUrlToUpdate = currentImageUrl;
      }
    }

    // 8. Update drink
    editDrinkMapper.updateDrink(
        request.getDrinkId(),
        trimmedDrinkName,
        imageUrlToUpdate,
        request.getStatus(),
        request.getDrinkCategoryId(),
        currentUserShopId,
        currentUserRoleName);
  }
}
