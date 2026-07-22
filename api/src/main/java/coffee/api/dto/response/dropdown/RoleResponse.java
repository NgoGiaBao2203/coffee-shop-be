package coffee.api.dto.response.dropdown;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.result.RoleResult;
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
public class RoleResponse extends BaseApiResponse {
  private List<RoleResult> roleResult;

  public static RoleResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return RoleResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .roleResult(null)
      .build();
  }

  // Update this to accept List<RoleResult> instead of a single element
  public static RoleResponse of(
    ResponseCode responseCode,
    String message,
    List<RoleResult> roleResult
  ) {
    return RoleResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .roleResult(roleResult)
      .build();
  }
}