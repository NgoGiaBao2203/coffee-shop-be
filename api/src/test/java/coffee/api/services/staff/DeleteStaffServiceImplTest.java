package coffee.api.services.staff;

import coffee.api.dto.request.user.DeleteStaffRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteStaffMapper;
import coffee.api.services.services_implement.staff.DeleteStaffServiceImpl;
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
public class DeleteStaffServiceImplTest {

  @Mock
  private CommonMapper commonMapper;

  @Mock
  private DeleteStaffMapper deleteStaffMapper;

  @InjectMocks
  private DeleteStaffServiceImpl deleteStaffService;

  private DeleteStaffRequest validRequest;
  private UUID targetProfileId;

  @BeforeEach
  void setUp() {
    targetProfileId = UUID.randomUUID();

    validRequest = new DeleteStaffRequest();
    validRequest.setProfileId(targetProfileId);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkStaffExisted(validRequest.getProfileId())).thenReturn(true);

    // Act
    assertDoesNotThrow(() -> deleteStaffService.process(validRequest));

    // Assert
    verify(commonMapper, times(1)).checkStaffExisted(validRequest.getProfileId());
    verify(deleteStaffMapper, times(1)).deleteStaff(validRequest.getProfileId());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenStaffDoesNotExist_TC002() {
    // Arrange
    when(commonMapper.checkStaffExisted(validRequest.getProfileId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception = assertThrows(DataNotFoundException.class, () ->
      deleteStaffService.process(validRequest)
    );

    assertEquals("Data not found", exception.getMessage());
    assertEquals(targetProfileId, exception.getId()); // Using getDynamicData() to match custom payload tracker from earlier cases

    // Verify it fails fast and never hits the database modification mapper layer
    verify(commonMapper, times(1)).checkStaffExisted(validRequest.getProfileId());
    verify(deleteStaffMapper, never()).deleteStaff(any());
  }
}