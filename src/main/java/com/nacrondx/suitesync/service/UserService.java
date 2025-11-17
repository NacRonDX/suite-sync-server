package com.nacrondx.suitesync.service;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.user.Address;
import com.nacrondx.suitesync.model.user.CreateUserRequest;
import com.nacrondx.suitesync.model.user.UserResponse;
import com.nacrondx.suitesync.model.user.UserStatus;
import com.nacrondx.suitesync.model.user.UserType;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
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
