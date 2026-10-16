package coffee.api.services.services_implement.detail;

import coffee.api.dto.result.DrinkResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.GetDrinkDetailMapper;
import coffee.api.services.services_interface.detail.IGetDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDrinkDetailServiceImpl implements IGetDrinkDetailService {

  private final GetDrinkDetailMapper getDrinkDetailMapper;

  @Override
  public DrinkResult process(UUID drinkId, String currentUserRoleName, UUID currentUserShopId) {
    DrinkResult drink =
        getDrinkDetailMapper.getDrinkDetailById(drinkId, currentUserShopId, currentUserRoleName);

    if (drink == null) {
      throw new DataNotFoundException("Drink not found with id: " + drinkId, drinkId);
    }

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
