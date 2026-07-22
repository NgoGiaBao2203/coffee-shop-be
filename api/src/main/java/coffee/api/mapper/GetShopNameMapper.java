package coffee.api.mapper;

import coffee.api.dto.result.ShopNameResult;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GetShopNameMapper {
  List<ShopNameResult> getShopNames();
}
