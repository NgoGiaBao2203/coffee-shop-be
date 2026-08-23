package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.CreateShopBranchRequest;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateShopBranchMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class CreateShopBranchServiceImplTest {

  @Mock private CreateShopBranchMapper createShopBranchMapper;

  @InjectMocks private CreateShopBranchServiceImpl createShopBranchService;

  private CreateShopBranchRequest validRequest;

  @BeforeEach
  void setUp() {
    validRequest = new CreateShopBranchRequest();
    validRequest.setShopName("Coffee Shop - Chi nhánh 3");
    validRequest.setAddress("789 Đường 30/4, Quận Ninh Kiều, Cần Thơ");
    validRequest.setPhoneNumber("02923999111");
    validRequest.setIsDeleted(false);
  }

  @Test
  void process_Success_AsOwner_TC001() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByPhone(validRequest.getPhoneNumber()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, times(1)).checkShopExistedByPhone(validRequest.getPhoneNumber());
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_Success_WhenIsDeletedIsNull_TC002() {
    // Arrange
    validRequest.setIsDeleted(null);

    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByPhone(validRequest.getPhoneNumber()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(false));
  }

  @Test
  void process_Success_WhenPhoneNumberIsNull_TC003() {
    // Arrange
    validRequest.setPhoneNumber(null);

    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, never()).checkShopExistedByPhone(anyString());
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            isNull(),
            eq(false));
  }

  @Test
  void process_ThrowsUserExistException_WhenShopNameExists_TC004() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Shop name is existed", exception.getMessage());
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, never()).checkShopExistedByAddress(anyString());
    verify(createShopBranchMapper, never()).checkShopExistedByPhone(anyString());
    verify(createShopBranchMapper, never()).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenAddressExists_TC005() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Address is existed", exception.getMessage());
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, never()).checkShopExistedByPhone(anyString());
    verify(createShopBranchMapper, never()).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenPhoneNumberExists_TC006() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByPhone(validRequest.getPhoneNumber()))
        .thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(UserExistException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Phone number is existed", exception.getMessage());
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, times(1)).checkShopExistedByPhone(validRequest.getPhoneNumber());
    verify(createShopBranchMapper, never()).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC007() {
    // Arrange
    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByPhone(validRequest.getPhoneNumber()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createShopBranchMapper)
        .createShopBranch(any(), any(), any(), any(), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> createShopBranchService.process(validRequest));

    assertEquals("Database insertion error", exception.getMessage());
    verify(createShopBranchMapper, times(1)).createShopBranch(any(), any(), any(), any(), any());
  }

  @Test
  void process_Success_WhenPhoneNumberIsBlank_TC008() {
    // Arrange
    validRequest.setPhoneNumber("   ");

    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1)).checkShopExistedByName(validRequest.getShopName());
    verify(createShopBranchMapper, times(1)).checkShopExistedByAddress(validRequest.getAddress());
    verify(createShopBranchMapper, never()).checkShopExistedByPhone(anyString());
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq("   "),
            eq(false));
  }

  @Test
  void process_Success_WhenIsDeletedIsTrue_TC009() {
    // Arrange
    validRequest.setIsDeleted(true);

    when(createShopBranchMapper.checkShopExistedByName(validRequest.getShopName()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByAddress(validRequest.getAddress()))
        .thenReturn(false);
    when(createShopBranchMapper.checkShopExistedByPhone(validRequest.getPhoneNumber()))
        .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> createShopBranchService.process(validRequest));

    // Assert
    verify(createShopBranchMapper, times(1))
        .createShopBranch(
            any(UUID.class),
            eq(validRequest.getShopName()),
            eq(validRequest.getAddress()),
            eq(validRequest.getPhoneNumber()),
            eq(true));
  }
}
