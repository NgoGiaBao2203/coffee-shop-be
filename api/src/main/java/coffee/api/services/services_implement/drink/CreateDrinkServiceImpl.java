package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.exceptions.UserExistException;
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

  private final CreateDrinkMapper createDrinksMapper;
  private final IFileStorageService fileStorageService;

  @Override
  @Transactional
  public void process(
      CreateDrinksRequest request, MultipartFile imageFile, UUID currentShopID, String roleName) {

    UUID targetShopId = validateShopId(roleName, request.getShopId(), currentShopID);

    Boolean isDrinkExisted =
        createDrinksMapper.checkDrinkExistedByName(targetShopId, request.getDrinkName());
    if (isDrinkExisted) {
      throw new UserExistException("Drink is existed");
    }

    String size = request.getSize() != null ? request.getSize().trim().toUpperCase() : "";
    if (!size.equals("S") && !size.equals("M") && !size.equals("L")) {
      throw new RuntimeException("Size is invalid. Must be S, M, or L");
    }

    if (request.getPrice() == null || request.getPrice() <= 0) {
      throw new RuntimeException("Price must be greater than 0");
    }

    // Save image to disk if uploaded
    String storedImageUrl = request.getImageUrl();
    if (imageFile != null && !imageFile.isEmpty()) {
      storedImageUrl = fileStorageService.storeDrinkImage(imageFile);
    }

    createDrinksMapper.createDrink(
        request.getDrinkCategoryId(),
        request.getDrinkDetailId(),
        targetShopId,
        request.getDrinkName(),
        storedImageUrl,
        request.getStatus(),
        request.getIsDeleted(),
        size,
        request.getPrice());
  }

  private UUID validateShopId(String roleName, UUID requestShopId, UUID currentShopId) {
    if ("OWNER".equals(roleName)) {
      return requestShopId;
    }
    return currentShopId;
  }
}
