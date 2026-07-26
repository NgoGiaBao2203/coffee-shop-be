package coffee.api.dto.response.category;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.CategoryResult;
import coffee.api.enums.ResponseCode;
import coffee.api.utils.MdcUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"code", "message", "errorDetails", "traceId"})
public class GetCategoryResponse extends BaseApiResponse {
  private PageResponse<CategoryResult> categories;


  public static GetCategoryResponse of(
    ResponseCode responseCode,
    String message,
    PageResponse<CategoryResult> categories
  ) {
    return GetCategoryResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .categories(categories)
      .build();
  }
}
