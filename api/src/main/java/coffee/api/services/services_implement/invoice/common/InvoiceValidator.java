package coffee.api.services.services_implement.invoice.common;

import coffee.api.dto.request.invoice.InvoiceItemRequest;
import coffee.api.dto.result.DrinkDetailPriceResult;
import coffee.api.dto.result.InvoiceResult;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateInvoiceMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceValidator {
  private static final BigDecimal MAX_ALLOWED_TOTAL_AMOUNT = new BigDecimal("9999999999999999.99");
  private final CreateInvoiceMapper createInvoiceMapper;
  private final CommonMapper commonMapper;

  /** Lock invoice and verify shop permission, STAFF ownership and unpaid status. */
  public InvoiceResult validateInvoiceForStatusChange(
      UUID invoiceId, String roleName, UUID currentUserShopId, UUID currentUserId) {
    InvoiceResult invoice = commonMapper.lockInvoice(invoiceId);
    if (invoice == null) {
      throw new DataNotFoundException("Invoice not found", invoiceId);
    }
    UUID shopId = resolveTargetShopId(invoice.getShopId(), roleName, currentUserShopId);
    validateShopPermission(shopId, roleName, currentUserId);
    if (Roles.STAFF.name().equalsIgnoreCase(roleName)
        && !currentUserId.equals(invoice.getProfileId())) {
      throw new InvalidRequestException("You do not have permission to change this invoice");
    }
    if (!"0".equals(invoice.getStatus())) {
      throw new InvalidRequestException(
          "Cannot change an invoice that is already completed or cancelled");
    }
    validateOccupiedInvoiceTable(invoice.getTableId(), shopId);
    return invoice;
  }

  /** Verify and lock the occupied table before updating its invoice. */
  public void validateOccupiedInvoiceTable(UUID tableId, UUID shopId) {
    if (tableId == null || commonMapper.lockOccupiedInvoiceTable(tableId, shopId) == null) {
      throw new InvalidRequestException(
          "Current table is unavailable or does not belong to this shop");
    }
  }

  /** Keep the table occupied while other unpaid invoices use it; otherwise release it. */
  public void releaseInvoiceTable(UUID tableId, UUID shopId) {
    if (commonMapper.hasUnpaidTableInvoices(tableId)) {
      return;
    }
    if (commonMapper.releaseInvoiceTable(tableId, shopId) != 1) {
      throw new InvalidRequestException("Table cannot be released. Please reload and try again");
    }
  }

  /** Verify submitted prices match current sellable drink prices and calculate invoice total. */
  public BigDecimal validateItemPricesAndCalculateTotal(
      List<InvoiceItemRequest> items, UUID targetShopId) {

    Set<UUID> drinkDetailIds =
        items.stream().map(InvoiceItemRequest::getDrinkDetailId).collect(Collectors.toSet());

    Map<UUID, BigDecimal> priceMap =
        createInvoiceMapper.getSellableDrinkPrices(drinkDetailIds, targetShopId).stream()
            .collect(
                Collectors.toMap(
                    DrinkDetailPriceResult::getDrinkDetailId, DrinkDetailPriceResult::getPrice));

    BigDecimal totalAmount = BigDecimal.ZERO;

    for (InvoiceItemRequest item : items) {
      BigDecimal unitPrice = priceMap.get(item.getDrinkDetailId());

      if (unitPrice == null || unitPrice.signum() < 0) {
        throw new InvalidRequestException("Drink is unavailable or does not belong to this shop");
      }

      if (unitPrice.compareTo(item.getUnitPrice()) != 0) {
        throw new InvalidRequestException("Drink price has changed. Please reload and try again");
      }

      BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
      totalAmount = totalAmount.add(itemTotal);
    }

    if (totalAmount.compareTo(MAX_ALLOWED_TOTAL_AMOUNT) > 0) {
      throw new InvalidRequestException("Invoice total exceeds allowed amount");
    }

    return totalAmount;
  }

  /**
   * Verify the required shop ID and user role before creating or editing an invoice. MANAGER and
   * STAFF: Verify the assigned Id and prevent access to other shops. Deny access for unauthorized
   * roles.
   */
  public UUID resolveTargetShopId(
      UUID requestedShopId, String currentUserRoleName, UUID currentUserShopId) {
    if (requestedShopId == null) {
      throw new InvalidRequestException("Shop ID is required");
    }
    if (Roles.OWNER.name().equalsIgnoreCase(currentUserRoleName)) {
      return requestedShopId;
    }
    validateManagerOrStaffRole(currentUserRoleName);
    if (currentUserShopId == null) {
      throw new InvalidRequestException("User is not assigned to any shop");
    }
    if (!currentUserShopId.equals(requestedShopId)) {
      throw new InvalidRequestException("You do not have permission to access this shop");
    }
    return currentUserShopId;
  }

  /** Verify the shop is not deleted and MANAGER or STAFF has active membership. */
  public void validateShopPermission(UUID shopId, String roleName, UUID currentUserId) {
    if (!Boolean.TRUE.equals(commonMapper.checkShopExisted(shopId))) {
      throw new DataNotFoundException("Data not found", shopId);
    }
    if (currentUserId == null) {
      throw new InvalidRequestException("You do not have permission to access this shop");
    }
    if (isOwner(roleName)) {
      return;
    }
    if (!Boolean.TRUE.equals(commonMapper.checkShopIdIsExisted(currentUserId, shopId))) {
      throw new InvalidRequestException("You do not have permission to access this shop");
    }
  }

  /** Verify non-owner users have MANAGER or STAFF role to create invoices. */
  private void validateManagerOrStaffRole(String currentUserRoleName) {
    if (!Roles.MANAGER.name().equalsIgnoreCase(currentUserRoleName)
        && !Roles.STAFF.name().equalsIgnoreCase(currentUserRoleName)) {
      throw new InvalidRequestException("You do not have permission to create invoices");
    }
  }

  /** Check if the role is OWNER */
  private boolean isOwner(String roleName) {
    return Roles.OWNER.name().equalsIgnoreCase(roleName);
  }

  public void validateAvailableTable(UUID tableId, UUID shopId) {
    if (!createInvoiceMapper.checkTableAvailableInShop(tableId, shopId)) {
      throw new InvalidRequestException("Table is unavailable or does not exist in this shop");
    }
  }

  public void occupyAvailableTable(UUID tableId, UUID shopId) {
    if (createInvoiceMapper.occupyTable(tableId, shopId) != 1) {
      throw new InvalidRequestException("Table is unavailable or does not exist in this shop");
    }
  }
}
