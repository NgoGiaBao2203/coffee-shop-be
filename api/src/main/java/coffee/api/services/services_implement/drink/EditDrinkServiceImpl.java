package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateDrinkMapper;
import coffee.api.services.services_interface.drink.IEditDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditDrinkServiceImpl implements IEditDrinkService {
  private final CommonMapper commonMapper;
  private final UpdateDrinkMapper updateDrinkMapper;

  @Override
  public void process(EditDrinksRequest request, String currentUserRoleName) {
  }
}