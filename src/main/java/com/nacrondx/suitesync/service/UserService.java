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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException(
          "User with email " + request.getEmail() + " already exists");
    }

    var user =
        User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .userType(User.UserType.valueOf(request.getUserType().name()))
            .status(User.UserStatus.INACTIVE) // New users start as INACTIVE until activated
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

    return mapToUserResponse(savedUser);
  }

  @Transactional(readOnly = true)
  public UserResponse getUserById(Long userId) {
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    return mapToUserResponse(user);
  }

  @Transactional
  public UserResponse updateUser(Long userId, UpdateUserRequest request) {
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
    return mapToUserResponse(updatedUser);
  }

  @Transactional
  public void deleteUser(Long userId) {
    if (!userRepository.existsById(userId)) {
      throw new ResourceNotFoundException("User not found with id: " + userId);
    }
    userRepository.deleteById(userId);
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
              .collect(Collectors.toList());
      userPage =
          new org.springframework.data.domain.PageImpl<>(
              filteredUsers, pageable, userPage.getTotalElements());
    } else {
      userPage = userRepository.findAll(pageable);
    }

    var response = new UserPageResponse();
    response.setContent(
        userPage.getContent().stream().map(this::mapToUserResponse).collect(Collectors.toList()));
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
      Address address = new Address();
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
