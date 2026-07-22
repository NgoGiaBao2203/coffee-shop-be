package coffee.api.services.staff;

import coffee.api.dto.request.user.EditStaffRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.PhoneNumberExistedException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.UpdateStaffMapper;
import coffee.api.services.services_implement.staff.EditStaffServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EditStaffServiceImplTest {

  @Mock
  private CommonMapper commonMapper;

  @Mock
  private UpdateStaffMapper updateStaffMapper;

  @InjectMocks
  private EditStaffServiceImpl editStaffService;

  private EditStaffRequest validRequest;
  private String currentUserRoleName;
  private UUID targetProfileId;

  @BeforeEach
  void setUp() {
    targetProfileId = UUID.randomUUID();
    currentUserRoleName = "OWNER";

    validRequest = new EditStaffRequest();
    validRequest.setProfileId(targetProfileId);
    validRequest.setFullName("Nguyen Van Updated");
    validRequest.setPhoneNumber("0909999999");
    validRequest.setRoleId(UUID.randomUUID());
    validRequest.setShopId(UUID.randomUUID());
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkStaffExisted(validRequest.getProfileId())).thenReturn(true);
    when(commonMapper.checkStaffPhoneExisted(validRequest.getProfileId(), validRequest.getPhoneNumber()))
      .thenReturn(false);

    // Act
    assertDoesNotThrow(() -> editStaffService.process(validRequest, currentUserRoleName));

    // Assert
    verify(commonMapper, times(1)).checkStaffExisted(validRequest.getProfileId());
    verify(commonMapper, times(1))
      .checkStaffPhoneExisted(validRequest.getProfileId(), validRequest.getPhoneNumber());

    // Verifies the mapper layer receives the specific individual unrolled model fields
    verify(updateStaffMapper, times(1)).updateStaff(
      validRequest.getProfileId(),
      validRequest.getFullName(),
      validRequest.getPhoneNumber(),
      validRequest.getRoleId(),
      validRequest.getShopId(),
      currentUserRoleName
    );
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenStaffDoesNotExist_TC002() {
    // Arrange
    when(commonMapper.checkStaffExisted(validRequest.getProfileId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception = assertThrows(DataNotFoundException.class, () ->
      editStaffService.process(validRequest, currentUserRoleName)
    );

    assertEquals("Data not found", exception.getMessage());
    // Fixed method metadata targeting reference to match your data tracking models (.getDynamicData())
    assertEquals(targetProfileId, exception.getId());

    verify(commonMapper, times(1)).checkStaffExisted(validRequest.getProfileId());
    verify(commonMapper, never()).checkStaffPhoneExisted(any(), anyString());
    verify(updateStaffMapper, never()).updateStaff(any(), any(), any(), any(), any(), anyString());
  }

  @Test
  void process_ThrowsPhoneNumberExistedException_WhenPhoneConflictOccurs_TC003() {
    // Arrange
    when(commonMapper.checkStaffExisted(validRequest.getProfileId())).thenReturn(true);
    when(commonMapper.checkStaffPhoneExisted(validRequest.getProfileId(), validRequest.getPhoneNumber()))
      .thenReturn(true);

    // Act & Assert
    PhoneNumberExistedException exception = assertThrows(PhoneNumberExistedException.class, () ->
      editStaffService.process(validRequest, currentUserRoleName)
    );

    assertEquals("Phone number exited", exception.getMessage());
    assertEquals(validRequest.getPhoneNumber(), exception.getPhoneNumber());

    verify(commonMapper, times(1)).checkStaffExisted(validRequest.getProfileId());
    verify(commonMapper, times(1))
      .checkStaffPhoneExisted(validRequest.getProfileId(), validRequest.getPhoneNumber());
    verify(updateStaffMapper, never()).updateStaff(any(), any(), any(), any(), any(), anyString());
  }
}