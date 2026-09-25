package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.EditShopRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.UpdateShopMapper;
import coffee.api.services.services_interface.shop.IEditShopService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditShopServiceImpl implements IEditShopService {

  private final UpdateShopMapper updateShopMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(UUID shopId, EditShopRequest request) {
    String shopName = request.getShopName().trim();
    String address = request.getAddress().trim();

    // 1. Verify existence of the shop
    Boolean isShopExisted = updateShopMapper.checkShopExistedById(shopId);
    if (!Boolean.TRUE.equals(isShopExisted)) {
      throw new DataNotFoundException("Data not found", shopId);
    }

    // 2. Check if shop name is already taken by another active shop
    Boolean isNameExisted = updateShopMapper.checkShopExistedByNameExceptCurrent(shopId, shopName);
    if (Boolean.TRUE.equals(isNameExisted)) {
      throw new UserExistException("Shop name is existed");
    }

    // 3. Check if address is already taken by another active shop
    Boolean isAddressExisted =
        updateShopMapper.checkShopExistedByAddressExceptCurrent(shopId, address);
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new UserExistException("Address is existed");
    }

    String phoneNumber = request.getPhoneNumber().trim();
    updateShopMapper.updateShop(shopId, shopName, address, phoneNumber);
  }
}
