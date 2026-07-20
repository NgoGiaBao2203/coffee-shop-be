package coffee.api.dto.response.base_response;

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
public class ErrorApiResponse extends BaseApiResponse {
  private List<ErrorDetail> errorDetails;

  public static ErrorApiResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return ErrorApiResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .errorDetails(null)
      .traceId(MdcUtil.getTraceId())
      .build();
  }

  public static ErrorApiResponse of(
    ResponseCode responseCode,
    String message,
    List<ErrorDetail> errorDetails
  ) {
    return ErrorApiResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .errorDetails(errorDetails)
      .traceId(MdcUtil.getTraceId())
      .build();
  }
}
