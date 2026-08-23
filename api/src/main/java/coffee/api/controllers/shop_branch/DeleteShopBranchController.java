package coffee.api.controllers.shop_branch;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;
import coffee.api.dto.response.shop_branch.DeleteShopBranchResponse;
import coffee.api.enums.ResponseCode;
import coffee.api.services.services_interface.shop_branch.IDeleteShopBranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeleteShopBranchController {

  private final IDeleteShopBranchService deleteShopBranchService;

  @DeleteMapping("shop-branch/delete")
  @PreAuthorize("hasAnyRole('OWNER')")
  public ResponseEntity<DeleteShopBranchResponse> deleteShopBranch(
      @RequestBody @Valid DeleteShopBranchRequest request) {

    deleteShopBranchService.process(request);

    return ResponseEntity.ok()
        .body(
            DeleteShopBranchResponse.of(ResponseCode.SUCCESS, "Shop branch deleted successfully"));
  }
}
