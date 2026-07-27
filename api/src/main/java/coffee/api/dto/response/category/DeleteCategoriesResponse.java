package coffee.api.dto.response.category;

import coffee.api.dto.response.base_response.BaseApiResponse;
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
@JsonPropertyOrder({ "code", "message", "errorDetails", "traceId" })
public class DeleteCategoriesResponse extends BaseApiResponse {
  public static DeleteCategoriesResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return DeleteCategoriesResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .build();
  }
}
