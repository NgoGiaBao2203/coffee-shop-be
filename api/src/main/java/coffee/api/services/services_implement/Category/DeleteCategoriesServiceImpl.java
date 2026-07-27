package coffee.api.services.services_implement.category;

import coffee.api.dto.request.category.DeleteCategoriesRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteCategoryMapper;
import coffee.api.services.services_interface.category.IDeleteCategoriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCategoriesServiceImpl implements IDeleteCategoriesService {
  private final CommonMapper commonMapper;
  private final DeleteCategoryMapper deleteCategoryMapper;

  @Override
  public void process(
    DeleteCategoriesRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  ) {
    Boolean isCategoryExisted = commonMapper.checkCategoryExisted(
      request.getCategoryId(),
      currentUserRoleName,
      currentUserShopId
    );
    if (!isCategoryExisted) {
      throw new DataNotFoundException("Data not found", request.getCategoryId());
    }
    deleteCategoryMapper.deleteCategory(
      request.getCategoryId(),
      currentUserRoleName,
      currentUserShopId
    );
  }
}
