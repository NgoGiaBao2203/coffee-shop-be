package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
import coffee.api.services.services_interface.drink.IDeleteDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteDrinkServiceImpl implements IDeleteDrinkService {

  private final CommonMapper commonMapper;
  private final DeleteDrinkMapper deleteDrinkMapper;

  @Override
  public void process(DeleteDrinksRequest request) {
    Boolean isDrinkExisted = commonMapper.checkDrinkExisted(request.getDrinkId());
    if (!isDrinkExisted) {
      throw new DataNotFoundException("Data not found", request.getDrinkId());
    }
    deleteDrinkMapper.deleteDrink(request.getDrinkId());
  }
}