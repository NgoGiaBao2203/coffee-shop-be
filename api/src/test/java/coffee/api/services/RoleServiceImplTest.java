package coffee.api.services;

import coffee.api.dto.result.RoleResult;
import coffee.api.mapper.GetRoleMapper;
import coffee.api.services.services_implement.dropdown.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {

  @Mock
  private GetRoleMapper getRoleMapper;

  @InjectMocks
  private RoleServiceImpl roleService;

  private RoleResult ownerRole;
  private RoleResult managerRole;
  private RoleResult staffRole;

  @BeforeEach
  void setUp() {
    // Build mock returned items for common test configurations
    ownerRole = new RoleResult();
    ownerRole.setRoleName("OWNER");
    ownerRole.setRoleDisplayName(null);

    managerRole = new RoleResult();
    managerRole.setRoleName("MANAGER");
    managerRole.setRoleDisplayName(null);

    staffRole = new RoleResult();
    staffRole.setRoleName("STAFF");
    staffRole.setRoleDisplayName(null);
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    List<RoleResult> rawItems = Arrays.asList(ownerRole, managerRole, staffRole);
    when(getRoleMapper.getRoles()).thenReturn(rawItems);

    // Act
    List<RoleResult> results = roleService.process();

    // Assert
    assertNotNull(results);
    assertEquals(3, results.size());

    // Verify mapping properties match localized Vietnamese equivalents
    assertEquals("OWNER", results.get(0).getRoleName());
    assertEquals("CHỦ QUÁN", results.get(0).getRoleDisplayName());

    assertEquals("MANAGER", results.get(1).getRoleName());
    assertEquals("QUẢN LÝ", results.get(1).getRoleDisplayName());

    assertEquals("STAFF", results.get(2).getRoleName());
    assertEquals("NHÂN VIÊN", results.get(2).getRoleDisplayName());

    verify(getRoleMapper, times(1)).getRoles();
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    when(getRoleMapper.getRoles()).thenReturn(Collections.emptyList());

    // Act
    List<RoleResult> results = roleService.process();

    // Assert
    assertNotNull(results);
    assertTrue(results.isEmpty());

    verify(getRoleMapper, times(1)).getRoles();
  }
}