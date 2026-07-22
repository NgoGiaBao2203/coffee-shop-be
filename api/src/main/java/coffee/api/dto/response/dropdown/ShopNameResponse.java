package coffee.api.dto.response.dropdown;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.result.ShopNameResult;
import coffee.api.enums.ResponseCode;
import coffee.api.utils.MdcUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({ "code", "message", "errorDetails", "traceId" })
public class ShopNameResponse extends BaseApiResponse {
  private List<ShopNameResult> shopNameResults;

  public static ShopNameResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return ShopNameResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .shopNameResults(null)
      .build();
  }

  public static ShopNameResponse of(
    ResponseCode responseCode,
    String message,
    List<ShopNameResult> shopNameResults
  ) {
    return ShopNameResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .shopNameResults(shopNameResults)
      .build();
  }
}
