package coffee.api.services;

import coffee.api.dto.request.common.UserProfileRequest;
import coffee.api.dto.result.ProfileResult;
import coffee.api.exceptions.AccountDisableException;
import coffee.api.exceptions.InvalidUsernameOrPasswordException;
import coffee.api.model.UserProfile;
import coffee.api.repository.common.GetUserProfileRepository;
import coffee.api.services.services_implement.common.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceImplTest {
  @Mock
  private GetUserProfileRepository getUserProfileRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private ProfileServiceImpl profileService;

  private UserProfileRequest validRequest;
  private UserProfile activeProfile;

  @BeforeEach
  void setUp() {
    validRequest = new UserProfileRequest();
    validRequest.setUsername("testuser");
    validRequest.setPassword("rawPassword");

    activeProfile = new UserProfile();
    activeProfile.setUsername("testuser");
    activeProfile.setPassword("encodedPassword");
    activeProfile.setEmail("test@coffee.com");
    activeProfile.setIsDeleted(false);
  }

  @Test
  void process_Success_TC001() {
    // Arrange
    when(getUserProfileRepository.findByUsername(validRequest.getUsername())).thenReturn(activeProfile);
    when(passwordEncoder.matches(validRequest.getPassword(), activeProfile.getPassword())).thenReturn(true);

    // Act
    ProfileResult result = profileService.process(validRequest);

    // Assert
    assertNotNull(result);
    assertEquals("testuser", result.getUsername());
    assertEquals("test@coffee.com", result.getEmail());

    verify(getUserProfileRepository, times(1)).findByUsername(validRequest.getUsername());
    verify(passwordEncoder, times(1)).matches(validRequest.getPassword(), activeProfile.getPassword());
  }

  @Test
  void process_ThrowsInvalidUsernameOrPasswordException_TC002() {
    // Arrange
    when(getUserProfileRepository.findByUsername(validRequest.getUsername())).thenReturn(null);

    // Act & Assert
    InvalidUsernameOrPasswordException exception = assertThrows(
      InvalidUsernameOrPasswordException.class,
      () -> profileService.process(validRequest)
    );

    assertEquals("Username not found", exception.getMessage());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void process_ThrowsAccountDisableExceptionWhenDeleted_TC003() {
    // Arrange
    UserProfile deletedProfile = new UserProfile();
    deletedProfile.setUsername("testuser");
    deletedProfile.setEmail("test@coffee.com");
    deletedProfile.setIsDeleted(true);

    when(getUserProfileRepository.findByUsername(validRequest.getUsername())).thenReturn(deletedProfile);

    // Act & Assert
    AccountDisableException exception = assertThrows(
      AccountDisableException.class,
      () -> profileService.process(validRequest)
    );

    assertEquals("Profile was disabled", exception.getMessage());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void process_ThrowsAccountDisableExceptionWhenIsDeletedNull_TC004() {
    // Arrange
    UserProfile nullDeletedProfile = new UserProfile();
    nullDeletedProfile.setUsername("testuser");
    nullDeletedProfile.setEmail("test@coffee.com");
    nullDeletedProfile.setIsDeleted(null);

    when(getUserProfileRepository.findByUsername(validRequest.getUsername())).thenReturn(nullDeletedProfile);

    // Act & Assert
    assertThrows(AccountDisableException.class, () -> profileService.process(validRequest));
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void process_ThrowsInvalidUsernameOrPasswordExceptionWhenPasswordIncorrect_TC005() {
    // Arrange
    when(getUserProfileRepository.findByUsername(validRequest.getUsername())).thenReturn(activeProfile);
    when(passwordEncoder.matches(validRequest.getPassword(), activeProfile.getPassword())).thenReturn(false);

    // Act & Assert
    InvalidUsernameOrPasswordException exception = assertThrows(
      InvalidUsernameOrPasswordException.class,
      () -> profileService.process(validRequest)
    );

    assertEquals("Invalid password", exception.getMessage());
  }
}
