package coffee.api.dto.request.invoice;

import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import lombok.Data;

@Data
public class SearchInvoiceRequest {

  private UUID shopId;

  private Integer status;

  @Min(value = 1, message = ValidationMessage.Msg.PAGE_MIN)
  private int page = 1;

  @Min(value = 1, message = ValidationMessage.Msg.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.Msg.SIZE_MAX)
  private int size = 10;

  private String search;

  @Pattern(
      regexp =
          "^(invoiceId|totalAmount|status|tableNumber|createdAt|updateAt|isDeleted|shopName|staffName)$",
      message = ValidationMessage.Msg.SORT_BY_INVALID)
  private String sortBy = "createdAt";

  private SortDirection sortDirection = SortDirection.DESC;

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
