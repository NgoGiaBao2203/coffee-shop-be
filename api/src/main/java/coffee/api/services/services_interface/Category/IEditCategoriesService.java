package coffee.api.services.services_interface.category;

import coffee.api.dto.request.category.EditCategoriesRequest;

import java.util.UUID;

public interface IEditCategoriesService {
  void process(
    EditCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserId,
    UUID currentUserShopId
  );
}
