package coffee.api.dto.request.shop_branch;

import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchShopBranchRequest {

  @Min(value = 1, message = ValidationMessage.Msg.PAGE_MIN)
  private int page = 1;

  @Min(value = 1, message = ValidationMessage.Msg.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.Msg.SIZE_MAX)
  private int size = 10;

  @Size(max = 100, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(
      regexp = "^\\s*$|^[\\p{L}0-9\\s.,/&'#-]+$",
      message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String search;

  @Pattern(
      regexp = "^(shopId|shopName|address|phoneNumber|createdAt|updatedAt|isDeleted)$",
      message = ValidationMessage.Msg.SORT_BY_INVALID)
  private String sortBy = "shopId";

  private SortDirection sortDirection = SortDirection.ASC;

  public int calcOffset() {
    return (page - 1) * size;
  }

  public int totalPages(long totalElements) {
    return (int) Math.ceil((double) totalElements / size);
  }

  public String trimmedSearch() {
    if (search == null || search.isBlank()) return null;
    return search.trim();
  }
}
