package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateShopMapper {

  Boolean checkShopExistedByName(@Param("shopName") String shopName);

  Boolean checkShopExistedByAddress(@Param("address") String address);

  void createShop(
      @Param("shopId") UUID shopId,
      @Param("shopName") String shopName,
      @Param("address") String address,
      @Param("phoneNumber") String phoneNumber,
      @Param("isDeleted") Boolean isDeleted);
}
