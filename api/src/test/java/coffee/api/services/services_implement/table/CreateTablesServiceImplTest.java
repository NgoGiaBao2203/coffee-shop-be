package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.CreateTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.CreateTablesMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private CreateTablesMapper createTablesMapper;

  @InjectMocks private CreateTablesServiceImpl createTablesService;

  private CreateTablesRequest validRequest;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;
  private String staffRole;

  @BeforeEach
  void setUp() {
    currentUserShopId = UUID.randomUUID();

    validRequest = new CreateTablesRequest();
    validRequest.setTableNumber(1);
    validRequest.setDescription("Table near window");
    validRequest.setShopId(currentUserShopId);
    validRequest.setStatus(1);

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();
    staffRole = Roles.STAFF.getValue();
  }

  @Test
  void process_Success_WhenManagerCreatesTableInOwnShop_TC001() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createTablesMapper, times(1))
        .checkTableNumberExisted(validRequest.getShopId(), validRequest.getTableNumber());
    verify(createTablesMapper, times(1))
        .createTable(
            validRequest.getTableNumber(),
            validRequest.getDescription(),
            validRequest.getStatus(),
            validRequest.getShopId());
  }

  @Test
  void process_Success_WhenOwnerCreatesTableInDifferentShop_TC002() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(differentShopId, validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, ownerRole, currentUserShopId));

    verify(commonMapper, times(1)).checkShopExisted(differentShopId);
    verify(createTablesMapper, times(1))
        .checkTableNumberExisted(differentShopId, validRequest.getTableNumber());
    verify(createTablesMapper, times(1))
        .createTable(
            validRequest.getTableNumber(),
            validRequest.getDescription(),
            validRequest.getStatus(),
            differentShopId);
  }

  @Test
  void process_Success_WithoutDescription_TC003() {
    // Arrange
    validRequest.setDescription(null);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, ownerRole, currentUserShopId));

    verify(createTablesMapper, times(1))
        .createTable(
            validRequest.getTableNumber(),
            null,
            validRequest.getStatus(),
            validRequest.getShopId());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenUserIsStaff_TC004() {
    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, staffRole, currentUserShopId));

    assertEquals("Only OWNER and MANAGER can create tables", exception.getMessage());

    verify(commonMapper, never()).checkShopExisted(any());
    verify(createTablesMapper, never()).checkTableNumberExisted(any(), anyInt());
    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenManagerShopIdMismatches_TC005() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Shop Ids are not match profile", exception.getMessage());

    verify(commonMapper, never()).checkShopExisted(any());
    verify(createTablesMapper, never()).checkTableNumberExisted(any(), anyInt());
    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC006() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Data not found", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createTablesMapper, never()).checkTableNumberExisted(any(), anyInt());
    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenTableNumberAlreadyExists_TC007() {
    // Arrange
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(true);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals("Table number already exists in this shop", exception.getMessage());

    verify(commonMapper, times(1)).checkShopExisted(validRequest.getShopId());
    verify(createTablesMapper, times(1))
        .checkTableNumberExisted(validRequest.getShopId(), validRequest.getTableNumber());
    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStatusIsInvalid_TC008() {
    // Arrange
    validRequest.setStatus(5);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)", exception.getMessage());

    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStatusIsZero_TC009() {
    // Arrange
    validRequest.setStatus(0);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)", exception.getMessage());

    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), anyInt(), any());
  }

  @Test
  void process_Success_WithStatusOccupied_TC010() {
    // Arrange
    validRequest.setStatus(2);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, ownerRole, currentUserShopId));

    verify(createTablesMapper, times(1))
        .createTable(
            validRequest.getTableNumber(),
            validRequest.getDescription(),
            2,
            validRequest.getShopId());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenStatusIsNull_TC011() {
    // Arrange
    validRequest.setStatus(null);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> createTablesService.process(validRequest, managerRole, currentUserShopId));

    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)", exception.getMessage());

    verify(createTablesMapper, never()).createTable(anyInt(), anyString(), any(), any());
  }

  @Test
  void process_Success_WithStatusReserved_TC012() {
    // Arrange
    validRequest.setStatus(3);
    when(commonMapper.checkShopExisted(validRequest.getShopId())).thenReturn(true);
    when(createTablesMapper.checkTableNumberExisted(
            validRequest.getShopId(), validRequest.getTableNumber()))
        .thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(
        () -> createTablesService.process(validRequest, ownerRole, currentUserShopId));

    verify(createTablesMapper, times(1))
        .createTable(
            validRequest.getTableNumber(),
            validRequest.getDescription(),
            3,
            validRequest.getShopId());
  }
}
