package com.nacrondx.suitesync.controller;

import com.nacrondx.suitesync.api.AuthenticationApi;
import com.nacrondx.suitesync.model.auth.ChangePasswordRequest;
import com.nacrondx.suitesync.model.auth.ForgotPasswordRequest;
import com.nacrondx.suitesync.model.auth.LoginRequest;
import com.nacrondx.suitesync.model.auth.LoginResponse;
import com.nacrondx.suitesync.model.auth.RefreshTokenRequest;
import com.nacrondx.suitesync.model.auth.ResetPasswordRequest;
import com.nacrondx.suitesync.model.auth.SuccessResponse;
import com.nacrondx.suitesync.model.auth.TokenValidationResponse;
import com.nacrondx.suitesync.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthenticationApi {
  private final AuthService authService;

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    return ResponseEntity.ok(authService.authenticate(loginRequest));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SuccessResponse> changePassword(ChangePasswordRequest request) {
    authService.changePassword(request);
    return ResponseEntity.ok(
        new SuccessResponse().success(true).message("Password changed successfully"));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> logout() {
    authService.logout();
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<TokenValidationResponse> validateToken() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return ResponseEntity.ok(authService.validateToken(authentication));
  }

  @Override
  public ResponseEntity<LoginResponse> refreshToken(RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refreshToken(request));
  }

  @Override
  public ResponseEntity<SuccessResponse> forgotPassword(ForgotPasswordRequest request) {
    authService.forgotPassword(request);
    return ResponseEntity.ok(
        new SuccessResponse().success(true).message("Password reset email sent"));
  }

  @Override
  public ResponseEntity<SuccessResponse> resetPassword(ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(
        new SuccessResponse().success(true).message("Password reset successful"));
  }
}
