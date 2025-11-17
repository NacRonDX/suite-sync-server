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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public LoginResponse authenticate(LoginRequest loginRequest) {
    var user =
        userRepository
            .findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }

    if (user.getStatus() != User.UserStatus.ACTIVE) {
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

    return response;
  }

  public void changePassword(ChangePasswordRequest request) {}

  public void logout() {}

  public TokenValidationResponse validateToken(Authentication authentication) {
    return new TokenValidationResponse();
  }

  public LoginResponse refreshToken(RefreshTokenRequest request) {
    return new LoginResponse();
  }

  public void forgotPassword(ForgotPasswordRequest request) {}

  public void resetPassword(ResetPasswordRequest request) {}
}
