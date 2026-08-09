package coffee.api.mapper;

import coffee.api.dto.result.RoleResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GetRoleMapper {
  List<RoleResult> getRoles();
}
