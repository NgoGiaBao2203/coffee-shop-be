package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.UpdateShopBranchMapper;
import coffee.api.services.services_interface.shop_branch.IEditShopBranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditShopBranchServiceImpl implements IEditShopBranchService {

  private final UpdateShopBranchMapper updateShopBranchMapper;

  @Override
  public void process(EditShopBranchRequest request) {

    Boolean isShopExisted = updateShopBranchMapper.checkShopExistedById(request.getShopId());
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    Boolean isNameExisted =
        updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            request.getShopId(), request.getShopName().trim());
    if (Boolean.TRUE.equals(isNameExisted)) {
      throw new UserExistException("Shop name is existed");
    }

    Boolean isAddressExisted =
        updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            request.getShopId(), request.getAddress().trim());
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new UserExistException("Address is existed");
    }

    if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
      Boolean isPhoneExisted =
          updateShopBranchMapper.checkShopExistedByPhoneExceptCurrent(
              request.getShopId(), request.getPhoneNumber().trim());
      if (Boolean.TRUE.equals(isPhoneExisted)) {
        throw new UserExistException("Phone number is existed");
      }
    }

    Boolean isDeleted = Boolean.TRUE.equals(request.getIsDeleted());

    String phoneNumber =
        (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty())
            ? request.getPhoneNumber().trim()
            : null;

    updateShopBranchMapper.updateShopBranch(
        request.getShopId(),
        request.getShopName().trim(),
        request.getAddress().trim(),
        phoneNumber,
        isDeleted);
  }
}
