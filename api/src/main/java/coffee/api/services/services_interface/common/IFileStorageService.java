package coffee.api.services.services_interface.common;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
  String storeDrinkImage(MultipartFile file);

  void deleteDrinkImage(String imageUrl);
}
