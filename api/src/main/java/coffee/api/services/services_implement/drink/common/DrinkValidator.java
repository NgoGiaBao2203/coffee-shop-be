package coffee.api.services.services_implement.drink.common;

import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrinkValidator {

  private final CommonMapper commonMapper;

  /** Verify manager role belongs to active shop branch (without target shop). */
  public void verifyManagerAssignment(
      UUID currentUserId, String currentUserRoleName, UUID currentUserShopId) {

    if (!Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      return;
    }

    if (currentUserShopId == null) {
      throw new InvalidRequestException("Manager is not assigned to any shop");
    }

    boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
    if (!isShopMember) {
      throw new InvalidRequestException("You do not have permission to access this shop");
    }
  }

  /** Verify manager authorization against target shop branch. */
  public void validateManagerShopPermission(
      String roleName,
      UUID currentShopId,
      UUID targetShopId,
      UUID currentUserId,
      String mismatchShopMessage) {

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

  /** Verify category exists and belongs to shop for drink creation. */
  public void validateCategoryBelongsToShopForCreate(UUID categoryId, UUID shopId) {
    boolean isCategoryBelongsToShop = commonMapper.checkCategoryExistsInShop(categoryId, shopId);
    if (!isCategoryBelongsToShop) {
      throw new DataNotFoundException("Data not found", categoryId);
    }
  }

  /** Verify category exists and belongs to shop for drink editing. */
  public void validateCategoryBelongsToShopForEdit(UUID categoryId, UUID shopId) {
    boolean isCategoryBelongsToShop = commonMapper.checkCategoryExistsInShop(categoryId, shopId);
    if (!isCategoryBelongsToShop) {
      throw new InvalidRequestException("Category does not belong to the drink's shop");
    }
  }

  /** Verify drink existence and role/shop access. */
  public void validateDrinkExistence(UUID drinkId, String roleName, UUID shopId) {
    Boolean isDrinkExisted = commonMapper.checkDrinkExisted(drinkId, roleName, shopId);
    if (Boolean.FALSE.equals(isDrinkExisted)) {
      throw new DataNotFoundException("Data not found", drinkId);
    }
  }

  /** Trim and format drink name. */
  public String validateAndFormatName(String name) {
    return name != null ? name.trim() : "";
  }

  /** Trim, format uppercase and validate drink size. */
  public String validateAndFormatSize(String size) {
    String formattedSize = size == null ? "" : size.trim().toUpperCase();
    if (formattedSize.isEmpty()) {
      throw new InvalidRequestException("Drink size is required");
    }
    return formattedSize;
  }
}
