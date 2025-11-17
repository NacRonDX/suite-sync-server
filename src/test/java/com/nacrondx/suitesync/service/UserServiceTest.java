package com.nacrondx.suitesync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.user.Address;
import com.nacrondx.suitesync.model.user.CreateUserRequest;
import com.nacrondx.suitesync.model.user.UserStatus;
import com.nacrondx.suitesync.model.user.UserType;
import com.nacrondx.suitesync.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  private CreateUserRequest createUserRequest;
  private User savedUser;

  @BeforeEach
  void setUp() {
    createUserRequest = new CreateUserRequest();
    createUserRequest.setEmail("john.doe@example.com");
    createUserRequest.setFirstName("John");
    createUserRequest.setLastName("Doe");
    createUserRequest.setPhoneNumber("+1234567890");
    createUserRequest.setUserType(UserType.CUSTOMER);
    createUserRequest.setPassword("SecurePass123!");

    var address = new Address();
    address.setStreet("123 Main Street");
    address.setCity("New York");
    address.setState("NY");
    address.setPostalCode("10001");
    address.setCountry("USA");
    createUserRequest.setAddress(address);

    savedUser =
        User.builder()
            .id(1L)
            .email("john.doe@example.com")
            .firstName("John")
            .lastName("Doe")
            .phoneNumber("+1234567890")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .street("123 Main Street")
            .city("New York")
            .state("NY")
            .postalCode("10001")
            .country("USA")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Test
  void createUserWithValidDataShouldReturnUserResponse() {
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    var response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("john.doe@example.com", response.getEmail());
    assertEquals("John", response.getFirstName());
    assertEquals("Doe", response.getLastName());
    assertEquals("+1234567890", response.getPhoneNumber());
    assertEquals(UserType.CUSTOMER, response.getUserType());
    assertEquals(UserStatus.INACTIVE, response.getStatus());
    assertNotNull(response.getAddress());
    assertEquals("123 Main Street", response.getAddress().getStreet());
    assertEquals("New York", response.getAddress().getCity());
    assertEquals("NY", response.getAddress().getState());
    assertEquals("10001", response.getAddress().getPostalCode());
    assertEquals("USA", response.getAddress().getCountry());
    assertNotNull(response.getCreatedAt());
    assertNotNull(response.getUpdatedAt());

    verify(userRepository).existsByEmail("john.doe@example.com");
    verify(passwordEncoder).encode("SecurePass123!");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void createUserWithExistingEmailShouldThrowException() {
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> userService.createUser(createUserRequest));

    assertEquals("User with email john.doe@example.com already exists", exception.getMessage());
    verify(userRepository).existsByEmail("john.doe@example.com");
  }

  @Test
  void createUserShouldHashPassword() {
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    userService.createUser(createUserRequest);

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    var capturedUser = userCaptor.getValue();
    assertEquals("$2a$10$hashedpassword", capturedUser.getPasswordHash());
    verify(passwordEncoder).encode("SecurePass123!");
  }

  @Test
  void createUserWithoutAddressShouldWork() {
    createUserRequest.setAddress(null);

    var userWithoutAddress =
        User.builder()
            .id(2L)
            .email("jane.doe@example.com")
            .firstName("Jane")
            .lastName("Doe")
            .phoneNumber("+1234567890")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.INACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
    when(userRepository.save(any(User.class))).thenReturn(userWithoutAddress);

    createUserRequest.setEmail("jane.doe@example.com");
    createUserRequest.setFirstName("Jane");
    createUserRequest.setUserType(UserType.STAFF);

    var response = userService.createUser(createUserRequest);

    assertNotNull(response);
    assertEquals(2L, response.getId());
    assertEquals("jane.doe@example.com", response.getEmail());
  }
}
