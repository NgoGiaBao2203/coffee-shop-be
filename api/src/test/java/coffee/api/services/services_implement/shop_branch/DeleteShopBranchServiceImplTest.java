package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopBranchMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class DeleteShopBranchServiceImplTest {

  @Mock private DeleteShopBranchMapper deleteShopBranchMapper;

  @InjectMocks private DeleteShopBranchServiceImpl deleteShopBranchService;

  private DeleteShopBranchRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new DeleteShopBranchRequest();
    validRequest.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_TC001() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopBranchService.process(validRequest));

    // Assert
    verify(deleteShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopBranchMapper, times(1)).countActiveInvoicesByShopId(validRequest.getShopId());
    verify(deleteShopBranchMapper, times(1)).softDeleteCategoriesByShopId(validRequest.getShopId());
    verify(deleteShopBranchMapper, times(1)).softDeleteDrinksByShopId(validRequest.getShopId());
    verify(deleteShopBranchMapper, times(1)).softDeleteShopBranch(validRequest.getShopId());
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC002() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(deleteShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopBranchMapper, never()).countActiveInvoicesByShopId(any());
    verify(deleteShopBranchMapper, never()).softDeleteShopBranch(any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPendingInvoicesExist_TC003() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(2);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Can not delete branch pending invoices", exception.getMessage());
    verify(deleteShopBranchMapper, never()).softDeleteCategoriesByShopId(any());
    verify(deleteShopBranchMapper, never()).softDeleteDrinksByShopId(any());
    verify(deleteShopBranchMapper, never()).softDeleteShopBranch(any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC004() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    doThrow(new DataAccessException("Database deletion error") {})
        .when(deleteShopBranchMapper)
        .softDeleteShopBranch(validRequest.getShopId());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Database deletion error", exception.getMessage());
    verify(deleteShopBranchMapper, times(1)).softDeleteShopBranch(validRequest.getShopId());
  }
}
