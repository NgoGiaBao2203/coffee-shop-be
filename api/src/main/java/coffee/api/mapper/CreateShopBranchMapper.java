package coffee.api.mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CreateShopBranchMapper {

  Boolean checkShopExistedByName(@Param("shopName") String shopName);

  Boolean checkShopExistedByAddress(@Param("address") String address);

  Boolean checkShopExistedByPhone(@Param("phoneNumber") String phoneNumber);

  void createShopBranch(
      @Param("shopId") UUID shopId,
      @Param("shopName") String shopName,
      @Param("address") String address,
      @Param("phoneNumber") String phoneNumber,
      @Param("isDeleted") Boolean isDeleted);
}
