package coffee.api.repository.staff;

import coffee.api.dto.result.ProfileResult;
import coffee.api.mapper.GetStaffsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GetStaffsRepository {
  private final GetStaffsMapper getStaffsMapper;

  public List<ProfileResult> getStaffsFiltered(
    String search,
    String sortBy,
    String sortDirection,
    int size,
    int offset,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getStaffsMapper.getStaffsFiltered(
      search,
      sortBy,
      sortDirection,
      size,
      offset,
      currentUserRoleName,
      currentUserId
    );
  }

  public long countStaffsFiltered(
    String search,
    String currentUserRoleName,
    UUID currentUserId
  ) {
    return getStaffsMapper.countStaffsFiltered(
      search,
      currentUserRoleName,
      currentUserId
    );
  }
}
