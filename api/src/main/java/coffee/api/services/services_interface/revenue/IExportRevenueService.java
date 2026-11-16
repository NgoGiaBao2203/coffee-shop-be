package coffee.api.services.services_interface.revenue;

import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.result.RevenueExportResult;
import java.util.UUID;

public interface IExportRevenueService {
  RevenueExportResult process(
      SearchRevenueRequest request,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId);
}
