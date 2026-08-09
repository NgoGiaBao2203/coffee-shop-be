package coffee.api.services.services_interface.category;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import java.util.UUID;

public interface ICreateCategoriesService {
  void process(CreateCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId);
}
