package coffee.api.services.services_implement.staff.common;

import coffee.api.constants.ErrorMessage;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.mapper.CommonMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Checks shared by the create, edit and delete staff use cases. */
@Component
@RequiredArgsConstructor
public class StaffValidator {
  private final CommonMapper commonMapper;

  /** The shop must exist, and a MANAGER may only act on the shop they belong to. */
  public void validateShopAccess(
      UUID shopId, String currentUserRoleName, UUID currentUserId, UUID currentUserShopId) {
    if (!commonMapper.checkShopExisted(shopId)) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, shopId);
    }
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (currentUserShopId == null) {
        throw new InvalidRequestException(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH);
      }
      if (!currentUserShopId.equals(shopId)
          || !commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)) {
        throw new InvalidRequestException(ErrorMessage.SHOP_BRANCH_ACCESS_DENIED);
      }
    }
  }

  /** The role must exist and must not be OWNER; a MANAGER can not choose a role at all. */
  public void validateAssignableRole(UUID roleId, String currentUserRoleName) {
    if (!Roles.OWNER.getValue().equals(currentUserRoleName)) {
      return;
    }
    String roleName = commonMapper.findRoleNameById(roleId);
    if (roleName == null) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, roleId);
    }
    if (Roles.OWNER.getValue().equals(roleName)) {
      throw new InvalidRequestException(ErrorMessage.ROLE_NOT_ASSIGNABLE);
    }
  }

  /** The phone number must not belong to another active account (the same profile is ignored). */
  public void validatePhoneNotUsed(String phoneNumber, UUID excludedProfileId) {
    if (commonMapper.checkStaffPhoneExisted(phoneNumber, excludedProfileId)) {
      throw new PhoneNumberExistedException(ErrorMessage.STAFF_PHONE_EXISTED, phoneNumber);
    }
  }

  /** Unpaid invoices must stay with their staff: none may exist in the shop (any shop if null). */
  public void validateNoPendingInvoices(UUID profileId, UUID shopId) {
    if (commonMapper.countPendingInvoicesByStaff(profileId, shopId) > 0) {
      throw new InvalidRequestException(ErrorMessage.STAFF_HAS_PENDING_INVOICES);
    }
  }

  /** The target profile must be inside the caller's staff scope (never an OWNER or themselves). */
  public void validateManageable(UUID profileId, String currentUserRoleName, UUID currentUserId) {
    if (!commonMapper.checkStaffManageable(profileId, currentUserRoleName, currentUserId)) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, profileId);
    }
  }
}
