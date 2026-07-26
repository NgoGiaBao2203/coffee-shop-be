package coffee.api.services.drink;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.mapper.CreateDrinksMapper;
import coffee.api.services.services_implement.drink.CreateDrinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateDrinkServiceImplTest {

    @Mock
    private CreateDrinksMapper createDrinksMapper;

    @InjectMocks
    private CreateDrinkServiceImpl createDrinkService;

    private CreateDrinksRequest validRequest;
    private UUID currentUserId;
    private String currentUserRoleName;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.fromString("a1a1a1a1-1a1a-1a1a-1a1a-1a1a1a1a1a1a");
        currentUserRoleName = "MANAGER";

        validRequest = new CreateDrinksRequest();
        validRequest.setDrinkId(UUID.fromString("d1d1d1d1-1d1d-1d1d-1d1d-1d1d1d1d1d1d"));
        validRequest.setDrinkName("Espresso Cold Brew");
        validRequest.setImageUrl("https://example.com/images/espresso-cold-brew.jpg");
        validRequest.setStatus(1);
        validRequest.setIsDeleted(false);
        validRequest.setShopID(UUID.fromString("s1s1s1s1-1s1s-1s1s-1s1s-1s1s1s1s1s1s"));
        validRequest.setDrinkCategoryId(UUID.fromString("c1c1c1c1-1c1c-1c1c-1c1c-1c1c1c1c1c1c"));
        validRequest.setSize("M");
        validRequest.setPrice(new BigDecimal("45000.00"));
    }

    // =========================================================================
    // NORMAL CASES
    // =========================================================================

    @Test
    void process_Success_TC001() {
        // Arrange
        when(createDrinksMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID())).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

        // Assert
        verify(createDrinksMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());

        verify(createDrinksMapper, times(1)).createDrink(
                validRequest.getDrinkId(),
                validRequest.getDrinkName(),
                validRequest.getImageUrl(),
                validRequest.getStatus(),
                validRequest.getIsDeleted(),
                validRequest.getShopID(),
                validRequest.getDrinkCategoryId(),
                currentUserId,
                currentUserRoleName
        );

        ArgumentCaptor<UUID> detailIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(createDrinksMapper, times(1)).createDrinkDetail(
                detailIdCaptor.capture(),
                eq(validRequest.getSize()),
                eq(validRequest.getPrice()),
                eq(validRequest.getDrinkId())
        );
        assertNotNull(detailIdCaptor.getValue());
    }

    @Test
    void process_Success_WhenCheckDrinkExistedReturnsNull_TC002() {
        // Arrange
        when(createDrinksMapper.checkDrinkExistedByName(anyString(), any(UUID.class))).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

        // Verify that creation still proceeds
        verify(createDrinksMapper, times(1)).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(createDrinksMapper, times(1)).createDrinkDetail(any(), any(), any(), any());
    }

    @Test
    void process_Success_WhenPriceIsZero_TC003() {
        // Arrange
        validRequest.setPrice(BigDecimal.ZERO);
        when(createDrinksMapper.checkDrinkExistedByName(anyString(), any(UUID.class))).thenReturn(false);

        // Act
        assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

        // Assert
        verify(createDrinksMapper, times(1)).createDrinkDetail(
                any(UUID.class),
                anyString(),
                eq(BigDecimal.ZERO),
                any(UUID.class)
        );
    }

    // =========================================================================
    // ABNORMAL CASES
    // =========================================================================

    @Test
    void process_ThrowsRuntimeException_WhenDrinkAlreadyExists_TC004() {
        // Arrange
        when(createDrinksMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID())).thenReturn(true);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
        );

        // Assert
        assertEquals("The drink already exists.", exception.getMessage());
        verify(createDrinksMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
        verify(createDrinksMapper, never()).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(createDrinksMapper, never()).createDrinkDetail(any(), any(), any(), any());
    }

    @Test
    void process_ThrowsException_WhenCreateDrinkFails_TC005() {
        // Arrange
        when(createDrinksMapper.checkDrinkExistedByName(anyString(), any(UUID.class))).thenReturn(false);
        doThrow(new DataIntegrityViolationException("DB error on createDrink"))
                .when(createDrinksMapper).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
        );

        // Verify createDrink was attempted, but createDrinkDetail was not
        verify(createDrinksMapper, times(1)).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(createDrinksMapper, never()).createDrinkDetail(any(), any(), any(), any());
    }

    @Test
    void process_ThrowsException_WhenCreateDrinkDetailFails_TC006() {
        // Arrange
        when(createDrinksMapper.checkDrinkExistedByName(anyString(), any(UUID.class))).thenReturn(false);
        doNothing().when(createDrinksMapper).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
        doThrow(new DataIntegrityViolationException("DB error on createDrinkDetail"))
                .when(createDrinksMapper).createDrinkDetail(any(), any(), any(), any());

        // Act & Assert
        assertThrows(DataIntegrityViolationException.class, () ->
                createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
        );

        // Verify both mapper methods were called, as the exception happens on the second call
        verify(createDrinksMapper, times(1)).createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(createDrinksMapper, times(1)).createDrinkDetail(any(), any(), any(), any());
    }
}