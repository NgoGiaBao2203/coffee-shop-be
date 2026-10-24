package coffee.api.services.services_implement.drink_detail.common;

import coffee.api.dto.result.DrinkDetailResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateDrinkDetailMapper;
import coffee.api.mapper.GetDrinkDetailMapper;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrinkDetailValidator {

  private final CreateDrinkDetailMapper createDrinkDetailMapper;
  private final GetDrinkDetailMapper getDrinkDetailMapper;
  private final CommonMapper commonMapper;

  /** Verify the user has an active shop assignment before reading or changing drink details. */
  public void verifyShopAssignment(
      UUID currentUserId, String currentUserRoleName, UUID currentUserShopId) {
    if (Roles.OWNER.getValue().equals(currentUserRoleName)) {
      return;
    }
    if (currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop");
    }
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
      if (!isShopMember) {
        throw new InvalidRequestException("You do not have permission to access this shop");
      }
    }
  }

  /** Verify the drink is active and visible to the current user's role and shop. */
  public void validateDrinkExists(
      UUID drinkId, String currentUserRoleName, UUID currentUserShopId) {
    if (drinkId == null) {
      throw new InvalidRequestException("Drink ID is required");
    }
    if (!createDrinkDetailMapper.checkDrinkAccessible(
        drinkId, currentUserRoleName, currentUserShopId)) {
      throw new DataNotFoundException("Drink not found", drinkId);
    }
  }

  /** Return an active drink detail visible to the current user, or throw when it is unavailable. */
  public DrinkDetailResult validateDrinkDetailExists(
      UUID drinkDetailId, String currentUserRoleName, UUID currentUserShopId) {
    DrinkDetailResult drinkDetail =
        getDrinkDetailMapper.getDrinkDetailVariantById(
            drinkDetailId, currentUserShopId, currentUserRoleName);
    if (drinkDetail == null) {
      throw new DataNotFoundException("Drink detail not found", drinkDetailId);
    }
    return drinkDetail;
  }

  /** Normalize and validate a drink detail size without relying on drink-domain validation. */
  public String validateAndFormatSize(String size) {
    String formattedSize = size == null ? "" : size.trim().toUpperCase();
    if (!Set.of("S", "M", "L").contains(formattedSize)) {
      throw new InvalidRequestException("Drink detail size is invalid");
    }
    return formattedSize;
  }

  /** Reject null, zero, and negative prices before a database write. */
  public void validatePrice(BigDecimal price) {
    if (price == null || price.signum() <= 0) {
      throw new InvalidRequestException("Drink detail price must be greater than zero");
    }
  }

  /** Prevent two active variants of the same drink from using the same size. */
  public void validateSizeAvailable(UUID drinkId, String size, UUID excludedDrinkDetailId) {
    if (commonMapper.checkDrinkDetailSizeExisted(drinkId, size, excludedDrinkDetailId)) {
      throw new InvalidRequestException("Drink detail size already exists");
    }
  }
}
