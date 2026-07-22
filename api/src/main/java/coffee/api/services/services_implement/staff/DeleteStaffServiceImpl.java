package coffee.api.services.services_implement.staff;

import coffee.api.dto.request.user.DeleteStaffRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteStaffMapper;
import coffee.api.services.services_interface.staff.IDeleteStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteStaffServiceImpl implements IDeleteStaffService {
  private final CommonMapper commonMapper;
  private final DeleteStaffMapper deleteStaffMapper;

  @Override
  public void process(DeleteStaffRequest request) {
    Boolean isStaffExited = commonMapper.checkStaffExisted(request.getProfileId());
    if (!isStaffExited) {
      throw new DataNotFoundException("Data not found", request.getProfileId());
    }
    deleteStaffMapper.deleteStaff(request.getProfileId());
  }
}
