package coffee.api.services;

import coffee.api.dto.request.drink.DeleteDrinksRequest;
import coffee.api.exceptions.DataNotFoundException;
import coffee.api.mapper.CommonMapper;
import coffee.api.mapper.DeleteDrinkMapper;
import coffee.api.services.services_implement.drink.DeleteDrinkServiceImpl;
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
public class DeleteDrinkServiceImplTest {

  @Mock
  private CommonMapper commonMapper;

  @Mock
  private DeleteDrinkMapper deleteDrinkMapper;

  @InjectMocks
  private DeleteDrinkServiceImpl deleteDrinkService;

  private DeleteDrinksRequest validRequest;
  private UUID targetDrinkId;

  @BeforeEach
  void setUp() {
    targetDrinkId = UUID.randomUUID();

    validRequest = new DeleteDrinksRequest();
    validRequest.setDrinkId(targetDrinkId);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(commonMapper.checkDrinkExisted(validRequest.getDrinkId())).thenReturn(true);

    // Act
    assertDoesNotThrow(() -> deleteDrinkService.process(validRequest));

    // Assert
    verify(commonMapper, times(1)).checkDrinkExisted(validRequest.getDrinkId());
    verify(deleteDrinkMapper, times(1)).deleteDrink(validRequest.getDrinkId());
  }

  @Test
  void process_ThrowsDataNotFoundException_WhenDrinkDoesNotExist_TC002() {
    // Arrange
    when(commonMapper.checkDrinkExisted(validRequest.getDrinkId())).thenReturn(false);

    // Act & Assert
    DataNotFoundException exception = assertThrows(DataNotFoundException.class, () ->
      deleteDrinkService.process(validRequest)
    );

    assertEquals("Data not found", exception.getMessage());
    assertEquals(targetDrinkId, exception.getId()); // Match custom payload tracker from earlier cases

    // Verify it fails fast and never hits the database modification mapper layer
    verify(commonMapper, times(1)).checkDrinkExisted(validRequest.getDrinkId());
    verify(deleteDrinkMapper, never()).deleteDrink(any());
  }
}