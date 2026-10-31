package coffee.api.dto.request.invoice;

import coffee.api.enums.SortDirection;
import coffee.api.enums.ValidationMessage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class SearchInvoiceRequest {

  private UUID shopId;

  @Min(value = 0, message = ValidationMessage.Msg.STATUS_INVALID)
  @Max(value = 2, message = ValidationMessage.Msg.STATUS_INVALID)
  private Integer status;

  @Min(value = 1, message = ValidationMessage.Msg.PAGE_MIN)
  private int page = 1;

  @Min(value = 1, message = ValidationMessage.Msg.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.Msg.SIZE_MAX)
  private int size = 10;

  // Allows Unicode letters, numbers, spaces and punctuation used in invoice searches.
  @Size(max = 100, message = ValidationMessage.Msg.SIZE_MAX)
  @Pattern(
      regexp = "^\\s*$|^[\\p{L}\\p{N}\\s&/(),.'\\-]+$",
      message = ValidationMessage.Msg.SPECIAL_CHARACTERS)
  private String search;

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  @Pattern(
      regexp =
          "^(invoiceId|totalAmount|status|tableNumber|createdAt|updateAt|isDeleted|shopName|staffName)$",
      message = ValidationMessage.Msg.SORT_BY_INVALID)
  private String sortBy = "createdAt";

  @NotNull(message = ValidationMessage.Msg.FIELD_REQUIRED)
  private SortDirection sortDirection = SortDirection.DESC;

  public long calcOffset() {
    return (long) (page - 1) * size;
  }

  public int totalPages(long totalElements) {
    return (int) Math.ceil((double) totalElements / size);
  }

  public String trimmedSearch() {
    if (search == null || search.isBlank()) return null;
    return search.trim();
  }
}
