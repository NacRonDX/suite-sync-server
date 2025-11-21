package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.exception.ResourceNotFoundException;
import com.nacrondx.suitesync.model.user.Address;
import com.nacrondx.suitesync.model.user.CreateUserRequest;
import com.nacrondx.suitesync.model.user.UpdateUserRequest;
import com.nacrondx.suitesync.model.user.UserPageResponse;
import com.nacrondx.suitesync.model.user.UserResponse;
import com.nacrondx.suitesync.model.user.UserStatus;
import com.nacrondx.suitesync.model.user.UserType;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    log.info("Creating user with email: {}", request.getEmail());
    if (userRepository.existsByEmail(request.getEmail())) {
      log.warn("User with email: {} already exists", request.getEmail());
      throw new IllegalArgumentException(
          "User with email " + request.getEmail() + " already exists");
    }

    var confirmationToken = UUID.randomUUID().toString();
    var tokenExpiry = LocalDateTime.now().plusHours(24);

    var user =
        User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .confirmationToken(confirmationToken)
            .confirmationTokenExpiry(tokenExpiry)
            .build();

    if (request.getAddress() != null) {
      var address = request.getAddress();
      user.setStreet(address.getStreet());
      user.setCity(address.getCity());
      user.setState(address.getState());
      user.setPostalCode(address.getPostalCode());
      user.setCountry(address.getCountry());
    }

    var savedUser = userRepository.save(user);
    log.info("Successfully created user with ID: {}", savedUser.getId());

    log.info("Sending confirmation email to: {}", savedUser.getEmail());
    emailService.sendConfirmationEmail(
        savedUser.getEmail(), savedUser.getFirstName(), savedUser.getId(), confirmationToken);

    return mapToUserResponse(savedUser);
  }

  @Transactional
  public UserResponse activateUser(Long userId, String token) {
    log.info("Fetching user with ID: {}", userId);
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    if (user.getStatus() == User.UserStatus.ACTIVE) {
      log.warn("User with ID: {} is already active", userId);
      throw new IllegalStateException("User account is already active");
    }

    if (user.getConfirmationToken() == null || !user.getConfirmationToken().equals(token)) {
      log.warn("Invalid confirmation token for user ID: {}", userId);
      throw new IllegalArgumentException("Invalid confirmation token");
    }

    if (user.getConfirmationTokenExpiry() == null
        || user.getConfirmationTokenExpiry().isBefore(LocalDateTime.now())) {
      log.warn("Confirmation token expired for user ID: {}", userId);
      throw new IllegalArgumentException("Confirmation token has expired");
    }

    user.setStatus(User.UserStatus.ACTIVE);
    user.setConfirmationToken(null);
    user.setConfirmationTokenExpiry(null);

    var activatedUser = userRepository.save(user);
    log.info("Successfully activated user with ID: {}", userId);
    return mapToUserResponse(activatedUser);
  }

  @Transactional(readOnly = true)
  public UserResponse getUserById(Long userId) {
    log.info("Fetching user with ID: {}", userId);
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    return mapToUserResponse(user);
  }

  @Transactional
  public UserResponse updateUser(Long userId, UpdateUserRequest request) {
    log.info("Updating user with ID: {}", userId);
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    if (request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }
    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }
    if (request.getUserType() != null) {
      user.setUserType(User.UserType.valueOf(request.getUserType().name()));
    }

    if (request.getAddress() != null) {
      var address = request.getAddress();
      user.setStreet(address.getStreet());
      user.setCity(address.getCity());
      user.setState(address.getState());
      user.setPostalCode(address.getPostalCode());
      user.setCountry(address.getCountry());
    }

    var updatedUser = userRepository.save(user);
    log.info("Successfully updated user with ID: {}", userId);
    return mapToUserResponse(updatedUser);
  }

  @Transactional
  public void deleteUser(Long userId) {
    log.info("Deleting user with ID: {}", userId);
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User not found with id: " + userId);
    }
    userRepository.deleteById(userId);
    log.info("Successfully deleted user with ID: {}", userId);
  }

  @Transactional(readOnly = true)
  public UserPageResponse getAllUsers(UserType userType, Integer page, Integer size) {
    int pageNumber = page != null ? page : 0;
    int pageSize = size != null ? size : 20;

    var pageable = PageRequest.of(pageNumber, pageSize);
    Page<User> userPage;

    if (userType != null) {
      var entityUserType = User.UserType.valueOf(userType.name());
      userPage = userRepository.findAll(pageable);
      var filteredUsers =
          userPage.getContent().stream()
              .filter(user -> user.getUserType() == entityUserType)
              .toList();
      userPage =
          new org.springframework.data.domain.PageImpl<>(
              filteredUsers, pageable, userPage.getTotalElements());
    } else {
      userPage = userRepository.findAll(pageable);
    }

    var response = new UserPageResponse();
    response.setContent(userPage.getContent().stream().map(this::mapToUserResponse).toList());
    response.setPage(userPage.getNumber());
    response.setSize(userPage.getSize());
    response.setTotalElements(userPage.getTotalElements());
    response.setTotalPages(userPage.getTotalPages());

    return response;
  }

  private UserResponse mapToUserResponse(User user) {
    var response = new UserResponse();
    response.setId(user.getId());
    response.setEmail(user.getEmail());
    response.setFirstName(user.getFirstName());
    response.setLastName(user.getLastName());
    response.setPhoneNumber(user.getPhoneNumber());
    response.setUserType(UserType.fromValue(user.getUserType().name()));
    response.setStatus(UserStatus.fromValue(user.getStatus().name()));

    if (user.getStreet() != null
        || user.getCity() != null
        || user.getState() != null
        || user.getPostalCode() != null
        || user.getCountry() != null) {
      var address = new Address();
      address.setStreet(user.getStreet());
      address.setCity(user.getCity());
      address.setState(user.getState());
      address.setPostalCode(user.getPostalCode());
      address.setCountry(user.getCountry());
      response.setAddress(address);
    }

    response.setCreatedAt(OffsetDateTime.of(user.getCreatedAt(), ZoneOffset.UTC));
    response.setUpdatedAt(OffsetDateTime.of(user.getUpdatedAt(), ZoneOffset.UTC));

    return response;
  }
}
