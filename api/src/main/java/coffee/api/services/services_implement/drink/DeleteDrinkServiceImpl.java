package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
import coffee.api.services.services_interface.drink.IDeleteDrinkService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteDrinkServiceImpl implements IDeleteDrinkService {

  private final CommonMapper commonMapper;
  private final DeleteDrinkMapper deleteDrinkMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteDrinksRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {

    // 1. Verify drink ID input
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }

    // 2. Verify manager role belongs to active shop
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException("Manager is not assigned to any shop");
      }

      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }
    }

    // 3. Verify drink existence and role/shop access
    Boolean isDrinkExisted =
        commonMapper.checkDrinkExisted(
            request.getDrinkId(), currentUserRoleName, currentUserShopId);

    if (Boolean.FALSE.equals(isDrinkExisted)) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    // 4. Soft-delete only the drinks record and verify affected rows
    int affectedRows =
        deleteDrinkMapper.deleteDrink(
            request.getDrinkId(), currentUserId, currentUserRoleName, currentUserShopId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Drink has already been deleted or modified by another request", request.getDrinkId());
    }
  }
}
