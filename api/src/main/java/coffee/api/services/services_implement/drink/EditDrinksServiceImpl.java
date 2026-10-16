package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
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
  @Transactional
  public void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      String currentUserRoleName,
      UUID currentUserShopId) {
    Boolean isDrinkExisted =
        commonMapper.checkDrinkExisted(
            request.getDrinkId(), currentUserRoleName, currentUserShopId);

    if (Boolean.FALSE.equals(isDrinkExisted)) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    // Determine the image URL to use
    String imageUrlToUpdate = request.getImageUrl();

    // If a new image file is uploaded, store it and handle old image deletion
    if (imageFile != null && !imageFile.isEmpty()) {
      // Get the current drink's image URL for deletion
      String oldImageUrl =
          editDrinkMapper.getDrinkImageUrl(
              request.getDrinkId(), currentUserRoleName, currentUserShopId);

      // Store the new image
      imageUrlToUpdate = fileStorageService.storeDrinkImage(imageFile);

      // Delete the old image if it exists
      if (oldImageUrl != null && !oldImageUrl.trim().isEmpty()) {
        fileStorageService.deleteDrinkImage(oldImageUrl);
      }
    } else {
      // No new image uploaded, keep the current image
      if (imageUrlToUpdate == null || imageUrlToUpdate.trim().isEmpty()) {
        String currentImageUrl =
            editDrinkMapper.getDrinkImageUrl(
                request.getDrinkId(), currentUserRoleName, currentUserShopId);
        imageUrlToUpdate = currentImageUrl;
      }
    }

    editDrinkMapper.updateDrink(
        request.getDrinkId(),
        request.getDrinkName(),
        imageUrlToUpdate,
        request.getStatus(),
        request.getDrinkCategoryId(),
        request.getPrice(),
        request.getSize(),
        currentUserShopId,
        currentUserRoleName);
  }
}
