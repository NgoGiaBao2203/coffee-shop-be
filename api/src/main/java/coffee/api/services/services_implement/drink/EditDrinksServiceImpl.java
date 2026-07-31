package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.drink.IEditDrinksService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditDrinksServiceImpl implements IEditDrinksService {

  private final CommonMapper commonMapper;
  private final UpdateDrinkMapper editDrinkMapper;

  @Override
  @Transactional
  public void process(
    EditDrinksRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  ) {
    Boolean isDrinkExisted = commonMapper.checkDrinkExisted(
      request.getDrinkId(),
      currentUserRoleName,
      currentUserShopId
    );

    if (Boolean.FALSE.equals(isDrinkExisted)) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }

    editDrinkMapper.updateDrink(
      request.getDrinkId(),
      request.getDrinkName(),
      request.getImageUrl(),
      request.getStatus(),
      request.getDrinkCategoryId(),
      request.getPrice(),
      request.getSize(),
      currentUserShopId,
      currentUserRoleName
    );
  }
}