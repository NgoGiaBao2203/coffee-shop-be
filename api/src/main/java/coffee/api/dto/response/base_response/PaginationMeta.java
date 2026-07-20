package coffee.api.dto.response.base_response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMeta {
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
}
