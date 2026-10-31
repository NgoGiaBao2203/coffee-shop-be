package coffee.api.services.services_implement.drink_detail;

import coffee.api.dto.request.drink_detail.CreateDrinkDetailRequest;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import coffee.api.services.services_interface.drink_detail.ICreateDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateDrinkDetailServiceImpl implements ICreateDrinkDetailService {

  private final CreateDrinkDetailMapper createDrinkDetailMapper;
  private final DrinkDetailValidator drinkDetailValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateDrinkDetailRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {
    // 1. Validate the request and the active shop assignment
    validateRequest(request);
    drinkDetailValidator.verifyShopAssignment(
        currentUserId, currentUserRoleName, currentUserShopId);

    // 2. Verify the drink is active and accessible to the current user
    drinkDetailValidator.validateDrinkExists(
        request.getDrinkId(), currentUserRoleName, currentUserShopId);

    // 3. Validate price and prevent duplicate active sizes
    drinkDetailValidator.validatePrice(request.getPrice());
    String size = drinkDetailValidator.validateAndFormatSize(request.getSize());
    drinkDetailValidator.validateSizeAvailable(request.getDrinkId(), size, null);

    // 4. Insert the new drink detail
    int affectedRows =
        createDrinkDetailMapper.insertDrinkDetail(
            UUID.randomUUID(), request.getDrinkId(), size, request.getPrice());
    if (affectedRows != 1) {
      throw new InvalidRequestException("Unable to create drink detail");
    }
  }

  private void validateRequest(CreateDrinkDetailRequest request) {
    if (request == null || request.getDrinkId() == null) {
      throw new InvalidRequestException("Drink ID is required");
    }
  }
}
