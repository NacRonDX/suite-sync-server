package com.nacrondx.suitesync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nacrondx.suitesync.model.auth.LoginRequest;
import com.nacrondx.suitesync.model.auth.LoginResponse;
import com.nacrondx.suitesync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AdminUserAuthenticationIntegrationTest {
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void adminUserShouldAuthenticateWithCorrectCredentials() {
    var adminUser = userRepository.findByEmail("admin@suitesync.com");
    assertNotNull(adminUser.orElse(null), "Admin user should exist in database");

    var expectedPassword = "Admin123!@#";
    boolean passwordMatches =
        passwordEncoder.matches(expectedPassword, adminUser.get().getPasswordHash());
    assertTrue(
        passwordMatches, "Password 'Admin123!@#' should match the stored hash for admin user");

    var loginRequest = new LoginRequest("admin@suitesync.com", "Admin123!@#");
    var response = authService.authenticate(loginRequest);

    assertNotNull(response, "Login response should not be null");
    assertNotNull(response.getToken(), "Access token should not be null");
    assertNotNull(response.getRefreshToken(), "Refresh token should not be null");
    assertEquals("Bearer", response.getTokenType(), "Token type should be Bearer");
    assertEquals(
        LoginResponse.UserTypeEnum.ADMIN, response.getUserType(), "User type should be ADMIN");
    assertNotNull(response.getUserId(), "User ID should not be null");
    assertNotNull(response.getExpiresIn(), "Expiration time should not be null");
  }
}
