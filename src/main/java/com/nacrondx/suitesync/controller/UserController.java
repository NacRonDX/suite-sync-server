package com.nacrondx.suitesync.controller;

import com.nacrondx.suitesync.api.UsersApi;
import com.nacrondx.suitesync.model.user.CreateUserRequest;
import com.nacrondx.suitesync.model.user.UpdateUserRequest;
import com.nacrondx.suitesync.model.user.UserPageResponse;
import com.nacrondx.suitesync.model.user.UserResponse;
import com.nacrondx.suitesync.model.user.UserType;
import com.nacrondx.suitesync.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController implements UsersApi {
  private final UserService userService;

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.createUser(createUserRequest));
  }

  @Override
  public ResponseEntity<UserResponse> activateUser(Long userId, String token) {
    log.info("Received activation request for userId: {}", userId);
    return ResponseEntity.ok(userService.activateUser(userId, token));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserResponse> getUserById(Long userId) {
    return ResponseEntity.ok(userService.getUserById(userId));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserResponse> updateUser(Long userId, UpdateUserRequest updateUserRequest) {
    return ResponseEntity.ok(userService.updateUser(userId, updateUserRequest));
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> deleteUser(Long userId) {
    userService.deleteUser(userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserPageResponse> getAllUsers(
      UserType userType, Integer page, Integer size) {
    return ResponseEntity.ok(userService.getAllUsers(userType, page, size));
  }
}
