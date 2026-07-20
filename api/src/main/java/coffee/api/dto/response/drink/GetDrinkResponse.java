package coffee.api.dto.response.drink;

import coffee.api.dto.result.DrinkResult;
import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.response.base_response.PageResponse;
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
public class GetDrinkResponse extends BaseApiResponse {

  private PageResponse<DrinkResult> drinks;

  public static GetDrinkResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return GetDrinkResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .drinks(null)
      .build();
  }

  public static GetDrinkResponse of(
    ResponseCode responseCode,
    String message,
    PageResponse<DrinkResult> drinks
  ) {
    return GetDrinkResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .drinks(drinks)
      .build();
  }
}