package coffee.api.controllers.staff;

import coffee.api.constants.Authority;
import coffee.api.dto.request.staff.SearchStaffsRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ProfileResult;
import coffee.api.security.CustomUserDetail;
import coffee.api.services.services_interface.staff.IGetStaffsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GetStaffsController {
  private final IGetStaffsService getStaffsService;

  @PostMapping("staffs")
  @PreAuthorize(Authority.OWNER_OR_MANAGER)
  public PageResponse<ProfileResult> getStaffs(
      @AuthenticationPrincipal CustomUserDetail customUserDetail,
      @RequestBody(required = false) @Valid SearchStaffsRequest request) {
    if (request == null) {
      request = new SearchStaffsRequest();
    }
    return getStaffsService.process(
        request, customUserDetail.getRoleName(), customUserDetail.getUserId());
  }
}
