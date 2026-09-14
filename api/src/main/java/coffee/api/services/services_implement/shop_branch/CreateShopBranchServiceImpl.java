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
  public void process(CreateShopBranchRequest request) {
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

    String phoneNumber = request.getPhoneNumber().trim();

    UUID newShopId = UUID.randomUUID();
    Boolean isDeleted = false;

    createShopBranchMapper.createShopBranch(
        newShopId,
        request.getShopName().trim(),
        request.getAddress().trim(),
        phoneNumber,
        isDeleted);
  }
}
