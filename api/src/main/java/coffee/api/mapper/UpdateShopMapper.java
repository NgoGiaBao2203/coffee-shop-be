package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateShopMapper {

  Boolean checkShopExistedById(@Param("shopId") UUID shopId);

  Boolean checkShopExistedByNameExceptCurrent(
      @Param("shopId") UUID shopId, @Param("shopName") String shopName);

  Boolean checkShopExistedByAddressExceptCurrent(
      @Param("shopId") UUID shopId, @Param("address") String address);

  void updateShop(
      @Param("shopId") UUID shopId,
      @Param("shopName") String shopName,
      @Param("address") String address,
      @Param("phoneNumber") String phoneNumber);
}
