package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.EditShopBranchRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.UpdateShopBranchMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class EditShopBranchServiceImplTest {

  @Mock private UpdateShopBranchMapper updateShopBranchMapper;

  @InjectMocks private EditShopBranchServiceImpl editShopBranchService;

  private EditShopBranchRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new EditShopBranchRequest();
    validRequest.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    validRequest.setShopName("Coffee Shop - Chi nhánh 1");
    validRequest.setAddress("123 Đường 3/2, Quận Ninh Kiều, Cần Thơ");
    validRequest.setPhoneNumber("02923888999");
    validRequest.setIsDeleted(false);
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_TC001() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editShopBranchService.process(validRequest));

    // Assert
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByNameExceptCurrent(validRequest.getShopId(), validRequest.getShopName());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber());
    verify(updateShopBranchMapper, times(1))
        .updateShopBranch(
            eq(validRequest.getShopId()),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_Success_WhenPhoneNumberIsNull_TC002() {
    // Arrange
    validRequest.setPhoneNumber(null);

    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editShopBranchService.process(validRequest));

    // Assert
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByNameExceptCurrent(validRequest.getShopId(), validRequest.getShopName());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByPhoneExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, times(1))
        .updateShopBranch(
            eq(validRequest.getShopId()),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            isNull(),
            eq(false));
  }

  @Test
  void process_Success_WhenPhoneNumberIsBlank_TC003() {
    // Arrange
    validRequest.setPhoneNumber("   ");

    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editShopBranchService.process(validRequest));

    // Assert
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByPhoneExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, times(1))
        .updateShopBranch(
            eq(validRequest.getShopId()),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            isNull(),
            eq(false));
  }

  @Test
  void process_Success_WhenIsDeletedIsTrue_TC004() {
    // Arrange
    validRequest.setIsDeleted(true);

    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editShopBranchService.process(validRequest));

    // Assert
    verify(updateShopBranchMapper, times(1))
        .updateShopBranch(
            eq(validRequest.getShopId()),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(true));
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC005() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByNameExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, never()).updateShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenShopNameExistsInAnotherBranch_TC006() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Shop name is existed", exception.getMessage());
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByNameExceptCurrent(validRequest.getShopId(), validRequest.getShopName());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByAddressExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByPhoneExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, never()).updateShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenAddressExistsInAnotherBranch_TC007() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Address is existed", exception.getMessage());
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByNameExceptCurrent(validRequest.getShopId(), validRequest.getShopName());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress());
    verify(updateShopBranchMapper, never())
        .checkShopExistedByPhoneExceptCurrent(any(UUID.class), anyString());
    verify(updateShopBranchMapper, never()).updateShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenPhoneExistsInAnotherBranch_TC008() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Phone number is existed", exception.getMessage());
    verify(updateShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByNameExceptCurrent(validRequest.getShopId(), validRequest.getShopName());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress());
    verify(updateShopBranchMapper, times(1))
        .checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber());
    verify(updateShopBranchMapper, never()).updateShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC009() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(updateShopBranchMapper.checkShopExistedByNameExceptCurrent(
            validRequest.getShopId(), validRequest.getShopName()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByAddressExceptCurrent(
            validRequest.getShopId(), validRequest.getAddress()))
        .thenReturn(false);
    when(updateShopBranchMapper.checkShopExistedByPhoneExceptCurrent(
            validRequest.getShopId(), validRequest.getPhoneNumber()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database update error") {})
        .when(updateShopBranchMapper)
        .updateShopBranch(any(), any(), any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(DataAccessException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Database update error", exception.getMessage());
    verify(updateShopBranchMapper, times(1)).updateShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsException_WhenCheckShopExistedByIdFails_TC010() {
    // Arrange
    when(updateShopBranchMapper.checkShopExistedById(any()))
        .thenThrow(new RuntimeException("Database connection timeout"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> editShopBranchService.process(validRequest));

    assertEquals("Database connection timeout", exception.getMessage());
    verify(updateShopBranchMapper, never()).updateShopBranch(any(), any(), any(), any(), any());
  }
}
