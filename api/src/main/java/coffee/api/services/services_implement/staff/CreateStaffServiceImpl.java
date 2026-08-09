package coffee.api.services.services_implement.staff;

import coffee.api.dto.request.user.CreateStaffRequest;
import coffee.api.exceptions.EmailExistedException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateStaffMapper;
import coffee.api.services.services_interface.staff.ICreateStaffService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateStaffServiceImpl implements ICreateStaffService {
  private final CommonMapper commonMapper;
  private final CreateStaffMapper createStaffMapper;

  @Override
  public void process(CreateStaffRequest request, UUID currentUserId, String currentUserRoleName) {
    Boolean isStaffExited = commonMapper.checkStaffExistedByUsername(request.getUsername());
    if (isStaffExited) {
      throw new UserExistException("Staff is existed");
    }
    Boolean isPhoneNumberExisted =
        commonMapper.checkStaffPhoneExistedForCreate(request.getPhoneNumber());
    if (isPhoneNumberExisted) {
      throw new PhoneNumberExistedException("Staff phone is existed", request.getPhoneNumber());
    }
    Boolean isEmailExisted = commonMapper.checkStaffEmailExisted(request.getEmail());
    if (isEmailExisted) {
      throw new EmailExistedException("Staff email is existed", request.getEmail());
    }
    createStaffMapper.createStaff(
        request.getEmail(),
        request.getUsername(),
        request.getFullName(),
        request.getPhoneNumber(),
        request.getRoleId(),
        request.getShopId(),
        currentUserId,
        currentUserRoleName);
  }
}
