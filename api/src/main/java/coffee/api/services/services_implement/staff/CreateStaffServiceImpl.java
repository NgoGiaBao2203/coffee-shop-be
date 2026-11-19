package coffee.api.services.services_implement.staff;

import coffee.api.constants.ErrorMessage;
import coffee.api.dto.request.staff.CreateStaffRequest;
import coffee.api.exceptions.EmailExistedException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateStaffMapper;
import coffee.api.services.services_implement.staff.common.StaffValidator;
import coffee.api.services.services_interface.staff.ICreateStaffService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateStaffServiceImpl implements ICreateStaffService {
  private final CommonMapper commonMapper;
  private final CreateStaffMapper createStaffMapper;
  private final StaffValidator staffValidator;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(
      CreateStaffRequest request,
      String currentUserRoleName,
      UUID currentUserId,
      UUID currentUserShopId) {
    // 1. Verify shop access and the role that will be assigned
    staffValidator.validateShopAccess(
        request.getShopId(), currentUserRoleName, currentUserId, currentUserShopId);
    staffValidator.validateAssignableRole(request.getRoleId(), currentUserRoleName);

    // 2. Normalize input
    String username = request.getUsername().trim();
    String email = request.getEmail().trim();
    String phoneNumber = request.getPhoneNumber().trim();
    String fullName = request.getFullName().trim();

    // 3. Verify username, email and phone number are not used by an active account
    if (commonMapper.checkStaffExistedByUsername(username)) {
      throw new UserExistException(ErrorMessage.STAFF_USERNAME_EXISTED);
    }
    if (commonMapper.checkStaffEmailExisted(email)) {
      throw new EmailExistedException(ErrorMessage.STAFF_EMAIL_EXISTED, email);
    }
    staffValidator.validatePhoneNotUsed(phoneNumber, null);

    // 4. Insert profile and shop assignment
    createStaffMapper.createStaff(
        email,
        username,
        fullName,
        phoneNumber,
        request.getRoleId(),
        request.getShopId(),
        currentUserId,
        currentUserRoleName);
  }
}
