package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteDrinkMapper;
import coffee.api.services.services_implement.drink.common.DrinkValidator;
import coffee.api.services.services_interface.drink.IDeleteDrinkService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteDrinkServiceImpl implements IDeleteDrinkService {

  private final DeleteDrinkMapper deleteDrinkMapper;
  private final DrinkValidator drinkValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteDrinksRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify drink ID input
    validateRequest(request);

    // 2. Verify manager role belongs to active shop
    drinkValidator.verifyManagerAssignment(currentUserId, currentUserRoleName, currentUserShopId);

    // 3. Verify drink existence and role/shop access
    drinkValidator.validateDrinkExistence(
        request.getDrinkId(), currentUserRoleName, currentUserShopId);

    // 4. Soft-delete only the drinks record and verify affected rows
    softDeleteDrinkRecord(
        request.getDrinkId(), currentUserId, currentUserRoleName, currentUserShopId);
  }

  // 1. Verify drink ID input
  private void validateRequest(DeleteDrinksRequest request) {
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }
  }

  // 4. Soft-delete only the drinks record and verify affected rows
  private void softDeleteDrinkRecord(
      UUID drinkId, UUID currentUserId, String currentUserRoleName, UUID currentUserShopId) {
    int affectedRows =
        deleteDrinkMapper.deleteDrink(
            drinkId, currentUserId, currentUserRoleName, currentUserShopId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Drink has already been deleted or modified by another request", drinkId);
    }
  }
}
