package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.CreateShopBranchRequest;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateShopBranchMapper;
import coffee.api.services.services_interface.shop_branch.ICreateShopBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateShopBranchServiceImpl implements ICreateShopBranchService {

  private final CreateShopBranchMapper createShopBranchMapper;

  @Override
  public void process(
      CreateShopBranchRequest request, String currentUserRoleName, UUID currentUserId) {

    Boolean isShopExisted =
        createShopBranchMapper.checkShopExistedByName(request.getShopName().trim());
    if (Boolean.TRUE.equals(isShopExisted)) {
      throw new UserExistException("Shop name is existed");
    }

    Boolean isAddressExisted =
        createShopBranchMapper.checkShopExistedByAddress(request.getAddress().trim());
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new UserExistException("Address is existed");
    }

    if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
      Boolean isPhoneExisted =
          createShopBranchMapper.checkShopExistedByPhone(request.getPhoneNumber().trim());
      if (Boolean.TRUE.equals(isPhoneExisted)) {
        throw new UserExistException("Phone number is existed");
      }
    }

    UUID newShopId = request.getShopId() != null ? request.getShopId() : UUID.randomUUID();
    Boolean isDeleted = Boolean.TRUE.equals(request.getIsDeleted());

    createShopBranchMapper.createShopBranch(
        newShopId,
        request.getShopName(),
        request.getAddress(),
        request.getPhoneNumber(),
        isDeleted);
  }
}
