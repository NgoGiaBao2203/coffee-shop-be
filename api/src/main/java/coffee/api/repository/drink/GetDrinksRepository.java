package coffee.api.repository.drink;

import coffee.api.dto.result.DrinkResult;
import coffee.api.mapper.GetDrinksMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GetDrinksRepository {
  private final GetDrinksMapper getDrinksMapper;


  public List<DrinkResult> getDrinksFiltered(
    String search,
    String sortBy,
    String sortDirection,
    int size,
    int offset,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getDrinksMapper.getDrinksFiltered(
      search,
      sortBy,
      sortDirection,
      size,
      offset,
      currentUserRoleName,
      currentUserId
    );
  }

  public long countDrinksFiltered(
    String search,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getDrinksMapper.countDrinksFiltered(
      search,
      currentUserRoleName,
      currentUserId
    );
  }
}
