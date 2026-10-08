package coffee.api.services.services_interface.category;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import java.util.UUID;

public interface IDeleteCategoriesService {
  void process(
      DeleteCategoriesRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
