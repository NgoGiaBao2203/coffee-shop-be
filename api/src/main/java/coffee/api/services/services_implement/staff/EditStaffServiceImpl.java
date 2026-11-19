package coffee.api.services.services_implement.staff;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.staff.EditStaffRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateStaffMapper;
import coffee.api.services.services_implement.staff.common.StaffValidator;
import coffee.api.services.services_interface.staff.IEditStaffService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditStaffServiceImpl implements IEditStaffService {
  private final CommonMapper commonMapper;
  private final UpdateStaffMapper updateStaffMapper;
  private final StaffValidator staffValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      EditStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    // 1. Verify the target is inside the caller's staff scope
    staffValidator.validateManageable(request.getProfileId(), currentUserRoleName, currentUserId);

    // 2. Verify shop access and the role that will be assigned
    staffValidator.validateShopAccess(
        request.getShopId(), currentUserRoleName, currentUserId, currentUserShopId);
    staffValidator.validateAssignableRole(request.getRoleId(), currentUserRoleName);

    // 3. Verify the phone number is not used by another active account
    String phoneNumber = request.getPhoneNumber().trim();
    staffValidator.validatePhoneNotUsed(phoneNumber, request.getProfileId());

    // 4. Update the profile and verify it still exists
    int affectedRows =
        updateStaffMapper.updateStaff(
            request.getProfileId(),
            request.getFullName().trim(),
            phoneNumber,
            request.getRoleId(),
            currentUserRoleName);
    if (affectedRows == 0) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, request.getProfileId());
    }

    // 5. Only OWNER changes the shop assignment; a staff works in exactly one shop
    if (Roles.OWNER.getValue().equals(currentUserRoleName)) {
      moveStaffToShop(request, currentUserId);
    }
  }

  /** A different shop means moving: leave the old shop(s), then set the role in the new shop. */
  private void moveStaffToShop(EditStaffRequest request, UUID currentUserId) {
    UUID profileId = request.getProfileId();
    if (commonMapper.countOtherActiveStaffShops(profileId, request.getShopId()) > 0) {
      staffValidator.validateNoPendingInvoices(profileId, null);
      updateStaffMapper.removeOtherStaffShops(profileId, request.getShopId(), currentUserId);
    }
    int updatedShops =
        updateStaffMapper.updateStaffShop(profileId, request.getShopId(), request.getRoleId());
    if (updatedShops == 0) {
      updateStaffMapper.insertStaffShop(profileId, request.getShopId(), request.getRoleId());
    }
  }
}
