package coffee.api.services.services_interface.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.revenue.GetRevenueResponse;
import java.util.UUID;

public interface IGetRevenueService {
  GetRevenueResponse process(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
