package coffee.api.dto.response.base_response;

import coffee.api.enums.ResponseCode;
import coffee.api.utils.MdcUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
  private String code;
  private String message;
  private String traceId;
  private List<T> items;
  private PaginationMeta pagination;

  public static <T> PageResponse<T> of(
    String message, List<T> items,
    PaginationMeta pagination
  ) {
    return PageResponse.<T>builder()
      .code(ResponseCode.SUCCESS.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .items(items)
      .pagination(pagination)
      .build();
  }
}
