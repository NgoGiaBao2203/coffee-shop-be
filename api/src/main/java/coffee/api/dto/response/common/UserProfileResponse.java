package coffee.api.dto.response.common;

import coffee.api.dto.result.ProfileResult;
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
public class UserProfileResponse extends BaseApiResponse {
  private ProfileResult userProfile;

  public static UserProfileResponse of(
    ResponseCode responseCode,
    String message
  ) {
    return UserProfileResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .userProfile(null)
      .build();
  }

  public static UserProfileResponse of(
    ResponseCode responseCode,
    String message,
    ProfileResult userProfile
  ) {
    return UserProfileResponse.builder()
      .code(responseCode.getCode())
      .message(message)
      .traceId(MdcUtil.getTraceId())
      .userProfile(userProfile)
      .build();
  }
}
