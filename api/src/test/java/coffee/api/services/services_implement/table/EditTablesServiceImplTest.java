package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.EditTablesRequest;
import coffee.api.enums.ResponseCode;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestWithErrorDetailsException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.EditTablesMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EditTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private EditTablesMapper editTablesMapper;

  @InjectMocks private EditTablesServiceImpl editTablesService;

  private EditTablesRequest validRequest;
  private UUID tableId;
  private UUID shopId;
  private UUID currentUserId;
  private UUID currentUserShopId;
  private String managerRole;
  private String ownerRole;

  @BeforeEach
  void setUp() {
    tableId = UUID.randomUUID();
    shopId = UUID.randomUUID();
    currentUserId = UUID.randomUUID();
    currentUserShopId = shopId;

    managerRole = Roles.MANAGER.getValue();
    ownerRole = Roles.OWNER.getValue();

    validRequest = new EditTablesRequest();
    validRequest.setTableId(tableId);
    validRequest.setShopId(shopId);
    validRequest.setTableNumber(1);
    validRequest.setStatus(1);
    validRequest.setDescription("Updated description");
  }

  @Test
  void process_Success_WhenManagerEditsTableInOwnShop_TC001() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, managerRole, currentUserShopId))
        .thenReturn(false);

    // Act
    editTablesService.process(validRequest, managerRole, currentUserId, currentUserShopId);

    // Assert
    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, managerRole, currentUserShopId);
    verify(editTablesMapper, times(1)).updateTable(validRequest, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenOwnerEditsTable_TC002() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act
    editTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId);

    // Assert
    verify(commonMapper, times(1)).checkTableExisted(tableId, ownerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(commonMapper, times(1))
        .checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId);
    verify(editTablesMapper, times(1)).updateTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenStatusIsNull_TC003() {
    // Arrange
    validRequest.setStatus(null);
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act
    editTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId);

    // Assert
    verify(editTablesMapper, times(1)).updateTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenStatusIsReservedBoundary_TC004() {
    // Arrange
    validRequest.setStatus(3);
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act
    editTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId);

    // Assert
    verify(editTablesMapper, times(1)).updateTable(validRequest, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDoesNotExist_TC005() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table not found", exception.getMessage());
    assertEquals(tableId, exception.getId());

    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, never()).checkShopExisted(any());
    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC006() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop not found", exception.getMessage());
    assertEquals(shopId, exception.getId());

    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerShopIdDoesNotExist_TC007() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop Id not found", exception.getMessage());
    assertEquals(currentUserShopId, exception.getId());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerEditsDifferentShop_TC008() {
    // Arrange
    UUID differentShopId = UUID.randomUUID();
    validRequest.setShopId(differentShopId);

    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(differentShopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                editTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Access denied for this shop", exception.getMessage());
    assertEquals(differentShopId, exception.getId());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenTableNumberAlreadyExists_TC009() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(true);

    // Act & Assert
    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editTablesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertEquals(1, exception.getErrorDetails().size());
    assertEquals(
        ResponseCode.CONFLICT.getCode(), exception.getErrorDetails().get(0).getErrorCode());
    assertEquals(
        "Table number already exists in this shop",
        exception.getErrorDetails().get(0).getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenStatusIsLessThanOne_TC010() {
    // Arrange
    validRequest.setStatus(0);
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editTablesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertEquals(1, exception.getErrorDetails().size());
    assertEquals(
        ResponseCode.BAD_REQUEST.getCode(), exception.getErrorDetails().get(0).getErrorCode());
    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)",
        exception.getErrorDetails().get(0).getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestWithErrorDetailsException_WhenStatusIsGreaterThanThree_TC011() {
    // Arrange
    validRequest.setStatus(4);
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(false);

    // Act & Assert
    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editTablesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertEquals(1, exception.getErrorDetails().size());
    assertEquals(
        ResponseCode.BAD_REQUEST.getCode(), exception.getErrorDetails().get(0).getErrorCode());
    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)",
        exception.getErrorDetails().get(0).getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }

  @Test
  void
      process_ThrowsInvalidRequestWithErrorDetailsException_WhenBothTableNumberAndStatusAreInvalid_TC012() {
    // Arrange
    validRequest.setStatus(5);
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkTableNumberExisted(
            tableId, validRequest.getTableNumber(), shopId, ownerRole, currentUserShopId))
        .thenReturn(true);

    // Act & Assert
    InvalidRequestWithErrorDetailsException exception =
        assertThrows(
            InvalidRequestWithErrorDetailsException.class,
            () ->
                editTablesService.process(
                    validRequest, ownerRole, currentUserId, currentUserShopId));

    assertEquals("Invalid request", exception.getMessage());
    assertEquals(2, exception.getErrorDetails().size());
    assertEquals(
        ResponseCode.CONFLICT.getCode(), exception.getErrorDetails().get(0).getErrorCode());
    assertEquals(
        "Table number already exists in this shop",
        exception.getErrorDetails().get(0).getMessage());
    assertEquals(
        ResponseCode.BAD_REQUEST.getCode(), exception.getErrorDetails().get(1).getErrorCode());
    assertEquals(
        "Status must be 1 (available), 2 (occupied), or 3 (reserved)",
        exception.getErrorDetails().get(1).getMessage());

    verify(editTablesMapper, never()).updateTable(any(), any(), any());
  }
}
