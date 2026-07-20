package coffee.api.controllers.staff;

import coffee.api.dto.request.user.SearchUsersRequest;
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
  private final IGetStaffsService getStaffsrService;

  @PostMapping("staffs")
  @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
  public PageResponse<ProfileResult> getUserList(
    @AuthenticationPrincipal CustomUserDetail customUserDetail,
    @RequestBody(required = false) @Valid SearchUsersRequest request
  ) {
    if (request == null) {
      request = new SearchUsersRequest();
    }
    return getStaffsrService.process(
      request,
      customUserDetail.getRoleName(),
      customUserDetail.getUserId()
    );
  }
}
