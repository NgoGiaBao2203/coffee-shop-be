package coffee.api.services;

import coffee.api.dto.request.drink.CreateDrinksRequest;
import coffee.api.mapper.CreateDrinkMapper;
import coffee.api.services.services_implement.drink.CreateDrinkServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateDrinkImplTest {

  @Mock
  private CreateDrinkMapper createDrinkMapper;

  @InjectMocks
  private CreateDrinkServiceImpl createDrinkService;

  private CreateDrinksRequest validRequest;
  private UUID currentUserId;
  private String currentUserRoleName;
  private UUID drinkId;
  private UUID shopId;
  private UUID drinkCategoryId;

  @BeforeEach
  void setUp() {
    drinkId = UUID.fromString("f3333333-3333-3333-3333-333333333333");
    shopId = UUID.fromString("b1000000-0000-0000-0000-000000000001");
    drinkCategoryId = UUID.fromString("c1000000-0000-0000-0000-000000000002");
    currentUserId = UUID.randomUUID();
    currentUserRoleName = "MANAGER";

    validRequest = new CreateDrinksRequest();
    validRequest.setDrinkId(drinkId);
    validRequest.setDrinkName("Trà Sữa Oolong Lài Kem Mặn");
    validRequest.setPrice(52000.0f);
    validRequest.setSize("L");
    validRequest.setImageUrl("https://example.com/images/oolonglaikemman.png");
    validRequest.setStatus(1);
    validRequest.setIsDeleted(false);
    validRequest.setShopID(shopId);
    validRequest.setDrinkCategoryId(drinkCategoryId);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<UUID> detailIdCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<String> sizeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Float> priceCaptor = ArgumentCaptor.forClass(Float.class);
    ArgumentCaptor<UUID> drinkIdCaptor = ArgumentCaptor.forClass(UUID.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
    verify(createDrinkMapper, times(1)).createDrink(
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

    verify(createDrinkMapper, times(1)).createDrinkDetail(
      detailIdCaptor.capture(),
      sizeCaptor.capture(),
      priceCaptor.capture(),
      drinkIdCaptor.capture()
    );

    assertNotNull(detailIdCaptor.getValue());
    assertEquals(validRequest.getSize(), sizeCaptor.getValue());
    assertEquals(validRequest.getPrice(), priceCaptor.getValue());
    assertEquals(validRequest.getDrinkId(), drinkIdCaptor.getValue());
  }

  @Test
  void process_ThrowsRuntimeException_WhenDrinkAlreadyExists_TC002() {
    // Arrange
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(true);

    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
      createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
    );

    assertEquals("The drink already exists.", exception.getMessage());

    verify(createDrinkMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
    verify(createDrinkMapper, never()).createDrink(any(), any(), any(), anyInt(), any(), any(), any(), any(), any());
    verify(createDrinkMapper, never()).createDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_SuccessWithDifferentSize_TC003() {
    // Arrange
    validRequest.setSize("M");
    validRequest.setPrice(45000.0f);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<String> sizeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Float> priceCaptor = ArgumentCaptor.forClass(Float.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrinkDetail(
      any(),
      sizeCaptor.capture(),
      priceCaptor.capture(),
      eq(validRequest.getDrinkId())
    );

    assertEquals("M", sizeCaptor.getValue());
    assertEquals(45000.0f, priceCaptor.getValue());
  }

  @Test
  void process_SuccessWithStatusInactive_TC004() {
    // Arrange
    validRequest.setStatus(0);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<Integer> statusCaptor = ArgumentCaptor.forClass(Integer.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrink(
      any(), any(), any(), statusCaptor.capture(), any(), any(), any(), any(), any()
    );

    assertEquals(0, statusCaptor.getValue());
  }

  @Test
  void process_SuccessWithIsDeletedTrue_TC005() {
    // Arrange
    validRequest.setIsDeleted(true);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<Boolean> isDeletedCaptor = ArgumentCaptor.forClass(Boolean.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrink(
      any(), any(), any(), any(), isDeletedCaptor.capture(), any(), any(), any(), any()
    );

    assertEquals(true, isDeletedCaptor.getValue());
  }

  @Test
  void process_SuccessWithDifferentDrinkNames_TC006() {
    // Arrange
    validRequest.setDrinkName("Cà Phê Đen Đá");
    when(createDrinkMapper.checkDrinkExistedByName("Cà Phê Đen Đá", validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrink(
      any(), nameCaptor.capture(), any(), any(), any(), any(), any(), any(), any()
    );

    assertEquals("Cà Phê Đen Đá", nameCaptor.getValue());
  }

  @Test
  void process_SuccessWithDifferentShops_TC007() {
    // Arrange
    UUID differentShopId = UUID.fromString("b2000000-0000-0000-0000-000000000002");
    validRequest.setShopID(differentShopId);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), differentShopId))
      .thenReturn(false);

    ArgumentCaptor<UUID> shopIdCaptor = ArgumentCaptor.forClass(UUID.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrink(
      any(), any(), any(), any(), any(), shopIdCaptor.capture(), any(), any(), any()
    );

    assertEquals(differentShopId, shopIdCaptor.getValue());
  }

  @Test
  void process_ThrowsExceptionWhenCheckDrinkMapperFails_TC008() {
    // Arrange
    when(createDrinkMapper.checkDrinkExistedByName(anyString(), any()))
      .thenThrow(new DataAccessException("Database connection error") {});

    // Act & Assert
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
      createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
    );

    assertEquals("Database connection error", exception.getMessage());

    verify(createDrinkMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
    verify(createDrinkMapper, never()).createDrink(any(), any(), any(), anyInt(), any(), any(), any(), any(), any());
    verify(createDrinkMapper, never()).createDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsExceptionWhenCreateDrinkMapperFails_TC009() {
    // Arrange
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);
    doThrow(new DataAccessException("Database insertion error") {})
      .when(createDrinkMapper).createDrink(any(), any(), any(), anyInt(), any(), any(), any(), any(), anyString());

    // Act & Assert
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
      createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
    );

    assertEquals("Database insertion error", exception.getMessage());

    verify(createDrinkMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
    verify(createDrinkMapper, times(1)).createDrink(any(), any(), any(), anyInt(), any(), any(), any(), any(), any());
    verify(createDrinkMapper, never()).createDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_ThrowsExceptionWhenCreateDrinkDetailMapperFails_TC010() {
    // Arrange
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);
    doThrow(new DataAccessException("Database insertion error for drink detail") {})
      .when(createDrinkMapper).createDrinkDetail(any(), anyString(), anyFloat(), any());

    // Act & Assert
    DataAccessException exception = assertThrows(DataAccessException.class, () ->
      createDrinkService.process(validRequest, currentUserId, currentUserRoleName)
    );

    assertEquals("Database insertion error for drink detail", exception.getMessage());

    verify(createDrinkMapper, times(1)).checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID());
    verify(createDrinkMapper, times(1)).createDrink(any(), any(), any(), anyInt(), any(), any(), any(), any(), any());
    verify(createDrinkMapper, times(1)).createDrinkDetail(any(), any(), any(), any());
  }

  @Test
  void process_SuccessWithHighPrice_TC011() {
    // Arrange
    validRequest.setPrice(999999.99f);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<Float> priceCaptor = ArgumentCaptor.forClass(Float.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrinkDetail(any(), any(), priceCaptor.capture(), any());
    assertEquals(999999.99f, priceCaptor.getValue());
  }

  @Test
  void process_SuccessWithLowPrice_TC012() {
    // Arrange
    validRequest.setPrice(1000.0f);
    when(createDrinkMapper.checkDrinkExistedByName(validRequest.getDrinkName(), validRequest.getShopID()))
      .thenReturn(false);

    ArgumentCaptor<Float> priceCaptor = ArgumentCaptor.forClass(Float.class);

    // Act
    assertDoesNotThrow(() -> createDrinkService.process(validRequest, currentUserId, currentUserRoleName));

    // Assert
    verify(createDrinkMapper, times(1)).createDrinkDetail(any(), any(), priceCaptor.capture(), any());
    assertEquals(1000.0f, priceCaptor.getValue());
  }
}