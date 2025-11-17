package com.nacrondx.suitesync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.auth.LoginRequest;
import com.nacrondx.suitesync.model.auth.LoginResponse;
import com.nacrondx.suitesync.model.auth.RefreshTokenRequest;
import com.nacrondx.suitesync.model.auth.TokenValidationResponse;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private JwtDecoder jwtDecoder;

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

  @Test
  void validateTokenWithValidJwtShouldReturnTokenValidationResponse() {
    var jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("userType", "CUSTOMER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    var authentication = new JwtAuthenticationToken(jwt);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    var response = authService.validateToken(authentication);

    assertNotNull(response);
    assertTrue(response.getValid());
    assertEquals(1L, response.getUserId());
    assertEquals("test@example.com", response.getEmail());
    assertEquals(TokenValidationResponse.UserTypeEnum.CUSTOMER, response.getUserType());
    assertNotNull(response.getExpiresAt());

    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  void validateTokenWithInvalidAuthenticationTypeShouldThrowBadCredentialsException() {
    var authentication = new UsernamePasswordAuthenticationToken("test@example.com", "password");

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.validateToken(authentication));

    assertEquals("Invalid authentication token", exception.getMessage());
    verifyNoInteractions(userRepository);
  }

  @Test
  void validateTokenWithNonExistentUserShouldThrowBadCredentialsException() {
    var jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("nonexistent@example.com")
            .claim("userId", 999L)
            .claim("userType", "CUSTOMER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    var authentication = new JwtAuthenticationToken(jwt);

    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.validateToken(authentication));

    assertEquals("User not found", exception.getMessage());
    verify(userRepository).findByEmail("nonexistent@example.com");
  }

  @Test
  void validateTokenWithInactiveUserShouldThrowBadCredentialsException() {
    testUser.setStatus(User.UserStatus.INACTIVE);

    var jwt =
        Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("userType", "CUSTOMER")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    var authentication = new JwtAuthenticationToken(jwt);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.validateToken(authentication));

    assertEquals("User account is not active", exception.getMessage());
    verify(userRepository).findByEmail("test@example.com");
  }

  @Test
  void refreshTokenWithValidRefreshTokenShouldReturnNewTokens() {
    var refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");

    var jwt =
        Jwt.withTokenValue("valid-refresh-token")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("type", "refresh")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(604800))
            .build();

    when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    when(jwtService.generateToken(testUser)).thenReturn("new-access-token");
    when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");
    when(jwtService.getExpirationSeconds()).thenReturn(86400L);

    var response = authService.refreshToken(refreshTokenRequest);

    assertNotNull(response);
    assertEquals("new-access-token", response.getToken());
    assertEquals("new-refresh-token", response.getRefreshToken());
    assertEquals("Bearer", response.getTokenType());
    assertEquals(86400L, response.getExpiresIn());
    assertEquals(1L, response.getUserId());
    assertEquals(LoginResponse.UserTypeEnum.CUSTOMER, response.getUserType());
    assertEquals("test@example.com", response.getEmail());

    verify(jwtDecoder).decode("valid-refresh-token");
    verify(userRepository).findByEmail("test@example.com");
    verify(jwtService).generateToken(testUser);
    verify(jwtService).generateRefreshToken(testUser);
  }

  @Test
  void refreshTokenWithInvalidTokenTypeShouldThrowBadCredentialsException() {
    var refreshTokenRequest = new RefreshTokenRequest("access-token-not-refresh");

    var jwt =
        Jwt.withTokenValue("access-token-not-refresh")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("type", "access")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

    when(jwtDecoder.decode("access-token-not-refresh")).thenReturn(jwt);

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.refreshToken(refreshTokenRequest));

    assertEquals("Invalid token type", exception.getMessage());
    verify(jwtDecoder).decode("access-token-not-refresh");
    verifyNoInteractions(userRepository);
    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshTokenWithExpiredTokenShouldThrowBadCredentialsException() {
    var refreshTokenRequest = new RefreshTokenRequest("expired-refresh-token");

    when(jwtDecoder.decode("expired-refresh-token")).thenThrow(new JwtException("Token expired"));

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.refreshToken(refreshTokenRequest));

    assertEquals("Invalid or expired refresh token", exception.getMessage());
    verify(jwtDecoder).decode("expired-refresh-token");
    verifyNoInteractions(userRepository);
    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshTokenWithNonExistentUserShouldThrowBadCredentialsException() {
    var refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");

    var jwt =
        Jwt.withTokenValue("valid-refresh-token")
            .header("alg", "RS256")
            .subject("nonexistent@example.com")
            .claim("userId", 999L)
            .claim("type", "refresh")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(604800))
            .build();

    when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
    when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.refreshToken(refreshTokenRequest));

    assertEquals("User not found", exception.getMessage());
    verify(jwtDecoder).decode("valid-refresh-token");
    verify(userRepository).findByEmail("nonexistent@example.com");
    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshTokenWithInactiveUserShouldThrowBadCredentialsException() {
    testUser.setStatus(User.UserStatus.INACTIVE);

    var refreshTokenRequest = new RefreshTokenRequest("valid-refresh-token");

    var jwt =
        Jwt.withTokenValue("valid-refresh-token")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .claim("type", "refresh")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(604800))
            .build();

    when(jwtDecoder.decode("valid-refresh-token")).thenReturn(jwt);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.refreshToken(refreshTokenRequest));

    assertEquals("User account is not active", exception.getMessage());
    verify(jwtDecoder).decode("valid-refresh-token");
    verify(userRepository).findByEmail("test@example.com");
    verifyNoInteractions(jwtService);
  }

  @Test
  void refreshTokenWithMissingTypeClaimShouldThrowBadCredentialsException() {
    var refreshTokenRequest = new RefreshTokenRequest("token-without-type");

    var jwt =
        Jwt.withTokenValue("token-without-type")
            .header("alg", "RS256")
            .subject("test@example.com")
            .claim("userId", 1L)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(604800))
            .build();

    when(jwtDecoder.decode("token-without-type")).thenReturn(jwt);

    var exception =
        assertThrows(
            BadCredentialsException.class, () -> authService.refreshToken(refreshTokenRequest));

    assertEquals("Invalid token type", exception.getMessage());
    verify(jwtDecoder).decode("token-without-type");
    verifyNoInteractions(userRepository);
    verifyNoInteractions(jwtService);
  }
}
