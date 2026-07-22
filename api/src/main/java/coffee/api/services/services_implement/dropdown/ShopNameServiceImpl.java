package coffee.api.services.services_implement.dropdown;

import coffee.api.dto.result.ShopNameResult;
import coffee.api.mapper.GetShopNameMapper;
import coffee.api.services.services_interface.dropdown.IShopNameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShopNameServiceImpl implements IShopNameService {
  private final GetShopNameMapper getShopNameMapper;

  @Override
  public List<ShopNameResult> process() {
    return getShopNameMapper.getShopNames();
  }
}
