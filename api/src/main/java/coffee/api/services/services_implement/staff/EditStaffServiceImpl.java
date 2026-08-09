package coffee.api.services.services_implement.staff;

import coffee.api.dto.request.user.EditStaffRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateStaffMapper;
import coffee.api.services.services_interface.staff.IEditStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditStaffServiceImpl implements IEditStaffService {
  private final CommonMapper commonMapper;
  private final UpdateStaffMapper updateStaffMapper;

  @Override
  public void process(EditStaffRequest request, String currentUserRoleName) {
    Boolean isStaffExited = commonMapper.checkStaffExisted(request.getProfileId());
    if (!isStaffExited) {
      throw new DataNotFoundException("Data not found", request.getProfileId());
    }
    Boolean isStaffPhoneExited =
        commonMapper.checkStaffPhoneExisted(request.getProfileId(), request.getPhoneNumber());
    if (isStaffPhoneExited) {
      throw new PhoneNumberExistedException("Phone number exited", request.getPhoneNumber());
    }
    updateStaffMapper.updateStaff(
        request.getProfileId(),
        request.getFullName(),
        request.getPhoneNumber(),
        request.getRoleId(),
        request.getShopId(),
        currentUserRoleName);
  }
}
