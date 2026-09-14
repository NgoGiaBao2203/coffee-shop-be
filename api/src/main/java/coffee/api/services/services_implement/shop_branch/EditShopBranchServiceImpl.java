package coffee.api.services.services_implement.shop_branch;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.UpdateShopBranchMapper;
import coffee.api.services.services_interface.shop_branch.IEditShopBranchService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditShopBranchServiceImpl implements IEditShopBranchService {

  private final UpdateShopBranchMapper updateShopBranchMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(EditShopBranchRequest request) {
    UUID shopId = request.getShopId();
    String shopName = request.getShopName().trim();
    String address = request.getAddress().trim();

    // 1. Verify existence of the shop branch
    Boolean isShopExisted = updateShopBranchMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Check if shop name is already taken by another active branch
    Boolean isNameExisted =
        updateShopBranchMapper.checkShopExistedByNameExceptCurrent(shopId, shopName);
    if (Boolean.TRUE.equals(isNameExisted)) {
      throw new UserExistException("Shop name is existed");
    }

    // 3. Check if address is already taken by another active branch
    Boolean isAddressExisted =
        updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(shopId, address);
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new UserExistException("Address is existed");
    }

    String phoneNumber = request.getPhoneNumber().trim();
    updateShopBranchMapper.updateShopBranch(shopId, shopName, address, phoneNumber);
  }
}
