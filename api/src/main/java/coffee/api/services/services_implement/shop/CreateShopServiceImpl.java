package coffee.api.services.services_implement.shop;

import coffee.api.dto.request.shop.CreateShopRequest;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CreateShopMapper;
import coffee.api.services.services_interface.shop.ICreateShopService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateShopServiceImpl implements ICreateShopService {

  private final CreateShopMapper createShopMapper;

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void process(CreateShopRequest request, UUID currentUserId, String currentRoleName) {
    if (currentUserId == null || !"OWNER".equals(currentRoleName)) {
      throw new InvalidRequestException("Only authenticated owners can create shops");
    }

    String trimmedShopName = request.getShopName().trim();
    String trimmedAddress = request.getAddress().trim();
    String trimmedPhoneNumber = request.getPhoneNumber().trim();

    // 1. Verify unique shop name
    Boolean isShopExisted = createShopMapper.checkShopExistedByName(trimmedShopName);
    if (Boolean.TRUE.equals(isShopExisted)) {
      throw new InvalidRequestException("Shop name already exists");
    }

    // 2. Verify unique shop address
    Boolean isAddressExisted = createShopMapper.checkShopExistedByAddress(trimmedAddress);
    if (Boolean.TRUE.equals(isAddressExisted)) {
      throw new InvalidRequestException("Shop address already exists");
    }

    UUID newShopId = UUID.randomUUID();

    // 3. Create shop entity
    createShopMapper.createShop(
        newShopId, trimmedShopName, trimmedAddress, trimmedPhoneNumber, false);
  }
}
