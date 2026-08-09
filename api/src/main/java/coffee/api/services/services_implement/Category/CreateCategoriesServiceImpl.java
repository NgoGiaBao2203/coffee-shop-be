package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.CreateCategoriesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateCategoriesMapper;
import coffee.api.services.services_interface.category.ICreateCategoriesService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateCategoriesServiceImpl implements ICreateCategoriesService {
  private final CommonMapper commonMapper;
  private final CreateCategoriesMapper createCategoriesMapper;

  @Override
  public void process(
      CreateCategoriesRequest request, String currentUserRoleName, UUID currentUserShopId) {
    if (Roles.MANAGER.getValue().equals(currentUserRoleName)) {
      if (!currentUserShopId.equals(request.getShopId())) {
        throw new InvalidRequestException("Shop Ids are not match profile");
      }
    }

    Boolean isShopExisted = commonMapper.checkShopExisted(request.getShopId());
    if (!isShopExisted) {
      throw new DataNotFoundException("Data not found", request.getShopId());
    }

    createCategoriesMapper.createCategories(request, currentUserRoleName, currentUserShopId);
  }
}
