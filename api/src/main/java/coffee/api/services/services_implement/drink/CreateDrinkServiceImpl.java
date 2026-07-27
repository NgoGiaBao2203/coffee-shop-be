package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateDrinkServiceImpl implements ICreateDrinkService {

  private final CreateDrinkMapper createDrinksMapper;

  @Override
  public void process(
    CreateDrinksRequest request,
    UUID currentShopID,
    String roleName
  ) {

    UUID targetShopId = validateShopId(roleName, request.getShopId(), currentShopID);

    Boolean isDrinkExisted = createDrinksMapper.checkDrinkExistedByName(targetShopId, request.getDrinkName());
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

    createDrinksMapper.createDrink(
      request.getDrinkCategoryId(),
      request.getDrinkDetailId(),
      targetShopId,
      request.getDrinkName(),
      request.getImageUrl(),
      request.getStatus(),
      request.getIsDeleted(),
      size,
      request.getPrice()
    );
  }

  private UUID validateShopId(String roleName, UUID requestShopId, UUID currentShopId) {
    if ("OWNER".equals(roleName)) {
      return requestShopId;
    }
    return currentShopId;
  }
}