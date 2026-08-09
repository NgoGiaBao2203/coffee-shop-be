package coffee.api.mapper;

import coffee.api.dto.result.ShopNameResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GetShopNameMapper {
  List<ShopNameResult> getShopNames();
}
