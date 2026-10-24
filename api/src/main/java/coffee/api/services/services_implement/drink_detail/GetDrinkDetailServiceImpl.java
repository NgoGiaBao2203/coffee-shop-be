package coffee.api.services.services_implement.drink_detail;

import coffee.api.dto.result.DrinkResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import coffee.api.services.services_interface.drink_detail.IGetDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDrinkDetailServiceImpl implements IGetDrinkDetailService {

  private final GetDrinkDetailMapper getDrinkDetailMapper;
  private final DrinkDetailValidator drinkDetailValidator;

  @Override
  public DrinkResult process(
      UUID drinkId, UUID currentUserId, String currentUserRoleName, UUID currentUserShopId) {
    if (drinkId == null) {
      throw new InvalidRequestException("Drink ID is required");
    }

    // Verify the current user's active shop assignment
    drinkDetailValidator.verifyShopAssignment(
        currentUserId, currentUserRoleName, currentUserShopId);

    // 2. Fetch the drink and its active size and price variants
    DrinkResult drink =
        getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName);

    if (drink == null) {
      throw new DataNotFoundException("Drink not found with id: " + drinkId, drinkId);
    }

    // 3. Convert the stored status code to the response label
    drink.setStatus(normalizeStatus(drink.getStatus()));
    return drink;
  }

  private String normalizeStatus(String rawStatus) {
    if (rawStatus == null) return "UNKNOWN";

    return switch (rawStatus) {
      case "1", "ACTIVE" -> "Đang bán";
      case "0", "INACTIVE" -> "Ngừng bán";
      default -> "Không xác định";
    };
  }
}
