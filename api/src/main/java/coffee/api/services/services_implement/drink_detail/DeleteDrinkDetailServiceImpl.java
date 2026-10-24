package coffee.api.services.services_implement.drink_detail;

import coffee.api.dto.request.drink_detail.DeleteDrinkDetailRequest;
import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteDrinkDetailMapper;
import coffee.api.services.services_implement.drink_detail.common.DrinkDetailValidator;
import coffee.api.services.services_interface.drink_detail.IDeleteDrinkDetailService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteDrinkDetailServiceImpl implements IDeleteDrinkDetailService {

  private final DeleteDrinkDetailMapper deleteDrinkDetailMapper;
  private final DrinkDetailValidator drinkDetailValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteDrinkDetailRequest request,
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

    // 3. Keep at least one active size for the drink
    validateCanDeleteDetail(existingDetail.getDrinkId());

    // 4. Soft-delete the drink detail and verify that it was not changed concurrently
    int affectedRows =
        deleteDrinkDetailMapper.softDeleteDrinkDetail(
            request.getDrinkDetailId(), currentUserId, currentUserRoleName, currentUserShopId);
    if (affectedRows == 0) {
      throw new DataNotFoundException(
          "Drink detail has already been deleted or modified", request.getDrinkDetailId());
    }
  }

  private void validateRequest(DeleteDrinkDetailRequest request) {
    if (request == null || request.getDrinkDetailId() == null) {
      throw new InvalidRequestException("Drink detail ID is required");
    }
  }

  private void validateCanDeleteDetail(UUID drinkId) {
    if (deleteDrinkDetailMapper.countActiveDrinkDetails(drinkId) <= 1) {
      throw new InvalidRequestException("A drink must have at least one active size");
    }
  }
}
