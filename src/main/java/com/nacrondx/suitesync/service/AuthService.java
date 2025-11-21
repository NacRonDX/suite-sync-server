package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.auth.ChangePasswordRequest;
import com.nacrondx.suitesync.model.auth.ForgotPasswordRequest;
import com.nacrondx.suitesync.model.auth.LoginRequest;
import com.nacrondx.suitesync.model.auth.LoginResponse;
import com.nacrondx.suitesync.model.auth.RefreshTokenRequest;
import com.nacrondx.suitesync.model.auth.ResetPasswordRequest;
import com.nacrondx.suitesync.model.auth.TokenValidationResponse;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtDecoder jwtDecoder;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      JwtDecoder jwtDecoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.jwtDecoder = jwtDecoder;
  }

  public LoginResponse authenticate(LoginRequest loginRequest) {
    log.info("Authenticating user with email: {}", loginRequest.getEmail());
    var user =
        userRepository
            .findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
      log.warn("Authentication failed for email: {}", loginRequest.getEmail());
      throw new BadCredentialsException("Invalid email or password");
    }

    if (user.getStatus() != User.UserStatus.ACTIVE) {
      log.warn("Authentication attempt for inactive account: {}", loginRequest.getEmail());
      throw new BadCredentialsException("Account is not active");
    }

    var accessToken = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);

    var response = new LoginResponse();
    response.setToken(accessToken);
    response.setRefreshToken(refreshToken);
    response.setTokenType("Bearer");
    response.setExpiresIn(jwtService.getExpirationSeconds());
    response.setUserId(user.getId());
    response.setUserType(LoginResponse.UserTypeEnum.fromValue(user.getUserType().name()));
    log.info("User {} authenticated successfully", loginRequest.getEmail());

    return response;
  }

  public void changePassword(ChangePasswordRequest request) {}

  public TokenValidationResponse validateToken(Authentication authentication) {
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new BadCredentialsException("Invalid authentication token");
    }

    var jwt = jwtAuth.getToken();
    var email = jwt.getSubject();
    var userId = jwt.getClaim("userId");
    var userType = jwt.getClaim("userType");

    var user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

    if (user.getStatus() != User.UserStatus.ACTIVE) {
      throw new BadCredentialsException("User account is not active");
    }

    var response = new TokenValidationResponse();
    response.setValid(true);
    response.setUserId(userId instanceof Number ? ((Number) userId).longValue() : null);
    response.setEmail(email);
    response.setUserType(TokenValidationResponse.UserTypeEnum.fromValue(userType.toString()));
    response.setExpiresAt(
        jwt.getExpiresAt() != null
            ? OffsetDateTime.ofInstant(jwt.getExpiresAt(), ZoneOffset.UTC)
            : null);

    return response;
  }

  public LoginResponse refreshToken(RefreshTokenRequest request) {
    try {
      var jwt = jwtDecoder.decode(request.getRefreshToken());

      var tokenType = jwt.getClaim("type");
      if (!"refresh".equals(tokenType)) {
        throw new BadCredentialsException("Invalid token type");
      }

      var email = jwt.getSubject();

      var user =
          userRepository
              .findByEmail(email)
              .orElseThrow(() -> new BadCredentialsException("User not found"));

      if (user.getStatus() != User.UserStatus.ACTIVE) {
        throw new BadCredentialsException("User account is not active");
      }

      var accessToken = jwtService.generateToken(user);
      var newRefreshToken = jwtService.generateRefreshToken(user);

      var response = new LoginResponse();
      response.setToken(accessToken);
      response.setRefreshToken(newRefreshToken);
      response.setTokenType("Bearer");
      response.setExpiresIn(jwtService.getExpirationSeconds());
      response.setUserId(user.getId());
      response.setUserType(LoginResponse.UserTypeEnum.fromValue(user.getUserType().name()));
      response.setEmail(user.getEmail());

      return response;
    } catch (JwtException e) {
      throw new BadCredentialsException("Invalid or expired refresh token", e);
    }
  }

  public void forgotPassword(ForgotPasswordRequest request) {}

  public void resetPassword(ResetPasswordRequest request) {}
}
