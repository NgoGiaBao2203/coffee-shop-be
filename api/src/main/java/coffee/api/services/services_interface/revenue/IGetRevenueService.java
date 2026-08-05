package coffee.api.services.services_interface.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.RevenueResult;

import java.util.UUID;

public interface IGetRevenueService {
  PageResponse<RevenueResult> process(
    SearchRevenueRequest request,
    String currentUserRoleName,
    UUID currentUserShopId
  );
}
