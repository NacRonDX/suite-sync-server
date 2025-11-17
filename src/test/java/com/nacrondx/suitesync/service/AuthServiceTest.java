package com.nacrondx.suitesync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.auth.LoginRequest;
import com.nacrondx.suitesync.model.auth.LoginResponse;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  @InjectMocks private AuthService authService;

  private User testUser;
  private LoginRequest loginRequest;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    loginRequest = new LoginRequest("test@example.com", "password123");
  }

  @Test
  void authenticateWithValidCredentialsShouldReturnLoginResponse() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);
    when(jwtService.generateToken(testUser)).thenReturn("mock-access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("mock-refresh-token");
    when(jwtService.getExpirationSeconds()).thenReturn(86400L);

    var response = authService.authenticate(loginRequest);

    assertNotNull(response);
    assertEquals("mock-access-token", response.getToken());
    assertEquals("mock-refresh-token", response.getRefreshToken());
    assertEquals("Bearer", response.getTokenType());
    assertEquals(86400L, response.getExpiresIn());
    assertEquals(1L, response.getUserId());
    assertEquals(LoginResponse.UserTypeEnum.CUSTOMER, response.getUserType());

    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
    verify(jwtService).generateToken(testUser);
    verify(jwtService).generateRefreshToken(testUser);
  }

  @Test
  void authenticateWithInvalidEmailShouldThrowBadCredentialsException() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    var exception =
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).findByEmail("test@example.com");
    verifyNoInteractions(passwordEncoder);
    verifyNoInteractions(jwtService);
  }

  @Test
  void authenticateWithInvalidPasswordShouldThrowBadCredentialsException() {
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(false);

    var exception =
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));

    assertEquals("Invalid email or password", exception.getMessage());
    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
    verifyNoInteractions(jwtService);
  }

  @Test
  void authenticateWithInactiveUserShouldThrowBadCredentialsException() {
    testUser.setStatus(User.UserStatus.INACTIVE);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);

    var exception =
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));

    assertEquals("Account is not active", exception.getMessage());
    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
    verifyNoInteractions(jwtService);
  }

  @Test
  void authenticateWithSuspendedUserShouldThrowBadCredentialsException() {
    testUser.setStatus(User.UserStatus.SUSPENDED);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(passwordEncoder.matches("password123", testUser.getPasswordHash())).thenReturn(true);

    var exception =
        assertThrows(BadCredentialsException.class, () -> authService.authenticate(loginRequest));

    assertEquals("Account is not active", exception.getMessage());
    verify(userRepository).findByEmail("test@example.com");
    verify(passwordEncoder).matches("password123", testUser.getPasswordHash());
    verifyNoInteractions(jwtService);
  }
}
