package coffee.api.dto.response.detail;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.result.DrinkResult;
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
@JsonPropertyOrder({"code", "message", "traceId", "item"})
public class GetDrinkDetailResponse extends BaseApiResponse {
  private DrinkResult item;

  public static GetDrinkDetailResponse of(
      ResponseCode responseCode, String message, DrinkResult item) {
    return GetDrinkDetailResponse.builder()
        .code(responseCode.getCode())
        .message(message)
        .traceId(MdcUtil.getTraceId())
        .item(item)
        .build();
  }
}
