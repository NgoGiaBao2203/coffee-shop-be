package coffee.api.services.services_implement.table.common;

import coffee.api.constants.ErrorMessage;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TableValidator {

  private final CommonMapper commonMapper;

  /** Verify shop exists in the system. */
  public void validateShopExists(UUID shopId) {
    boolean isShopExisted = commonMapper.checkShopExisted(shopId);
    if (!isShopExisted) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, shopId);
    }
  }

  /** Verify table existence and role/shop access. */
  public void validateTableExists(
      UUID tableId, String currentUserRoleName, UUID currentUserShopId) {

    boolean isTableExisted =
        commonMapper.checkTableExisted(tableId, currentUserRoleName, currentUserShopId);
    if (!isTableExisted) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, tableId);
    }
  }

  /** Verify manager role belongs to active shop branch. */
  public void verifyManagerAssignment(
      UUID currentUserId, String currentUserRoleName, UUID currentUserShopId) {

    if (!Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      return;
    }

    if (currentUserShopId == null) {
      throw new InvalidRequestException(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH);
    }

    boolean isShopMember = commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId);
    if (!isShopMember) {
      throw new InvalidRequestException(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED);
    }
  }

  /** Verify authorization and active assignment for MANAGER with target shop branch. */
  public void verifyManagerAuthorization(
      UUID currentUserId, String currentUserRoleName, UUID currentUserShopId, UUID requestShopId) {

    verifyManagerAssignment(currentUserId, currentUserRoleName, currentUserShopId);

    if (Roles.MANAGER.getValue().equals(currentUserRoleName)
        && !currentUserShopId.equals(requestShopId)) {
      throw new InvalidRequestException(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED);
    }
  }

  /** Trim description string. */
  public String sanitizeDescription(String description) {
    if (description == null) {
      return null;
    }
    String trimmed = description.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
