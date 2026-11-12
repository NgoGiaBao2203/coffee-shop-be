package coffee.api.dto.request.revenue;

import coffee.api.constants.ValidationMessage;
import coffee.api.constants.ValidationPattern;
import coffee.api.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class SearchRevenueRequest {

  @Min(value = 1, message = ValidationMessage.PAGE_MIN)
  private int page = 1;

  @Min(value = 1, message = ValidationMessage.SIZE_MIN)
  @Max(value = 100, message = ValidationMessage.SIZE_MAX)
  private int size = 10;

  @Size(max = 100, message = ValidationMessage.SIZE_MAX)
  @Pattern(regexp = ValidationPattern.OPTIONAL_TEXT, message = ValidationMessage.SPECIAL_CHARACTERS)
  private String search;

  private UUID shopId;
  private UUID invoiceId;

  private LocalDate startDate;
  private LocalDate endDate;

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  @Pattern(
      regexp = "^(invoiceId|shopName|staffName|createdAt|totalAmount|tableNumber)$",
      message = ValidationMessage.SORT_BY_INVALID)
  private String sortBy = "createdAt";

  @NotNull(message = ValidationMessage.FIELD_REQUIRED)
  private SortDirection sortDirection = SortDirection.DESC;

  public void setStartDate(Object value) {
    if (value instanceof String str) {
      this.startDate = str.isBlank() ? null : LocalDate.parse(str);
    } else if (value instanceof LocalDate date) {
      this.startDate = date;
    } else {
      this.startDate = null;
    }
  }

  public void setEndDate(Object value) {
    if (value instanceof String str) {
      this.endDate = str.isBlank() ? null : LocalDate.parse(str);
    } else if (value instanceof LocalDate date) {
      this.endDate = date;
    } else {
      this.endDate = null;
    }
  }

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

  public String directionValue() {
    return sortDirection.name();
  }
}
