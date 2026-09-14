package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UpdateShopBranchMapper {

  Boolean checkShopExistedById(@Param("shopId") UUID shopId);

  Boolean checkShopExistedByNameExceptCurrent(
      @Param("shopId") UUID shopId, @Param("shopName") String shopName);

  Boolean checkShopExistedByAddressExceptCurrent(
      @Param("shopId") UUID shopId, @Param("address") String address);

  void updateShopBranch(
      @Param("shopId") UUID shopId,
      @Param("shopName") String shopName,
      @Param("address") String address,
      @Param("phoneNumber") String phoneNumber);
}
