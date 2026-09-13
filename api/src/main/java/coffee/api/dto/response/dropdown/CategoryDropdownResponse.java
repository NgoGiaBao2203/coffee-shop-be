package coffee.api.dto.response.dropdown;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.result.CategoryDropdownResult;
import coffee.api.enums.ResponseCode;
import coffee.api.utils.MdcUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"code", "message", "errorDetails", "traceId"})
public class CategoryDropdownResponse extends BaseApiResponse {
  private List<CategoryDropdownResult> categoryResult;

  public static CategoryDropdownResponse of(
      ResponseCode responseCode, String message, List<CategoryDropdownResult> categoryResult) {
    return CategoryDropdownResponse.builder()
        .code(responseCode.getCode())
        .message(message)
        .traceId(MdcUtil.getTraceId())
        .categoryResult(categoryResult)
        .build();
  }
}
