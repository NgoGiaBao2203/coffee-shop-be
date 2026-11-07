package coffee.api.services.services_implement.revenue.common;

import coffee.api.constants.ErrorMessage;
import coffee.api.enums.Roles;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.services.services_implement.invoice.common.InvoiceValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Shop permission and date range rules shared by the revenue list and revenue export. */
@Component
@RequiredArgsConstructor
public class RevenueFilterResolver {

  private final InvoiceValidator invoiceValidator;

  /**
   * Allow OWNER to view any shop (or all shops when no shop is requested) and restrict MANAGER to
   * the assigned shop. Other roles are denied. Returns the shop to filter by, or null for all.
   */
  public UUID resolveTargetShopId(
      UUID requestedShopId,
      String currentUserRoleName,
      UUID currentUserShopId,
      UUID currentUserId) {
    UUID targetShopId = requestedShopId;
    if (!Roles.OWNER.getValue().equals(currentUserRoleName)) {
      if (!Roles.MANAGER.getValue().equals(currentUserRoleName)) {
        throw new InvalidRequestException("You do not have permission to access revenue");
      }
      if (currentUserShopId == null) {
        throw new InvalidRequestException(ErrorMessage.USER_NOT_ASSIGNED_TO_SHOP);
      }
      targetShopId = currentUserShopId;
    }
    if (targetShopId != null) {
      invoiceValidator.validateShopPermission(targetShopId, currentUserRoleName, currentUserId);
    }
    return targetShopId;
  }

  /** Verify the start date is not after the end date. */
  public void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new InvalidRequestException("startDate must not be after endDate");
    }
  }

  /** Start of the start date, or null when no start date is requested. */
  public LocalDateTime toStartDateTime(LocalDate startDate) {
    return startDate != null ? startDate.atStartOfDay() : null;
  }

  /** End of the end date, or null when no end date is requested. */
  public LocalDateTime toEndDateTime(LocalDate endDate) {
    return endDate != null ? endDate.atTime(LocalTime.MAX) : null;
  }
}
