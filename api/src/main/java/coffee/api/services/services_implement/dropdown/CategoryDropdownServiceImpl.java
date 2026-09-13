package coffee.api.services.services_implement.dropdown;

import coffee.api.dto.result.CategoryDropdownResult;
import coffee.api.mapper.GetCategoryDropdownMapper;
import coffee.api.services.services_interface.dropdown.ICategoryDropdownService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryDropdownServiceImpl implements ICategoryDropdownService {
  private final GetCategoryDropdownMapper getCategoryDropdownMapper;

  @Override
  public List<CategoryDropdownResult> process(String roleName, UUID shopId) {
    return getCategoryDropdownMapper.getCategoryDropdown(roleName, shopId);
  }
}
