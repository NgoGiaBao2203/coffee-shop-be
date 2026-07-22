package coffee.api.mapper;

import coffee.api.dto.result.RoleResult;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GetRoleMapper {
  List<RoleResult> getRoles();
}
