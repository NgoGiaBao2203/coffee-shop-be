package coffee.api.services.services_implement.staff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.user.CreateStaffRequest;
import coffee.api.exceptions.EmailExistedException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateStaffMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateStaffServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateStaffMapper createStaffMapper;

  @InjectMocks private CreateStaffServiceImpl createStaffService;

  private CreateStaffRequest validRequest;
  private UUID currentUserId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    validRequest = new CreateStaffRequest();
    validRequest.setUsername("newstaff");
    validRequest.setPhoneNumber("0901234567");
    validRequest.setEmail("newstaff@coffee.com");
    validRequest.setFullName("Nguyen Van B");
    validRequest.setRoleId(UUID.randomUUID());
    validRequest.setShopId(UUID.randomUUID());

    currentUserId = UUID.randomUUID();
    currentUserRoleName = "MANAGER";
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkStaffExistedByUsername(validRequest.getUsername())).thenReturn(false);
    when(commonMapper.checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber()))
        .thenReturn(false);
    when(commonMapper.checkStaffEmailExisted(validRequest.getEmail())).thenReturn(false);

    // Act
    assertDoesNotThrow(
        () -> createStaffService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(commonMapper, times(1)).checkStaffExistedByUsername(validRequest.getUsername());
    verify(commonMapper, times(1)).checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber());
    verify(commonMapper, times(1)).checkStaffEmailExisted(validRequest.getEmail());

    // Verifies mapper unrolled signature explicitly matches service call
    verify(createStaffMapper, times(1))
        .createStaff(
            validRequest.getEmail(),
            validRequest.getUsername(),
            validRequest.getFullName(),
            validRequest.getPhoneNumber(),
            validRequest.getRoleId(),
            validRequest.getShopId(),
            currentUserId,
            currentUserRoleName);
  }

  @Test
  void process_ThrowsUserExistException_WhenUsernameExists_TC002() {
    // Arrange
    when(commonMapper.checkStaffExistedByUsername(validRequest.getUsername())).thenReturn(true);

    // Act & Assert
    UserExistException exception =
        assertThrows(
            UserExistException.class,
            () -> createStaffService.process(validRequest, currentUserId, currentUserRoleName));

    assertEquals("Staff is existed", exception.getMessage());

    verify(commonMapper, times(1)).checkStaffExistedByUsername(validRequest.getUsername());
    verify(commonMapper, never()).checkStaffPhoneExistedForCreate(anyString());
    verify(commonMapper, never()).checkStaffEmailExisted(anyString());
    verify(createStaffMapper, never())
        .createStaff(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsPhoneNumberExistedException_WhenPhoneExists_TC003() {
    // Arrange
    when(commonMapper.checkStaffExistedByUsername(validRequest.getUsername())).thenReturn(false);
    when(commonMapper.checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber()))
        .thenReturn(true);

    // Act & Assert
    PhoneNumberExistedException exception =
        assertThrows(
            PhoneNumberExistedException.class,
            () -> createStaffService.process(validRequest, currentUserId, currentUserRoleName));

    assertEquals("Staff phone is existed", exception.getMessage());
    // Fixed method reference targeting according to target exception getters (.getPhoneNumber() vs
    // .getDynamicData())
    assertEquals(validRequest.getPhoneNumber(), exception.getPhoneNumber());

    verify(commonMapper, times(1)).checkStaffExistedByUsername(validRequest.getUsername());
    verify(commonMapper, times(1)).checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber());
    verify(commonMapper, never()).checkStaffEmailExisted(anyString());
    verify(createStaffMapper, never())
        .createStaff(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsEmailExistedException_WhenEmailExists_TC004() {
    // Arrange
    when(commonMapper.checkStaffExistedByUsername(validRequest.getUsername())).thenReturn(false);
    when(commonMapper.checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber()))
        .thenReturn(false);
    when(commonMapper.checkStaffEmailExisted(validRequest.getEmail())).thenReturn(true);

    // Act & Assert
    EmailExistedException exception =
        assertThrows(
            EmailExistedException.class,
            () -> createStaffService.process(validRequest, currentUserId, currentUserRoleName));

    assertEquals("Staff email is existed", exception.getMessage());
    assertEquals(validRequest.getEmail(), exception.getEmail());

    verify(commonMapper, times(1)).checkStaffExistedByUsername(validRequest.getUsername());
    verify(commonMapper, times(1)).checkStaffPhoneExistedForCreate(validRequest.getPhoneNumber());
    verify(commonMapper, times(1)).checkStaffEmailExisted(validRequest.getEmail());
    verify(createStaffMapper, never())
        .createStaff(any(), any(), any(), any(), any(), any(), any(), any());
  }
}
