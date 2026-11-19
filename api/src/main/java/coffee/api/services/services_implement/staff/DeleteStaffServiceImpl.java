package coffee.api.services.services_implement.staff;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.staff.DeleteStaffRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteStaffMapper;
import coffee.api.services.services_implement.staff.common.StaffValidator;
import coffee.api.services.services_interface.staff.IDeleteStaffService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteStaffServiceImpl implements IDeleteStaffService {
  private final DeleteStaffMapper deleteStaffMapper;
  private final StaffValidator staffValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      DeleteStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    boolean isOwner = Roles.OWNER.getValue().equals(currentUserRoleName);

    // 1. Verify the target is inside the caller's staff scope
    staffValidator.validateManageable(request.getProfileId(), currentUserRoleName, currentUserId);
    if (!isOwner && currentUserShopId == null) {
      throw new InvalidRequestException(ErrorMessage.MANAGER_NOT_ASSIGNED_TO_SHOP_BRANCH);
    }

    // 2. Keep unpaid invoices intact: OWNER is checked across every shop, MANAGER in their shop
    UUID targetShopId = isOwner ? null : currentUserShopId;
    staffValidator.validateNoPendingInvoices(request.getProfileId(), targetShopId);

    // 3. Soft delete the assignment: OWNER removes every shop, MANAGER only their own shop
    int removedShops =
        deleteStaffMapper.softDeleteStaffShops(request.getProfileId(), targetShopId, currentUserId);
    if (!isOwner && removedShops == 0) {
      throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, request.getProfileId());
    }

    // 4. Soft delete the profile once the staff works in no shop anymore
    if (deleteStaffMapper.countActiveStaffShops(request.getProfileId()) == 0) {
      int affectedRows = deleteStaffMapper.softDeleteStaff(request.getProfileId());
      if (affectedRows == 0) {
        throw new DataNotFoundException(ErrorMessage.DATA_NOT_FOUND, request.getProfileId());
      }
    }
  }
}
