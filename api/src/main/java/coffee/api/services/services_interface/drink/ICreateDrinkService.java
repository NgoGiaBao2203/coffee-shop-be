package coffee.api.services.services_interface.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ICreateDrinkService {
  void process(
      CreateDrinksRequest request,
      MultipartFile imageFile,
      UUID currentUserId,
      UUID currentShopID,
      String roleName);
}
