package coffee.api.controllers.revenue;

import coffee.api.constants.Authority;
import coffee.api.dto.request.revenue.SearchRevenueRequest;
import coffee.api.dto.response.revenue.GetRevenueResponse;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.revenue.IGetRevenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetRevenueController {

  private final IGetRevenueService getRevenueService;

  @PostMapping("revenues")
  @PreAuthorize(Authority.OWNER_OR_MANAGER)
  public GetRevenueResponse getRevenues(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody @Valid SearchRevenueRequest request) {
    return getRevenueService.process(
        request,
        customUserDetail.getRoleName(),
        customUserDetail.getShopId(),
        customUserDetail.getUserId());
  }
}
