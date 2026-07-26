package coffee.api.services;

import coffee.api.dto.request.user.SearchUsersRequest;
import coffee.api.dto.response.base_response.PageResponse;
import coffee.api.dto.result.ProfileResult;
import coffee.api.enums.SortDirection;
import coffee.api.mapper.GetStaffsMapper;
import coffee.api.services.services_implement.staff.GetStaffsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GetStaffsServiceImplTest {

  @Mock
  private GetStaffsMapper getStaffsMapper;

  @InjectMocks
  private GetStaffsServiceImpl getStaffsService;

  private SearchUsersRequest validRequest;
  private ProfileResult sampleStaffResult;
  private UUID currentUserId;
  private String currentUserRoleName;

  @BeforeEach
  void setUp() {
    // Initialize standard valid request with dropdown filters (roleId & branchShopId)
    validRequest = new SearchUsersRequest();
    validRequest.setPage(1);
    validRequest.setSize(10);
    validRequest.setSearch("Nguyen");
    validRequest.setRoleId("ROLE_STAFF_ID_001");
    validRequest.setBranchShopId("SHOP_ID_001");
    validRequest.setSortBy("fullName");
    validRequest.setSortDirection(SortDirection.DESC);

    // Context user configurations
    currentUserId = UUID.randomUUID();
    currentUserRoleName = "MANAGER";

    // Build mock single returned item
    sampleStaffResult = new ProfileResult();
    sampleStaffResult.setProfileId(UUID.randomUUID());
    sampleStaffResult.setEmail("staff1@coffee.com");
    sampleStaffResult.setUsername("staff1");
    sampleStaffResult.setFullName("Nguyen Van A");
    sampleStaffResult.setPhoneNumber("0901234567");
    sampleStaffResult.setShopName("Saigon Drip & Brew");
    sampleStaffResult.setRoleName("STAFF"); // Will be converted to "NHÂN VIÊN" by ConvertRoleVN
    sampleStaffResult.setCreatedAt(LocalDateTime.now());
    sampleStaffResult.setUpdatedAt(LocalDateTime.now());
    sampleStaffResult.setIsDeleted(false);
  }

  @Test
  void process_SuccessWithItems_TC001() {
    // Arrange
    String search = validRequest.trimmedSearch();
    String roleId = validRequest.getRoleId();
    String branchShopId = validRequest.getBranchShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 1L;

    List<ProfileResult> expectedItems = Collections.singletonList(sampleStaffResult);

    when(getStaffsMapper.countStaffsFiltered(
      search, roleId, branchShopId, currentUserRoleName, currentUserId
    )).thenReturn(totalElements);

    when(getStaffsMapper.getStaffsFiltered(
      search, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId
    )).thenReturn(expectedItems);

    // Act
    PageResponse<ProfileResult> response = getStaffsService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertEquals("Get staff list successfully", response.getMessage());
    assertNotNull(response.getItems());
    assertEquals(1, response.getItems().size());
    assertEquals("staff1@coffee.com", response.getItems().getFirst().getEmail());

    // Verifies Vietnamese translation conversion for role name
    assertEquals("NHÂN VIÊN", response.getItems().getFirst().getRoleName());

    // Pagination metadata assertions
    assertNotNull(response.getPagination());
    assertEquals(1, response.getPagination().getPage());
    assertEquals(10, response.getPagination().getSize());
    assertEquals(1L, response.getPagination().getTotalElements());
    assertEquals(1, response.getPagination().getTotalPages());

    verify(getStaffsMapper, times(1))
      .countStaffsFiltered(search, roleId, branchShopId, currentUserRoleName, currentUserId);
    verify(getStaffsMapper, times(1))
      .getStaffsFiltered(search, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_SuccessWithEmptyResults_TC002() {
    // Arrange
    validRequest.setSearch("NonExistentUser");
    String search = validRequest.trimmedSearch();
    String roleId = validRequest.getRoleId();
    String branchShopId = validRequest.getBranchShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 0L;

    when(getStaffsMapper.countStaffsFiltered(
      search, roleId, branchShopId, currentUserRoleName, currentUserId
    )).thenReturn(totalElements);

    when(getStaffsMapper.getStaffsFiltered(
      search, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId
    )).thenReturn(Collections.emptyList());

    // Act
    PageResponse<ProfileResult> response = getStaffsService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNotNull(response);
    assertTrue(response.getItems().isEmpty());
    assertEquals(0L, response.getPagination().getTotalElements());
    assertEquals(0, response.getPagination().getTotalPages());

    verify(getStaffsMapper, times(1))
      .countStaffsFiltered(search, roleId, branchShopId, currentUserRoleName, currentUserId);
    verify(getStaffsMapper, times(1))
      .getStaffsFiltered(search, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }

  @Test
  void process_SuccessWithNullAndBlankSearchStrings_TC003() {
    // Arrange
    validRequest.setSearch("   "); // Trimmable whitespace input
    String search = validRequest.trimmedSearch(); // Evaluates to null
    String roleId = validRequest.getRoleId();
    String branchShopId = validRequest.getBranchShopId();
    String sortBy = validRequest.getSortBy();
    String sortDirection = validRequest.directionValue();
    int size = validRequest.getSize();
    int offset = validRequest.calcOffset();
    long totalElements = 5L;

    when(getStaffsMapper.countStaffsFiltered(
      null, roleId, branchShopId, currentUserRoleName, currentUserId
    )).thenReturn(totalElements);

    when(getStaffsMapper.getStaffsFiltered(
      null, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId
    )).thenReturn(Collections.singletonList(sampleStaffResult));

    // Act
    PageResponse<ProfileResult> response = getStaffsService.process(validRequest, currentUserRoleName, currentUserId);

    // Assert
    assertNull(search);
    assertNotNull(response);
    assertEquals(5L, response.getPagination().getTotalElements());
    assertEquals("NHÂN VIÊN", response.getItems().getFirst().getRoleName());

    verify(getStaffsMapper, times(1))
      .countStaffsFiltered(null, roleId, branchShopId, currentUserRoleName, currentUserId);
    verify(getStaffsMapper, times(1))
      .getStaffsFiltered(null, roleId, branchShopId, sortBy, sortDirection, size, offset, currentUserRoleName, currentUserId);
  }
}