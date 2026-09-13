package coffee.api.services.services_interface.dropdown;

import coffee.api.dto.result.CategoryDropdownResult;
import java.util.List;
import java.util.UUID;

public interface ICategoryDropdownService {
  List<CategoryDropdownResult> process(String roleName, UUID shopId);
}
