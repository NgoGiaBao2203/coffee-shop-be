package coffee.api.utils;

import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import java.util.UUID;

public final class DrinkValidationHelper {

  private DrinkValidationHelper() {
    // Utility class
  }

  public static void validateManagerShopPermission(
      String roleName,
      UUID currentShopId,
      UUID targetShopId,
      UUID currentUserId,
      String mismatchShopMessage,
      CommonMapper commonMapper) {
    if (!Roles.MANAGER.getValue().equals(roleName)) {
      return;
    }

    if (currentShopId == null) {
      throw new InvalidRequestException("Manager is not assigned to any shop");
    }

    if (!currentShopId.equals(targetShopId)) {
      throw new InvalidRequestException(mismatchShopMessage);
    }

    boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentShopId);
    if (!isShopMember) {
      throw new InvalidRequestException("You do not have permission to access this shop");
    }
  }

  public static void validateCategoryBelongsToShopForCreate(
      UUID categoryId, UUID shopId, CommonMapper commonMapper) {
    boolean isCategoryBelongsToShop = commonMapper.checkCategoryExistsInShop(categoryId, shopId);
    if (!isCategoryBelongsToShop) {
      throw new DataNotFoundException("Data not found", categoryId);
    }
  }

  public static void validateCategoryBelongsToShopForEdit(
      UUID categoryId, UUID shopId, CommonMapper commonMapper) {
    boolean isCategoryBelongsToShop = commonMapper.checkCategoryExistsInShop(categoryId, shopId);
    if (!isCategoryBelongsToShop) {
      throw new InvalidRequestException("Category does not belong to the drink's shop");
    }
  }

  public static String validateAndFormatName(String rawName) {
    return rawName != null ? rawName.trim() : "";
  }

  public static String validateAndFormatSize(String rawSize) {
    String size = rawSize == null ? "" : rawSize.trim().toUpperCase();
    if (size.isEmpty()) {
      throw new InvalidRequestException("Drink size is required");
    }
    return size;
  }
}
