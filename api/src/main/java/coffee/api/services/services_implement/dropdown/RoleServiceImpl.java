package coffee.api.services.services_implement.dropdown;

import coffee.api.dto.result.RoleResult;
import coffee.api.mapper.GetRoleMapper;
import coffee.api.services.services_interface.dropdown.IRoleService;
import coffee.api.utils.ConvertRoleVN;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService {
  private final GetRoleMapper getRoleMapper;

  @Override
  public List<RoleResult> process() {
    List<RoleResult> rawItems = getRoleMapper.getRoles();
    return rawItems.stream()
        .peek(
            roleResult -> {
              String vnRole = ConvertRoleVN.toVietnamese(roleResult.getRoleName());
              roleResult.setRoleDisplayName(vnRole);
            })
        .collect(Collectors.toList());
  }
}
