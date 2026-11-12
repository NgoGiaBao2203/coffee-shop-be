package coffee.api.dto.response.revenue;

import coffee.api.dto.response.base_response.BaseApiResponse;
import coffee.api.dto.response.base_response.PaginationMeta;
import coffee.api.dto.result.RevenueResult;
import coffee.api.enums.ResponseCode;
import coffee.api.utils.MdcUtil;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"code", "message", "traceId", "totalRevenue", "items", "pagination"})
public class GetRevenueResponse extends BaseApiResponse {
  private BigDecimal totalRevenue;
  private List<RevenueResult> items;
  private PaginationMeta pagination;

  public static GetRevenueResponse of(
      ResponseCode responseCode,
      String message,
      BigDecimal totalRevenue,
      List<RevenueResult> items,
      PaginationMeta pagination) {
    return GetRevenueResponse.builder()
        .code(responseCode.getCode())
        .message(message)
        .traceId(MdcUtil.getTraceId())
        .totalRevenue(totalRevenue)
        .items(items)
        .pagination(pagination)
        .build();
  }
}
