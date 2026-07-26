package coffee.api.repository.category;

import coffee.api.dto.result.CategoryResult;
import coffee.api.mapper.GetCategoriesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GetCategoryRepository {

  private final GetCategoriesMapper getCategoriesMapper;

  public List<CategoryResult> getCategoriesFiltered(
    String search,
    String sortBy,
    String sortDirection,
    int size,
    int offset,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getCategoriesMapper.getCategoriesFiltered(
      search,
      sortBy,
      sortDirection,
      size,
      offset,
      currentUserRoleName,
      currentUserId
    );
  }

  public long countCategoriesFiltered(
    String search,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getCategoriesMapper.countCategoriesFiltered(
      search,
      currentUserRoleName,
      currentUserId
    );
  }
}