package coffee.api.services.services_implement.shop_branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.shop_branch.DeleteShopBranchRequest;
import coffee.api.enums.ValidationMessage;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.exceptions.InvalidRequestException;
import coffee.api.mapper.DeleteShopBranchMapper;
import coffee.api.security.CustomUserDetail;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class DeleteShopBranchServiceImplTest {

  @Mock private DeleteShopBranchMapper deleteShopBranchMapper;
  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;

  @InjectMocks private DeleteShopBranchServiceImpl deleteShopBranchService;

  private Validator validator;
  private DeleteShopBranchRequest validRequest;
  private final UUID testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @BeforeEach
  void setUp() {
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      validator = factory.getValidator();
    }

    validRequest = new DeleteShopBranchRequest();
    validRequest.setShopId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    // Set up default Security Context with an authenticated CustomUserDetail
    CustomUserDetail userDetail =
        new CustomUserDetail(
            testUserId,
            "owner@coffeeshop.com",
            "encodedPassword",
            true,
            "OWNER",
            null,
            Collections.emptyList());

    SecurityContextHolder.setContext(securityContext);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(userDetail);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // =========================================================================
  // SUCCESS / NORMAL CASES
  // =========================================================================

  @Test
  void process_Success_CascadeSoftDeleteAllRelatedEntities_TC001() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopBranchService.process(validRequest));

    // Assert:
    InOrder inOrder = inOrder(deleteShopBranchMapper);
    inOrder.verify(deleteShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .countActiveInvoicesByShopId(validRequest.getShopId());
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinksByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteCategoriesByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteTablesByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteProfileShopsByShopId(validRequest.getShopId(), testUserId);
    inOrder
        .verify(deleteShopBranchMapper, times(1))
        .softDeleteShopBranch(validRequest.getShopId(), testUserId);
  }

  @Test
  void process_Success_WhenAuthenticationIsNull_TC002() {
    // Arrange: SecurityContext returns null Authentication (null safety branch check)
    when(securityContext.getAuthentication()).thenReturn(null);
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopBranchService.process(validRequest));

    // Assert
    verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinksByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteCategoriesByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteTablesByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteProfileShopsByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1)).softDeleteShopBranch(validRequest.getShopId(), null);
  }

  @Test
  void process_Success_WhenPrincipalIsNotCustomUserDetail_TC003() {
    // Arrange: Authentication is present but principal is not CustomUserDetail (e.g. anonymous
    // user)
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn("anonymousUser");
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    // Act
    assertDoesNotThrow(() -> deleteShopBranchService.process(validRequest));

    // Assert: Fallback deletedBy safely to null
    verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinksByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteCategoriesByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteTablesByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1))
        .softDeleteProfileShopsByShopId(validRequest.getShopId(), null);
    verify(deleteShopBranchMapper, times(1)).softDeleteShopBranch(validRequest.getShopId(), null);
  }

  // =========================================================================
  // ABNORMAL / EXCEPTION CASES
  // =========================================================================

  @Test
  void process_ThrowsDataNotFoundException_WhenShopDoesNotExist_TC004() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception =
        assertThrows(
            DataNotFoundException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Data not found", exception.getMessage());
    assertEquals(validRequest.getShopId(), exception.getId());

    verify(deleteShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopBranchMapper, never()).countActiveInvoicesByShopId(any());
    verify(deleteShopBranchMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteDrinksByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteCategoriesByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteTablesByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteProfileShopsByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteShopBranch(any(), any());
  }

  @Test
  void process_ThrowsInvalidRequestException_WhenPendingInvoicesExist_TC005() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(3);

    // Act & Assert
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Can not delete branch with pending invoices", exception.getMessage());

    verify(deleteShopBranchMapper, times(1)).checkShopExistedById(validRequest.getShopId());
    verify(deleteShopBranchMapper, times(1)).countActiveInvoicesByShopId(validRequest.getShopId());
    verify(deleteShopBranchMapper, never()).softDeleteDrinkDetailsByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteDrinksByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteCategoriesByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteTablesByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteProfileShopsByShopId(any(), any());
    verify(deleteShopBranchMapper, never()).softDeleteShopBranch(any(), any());
  }

  @Test
  void process_ThrowsDataAccessException_WhenDatabaseFails_TC006() {
    // Arrange
    when(deleteShopBranchMapper.checkShopExistedById(validRequest.getShopId())).thenReturn(true);
    when(deleteShopBranchMapper.countActiveInvoicesByShopId(validRequest.getShopId()))
        .thenReturn(0);

    doThrow(new DataAccessException("Database deletion error") {})
        .when(deleteShopBranchMapper)
        .softDeleteDrinkDetailsByShopId(eq(validRequest.getShopId()), any());

    // Act & Assert
    DataAccessException exception =
        assertThrows(
            DataAccessException.class, () -> deleteShopBranchService.process(validRequest));

    assertEquals("Database deletion error", exception.getMessage());
    verify(deleteShopBranchMapper, times(1))
        .softDeleteDrinkDetailsByShopId(validRequest.getShopId(), testUserId);
  }

  // =========================================================================
  // REQUEST BEAN VALIDATION CASES
  // =========================================================================

  @Test
  void process_ValidationSuccess_WhenShopIdIsValid_TC007() {
    Set<ConstraintViolation<DeleteShopBranchRequest>> violations = validator.validate(validRequest);
    assertEquals(0, violations.size());
  }

  @Test
  void process_ValidationFails_WhenShopIdIsNull_TC008() {
    validRequest.setShopId(null);
    Set<ConstraintViolation<DeleteShopBranchRequest>> violations = validator.validate(validRequest);

    assertEquals(1, violations.size());
    assertEquals(ValidationMessage.Msg.FIELD_REQUIRED, violations.iterator().next().getMessage());
  }
}
