package coffee.api.services.services_implement.drink;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.exceptions.UserExistException;
import coffee.api.mapper.CreateDrinkMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

@ExtendWith(MockitoExtension.class)
public class CreateDrinkServiceImplTest {

  @Mock private CreateDrinkMapper createDrinkMapper;

  @InjectMocks private CreateDrinkServiceImpl createDrinkService;

  private CreateDrinksRequest validRequest;
  private UUID currentShopId;
  private UUID requestShopId;
  private UUID drinkCategoryId;
  private UUID drinkDetailId;

  @BeforeEach
  void setUp() {
    currentShopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    requestShopId = UUID.fromString("b2000000-0000-0000-0000-000000000002");
    drinkCategoryId = UUID.fromString("c1000000-0000-0000-0000-000000000001");
    drinkDetailId = UUID.randomUUID();

    validRequest = new CreateDrinksRequest();
    validRequest.setDrinkName("Trà Sữa Oolong Lài Kem Mặn");
    validRequest.setPrice(52000.0f);
    validRequest.setSize("L");
    validRequest.setImageUrl("https://example.com/images/oolonglaikemman.png");
    validRequest.setStatus(1);
    validRequest.setIsDeleted(false);
    validRequest.setShopId(requestShopId);
    validRequest.setDrinkCategoryId(drinkCategoryId);
    validRequest.setDrinkDetailId(drinkDetailId);
  }

  @Test
  void process_Success_AsManager_TC001() {
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    verify(createDrinkMapper, times(1))
        .checkDrinkExistedByName(currentShopId, validRequest.getDrinkName());
    verify(createDrinkMapper, times(1))
        .createDrink(
            eq(validRequest.getDrinkCategoryId()),
            eq(validRequest.getDrinkDetailId()),
            eq(currentShopId),
            eq(validRequest.getDrinkName()),
            eq(validRequest.getImageUrl()),
            eq(validRequest.getStatus()),
            eq(validRequest.getIsDeleted()),
            eq("L"),
            eq(52000.0f));
  }

  @Test
  void process_Success_AsOwner_TC002() {
    when(createDrinkMapper.checkDrinkExistedByName(requestShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentShopId, "OWNER"));

    verify(createDrinkMapper, times(1))
        .checkDrinkExistedByName(requestShopId, validRequest.getDrinkName());
    verify(createDrinkMapper, times(1))
        .createDrink(
            eq(validRequest.getDrinkCategoryId()),
            eq(validRequest.getDrinkDetailId()),
            eq(requestShopId),
            eq(validRequest.getDrinkName()),
            eq(validRequest.getImageUrl()),
            eq(validRequest.getStatus()),
            eq(validRequest.getIsDeleted()),
            eq("L"),
            eq(52000.0f));
  }

  @Test
  void process_Success_WithLowerCaseSize_TC003() {
    validRequest.setSize("m ");
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    verify(createDrinkMapper, times(1))
        .createDrink(any(), any(), any(), any(), any(), any(), any(), eq("M"), any());
  }

  @Test
  void process_ThrowsUserExistException_WhenDrinkAlreadyExists_TC004() {
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(true);

    UserExistException exception =
        assertThrows(
            UserExistException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Drink is existed", exception.getMessage());
    verify(createDrinkMapper, never())
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsRuntimeException_WhenSizeIsInvalid_TC005() {
    validRequest.setSize("XL");
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Size is invalid. Must be S, M, or L", exception.getMessage());
    verify(createDrinkMapper, never())
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsRuntimeException_WhenPriceIsNull_TC006() {
    validRequest.setPrice(null);
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Price must be greater than 0", exception.getMessage());
    verify(createDrinkMapper, never())
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsRuntimeException_WhenPriceIsZeroOrNegative_TC007() {
    validRequest.setPrice(0.0f);
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Price must be greater than 0", exception.getMessage());
    verify(createDrinkMapper, never())
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsException_WhenCheckDrinkMapperFails_TC008() {
    when(createDrinkMapper.checkDrinkExistedByName(any(), anyString()))
        .thenThrow(new DataAccessException("Database connection error") {});

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Database connection error", exception.getMessage());
    verify(createDrinkMapper, never())
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsException_WhenCreateDrinkMapperFails_TC009() {
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    doThrow(new DataAccessException("Database insertion error") {})
        .when(createDrinkMapper)
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());

    DataAccessException exception =
        assertThrows(
            DataAccessException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Database insertion error", exception.getMessage());
    verify(createDrinkMapper, times(1))
        .createDrink(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void process_ThrowsRuntimeException_WhenSizeIsNull_TC010() {
    validRequest.setSize(null);
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    assertEquals("Size is invalid. Must be S, M, or L", exception.getMessage());
  }

  @Test
  void process_Success_AsOtherRole_TC011() {
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentShopId, "STAFF"));

    verify(createDrinkMapper, times(1))
        .checkDrinkExistedByName(currentShopId, validRequest.getDrinkName());
  }

  @Test
  void process_Success_WithSizeS_TC012() {
    validRequest.setSize("s");
    when(createDrinkMapper.checkDrinkExistedByName(currentShopId, validRequest.getDrinkName()))
        .thenReturn(false);

    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentShopId, "MANAGER"));

    verify(createDrinkMapper, times(1))
        .createDrink(any(), any(), any(), any(), any(), any(), any(), eq("S"), any());
  }
}
