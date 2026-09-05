package coffee.api.services.services_implement.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.table.DeleteTablesRequest;
import coffee.api.enums.Roles;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteTablesMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteTablesServiceImplTest {

  @Mock private CommonMapper commonMapper;

  @Mock private DeleteTablesMapper deleteTablesMapper;

  @InjectMocks private DeleteTablesServiceImpl deleteTablesService;

  private DeleteTablesRequest validRequest;
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

    validRequest = new DeleteTablesRequest();
    validRequest.setTableId(tableId);
    validRequest.setShopId(shopId);
  }

  @Test
  void process_Success_WhenManagerDeletesTableInOwnShop_TC001() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(true);

    // Act
    deleteTablesService.process(validRequest, managerRole, currentUserId, currentUserShopId);

    // Assert
    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, times(1)).checkShopIdIsExisted(currentUserId, currentUserShopId);
    verify(deleteTablesMapper, times(1)).deleteTable(tableId, managerRole, currentUserShopId);
  }

  @Test
  void process_Success_WhenOwnerDeletesTable_TC002() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, ownerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);

    // Act
    deleteTablesService.process(validRequest, ownerRole, currentUserId, currentUserShopId);

    // Assert
    verify(commonMapper, times(1)).checkTableExisted(tableId, ownerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteTablesMapper, times(1)).deleteTable(tableId, ownerRole, currentUserShopId);
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenTableDoesNotExist_TC003() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Table not found", exception.getMessage());
    assertEquals(tableId, exception.getId());

    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, never()).checkShopExisted(any());
    verify(deleteTablesMapper, never()).deleteTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC004() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop not found", exception.getMessage());
    assertEquals(shopId, exception.getId());

    verify(commonMapper, times(1)).checkTableExisted(tableId, managerRole, currentUserShopId);
    verify(commonMapper, times(1)).checkShopExisted(shopId);
    verify(commonMapper, never()).checkShopIdIsExisted(any(), any());
    verify(deleteTablesMapper, never()).deleteTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerShopIdDoesNotExist_TC005() {
    // Arrange
    when(commonMapper.checkTableExisted(tableId, managerRole, currentUserShopId)).thenReturn(true);
    when(commonMapper.checkShopExisted(shopId)).thenReturn(true);
    when(commonMapper.checkShopIdIsExisted(currentUserId, currentUserShopId)).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class,
            () ->
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Shop Id not found", exception.getMessage());
    assertEquals(currentUserShopId, exception.getId());

    verify(deleteTablesMapper, never()).deleteTable(any(), any(), any());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenManagerAttemptsToDeleteFromDifferentShop_TC006() {
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
                deleteTablesService.process(
                    validRequest, managerRole, currentUserId, currentUserShopId));

    assertEquals("Access denied for this shop", exception.getMessage());
    assertEquals(differentShopId, exception.getId());

    verify(deleteTablesMapper, never()).deleteTable(any(), any(), any());
  }
}
