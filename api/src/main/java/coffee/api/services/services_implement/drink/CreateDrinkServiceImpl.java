package coffee.api.services.services_implement.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_interface.drink.ICreateDrinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateDrinkServiceImpl implements ICreateDrinkService {

  private final CreateDrinkMapper createDrinksMapper;

  @Override
  @Transactional
  public void process(
    CreateDrinksRequest request
  ) {
    Boolean isDrinkExisted = createDrinksMapper.checkDrinkExistedByName(
      request.getDrinkName(),
      request.getShopID()
    );

    if (Boolean.TRUE.equals(isDrinkExisted)) {
      throw new RuntimeException("The drink already exists.");
    }

    createDrinksMapper.createDrink(
      request.getDrinkId(),
      request.getDrinkName(),
      request.getImageUrl(),
      request.getStatus(),
      request.getIsDeleted(),
      request.getShopID(),
      request.getDrinkCategoryId()
    );

    UUID newDrinkDetailId = UUID.randomUUID();
    createDrinksMapper.createDrinkDetail(
      newDrinkDetailId,
      request.getSize(),
      request.getPrice(),
      request.getDrinkId()
    );
  }
}