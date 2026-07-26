package coffee.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface DeleteDrinkMapper {
  void deleteDrink(@Param("drinkId") UUID drinkId);
}