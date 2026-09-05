package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.EditDrinksRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface IEditDrinksService {
  void process(
      EditDrinksRequest request,
      MultipartFile imageFile,
      String currentUserRoleName,
      UUID currentUserShopId);
}
