package coffee.api.services.services_implement.drink_detail;

import coffee.api.dto.request.drink_detail.EditDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.UpdateDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import coffee.api.services.services_interface.drink_detail.IEditDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditDrinkDetailServiceImpl implements IEditDrinkDetailService {

  private final UpdateDrinkDetailMapper updateDrinkDetailMapper;
  private final DrinkDetailValidator drinkDetailValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditDrinkDetailRequest request,
      UUID currentUserId,
      String currentUserRoleName,
      UUID currentUserShopId) {
    // 1. Validate the request and the active shop assignment
    validateRequest(request);
    drinkDetailValidator.verifyShopAssignment(
        currentUserId, currentUserRoleName, currentUserShopId);

    // 2. Verify the drink detail is active and accessible to the current user
    DrinkDetailResult existingDetail =
        drinkDetailValidator.validateDrinkDetailExists(
            request.getDrinkDetailId(), currentUserRoleName, currentUserShopId);
    drinkDetailValidator.validateDrinkExists(
        existingDetail.getDrinkId(), currentUserRoleName, currentUserShopId);

    // 3. Validate price and prevent duplicate active sizes
    drinkDetailValidator.validatePrice(request.getPrice());
    String size = drinkDetailValidator.validateAndFormatSize(request.getSize());
    drinkDetailValidator.validateSizeAvailable(
        existingDetail.getDrinkId(), size, request.getDrinkDetailId());

    // 4. Update the drink detail and verify that it was not deleted concurrently
    int affectedRows =
        updateDrinkDetailMapper.updateDrinkDetail(
            request.getDrinkDetailId(),
            size,
            request.getPrice(),
            currentUserRoleName,
            currentUserShopId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Drink detail has already been deleted or modified", request.getDrinkDetailId());
    }
  }

  private void validateRequest(EditDrinkDetailRequest request) {
    if (request == null || request.getDrinkDetailId() == null) {
      throw new InvalidRequestException("Drink detail ID is required");
    }
  }
}
