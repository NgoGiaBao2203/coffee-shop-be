package coffee.api.dto.request.shop;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import coffee.api.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchShopRequest {

  @Min(value = 1, message = ValidationMessage.PAGE_MIN)
  private int page = 1;

  @Min(value = 1, message = ValidationMessage.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.SIZE_MAX)
  private int size = 10;

  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.SHOP_SEARCH, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String search;

  @Pattern(
      regexp = "^(shopId|shopName|address|phoneNumber|createdAt|updatedAt|isDeleted)$",
      message = ValidationMessage.SORT_BY_INVALID)
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
