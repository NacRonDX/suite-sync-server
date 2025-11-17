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

  @Test
  void getUserByIdShouldReturnUserResponse() {
    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(savedUser));

    var response = userService.getUserById(1L);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("john.doe@example.com", response.getEmail());
    assertEquals("John", response.getFirstName());
    assertEquals("Doe", response.getLastName());
    verify(userRepository).findById(1L);
  }

  @Test
  void getUserByIdWithNonExistentIdShouldThrowException() {
    when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    var exception =
        assertThrows(
            com.nacrondx.suitesync.exception.ResourceNotFoundException.class,
            () -> userService.getUserById(999L));

    assertEquals("User not found with id: 999", exception.getMessage());
    verify(userRepository).findById(999L);
  }

  @Test
  void updateUserShouldUpdateAllFields() {
    var updateRequest = new com.nacrondx.suitesync.model.user.UpdateUserRequest();
    updateRequest.setFirstName("Jane");
    updateRequest.setLastName("Smith");
    updateRequest.setPhoneNumber("+9876543210");
    updateRequest.setUserType(UserType.STAFF);

    var address = new Address();
    address.setStreet("456 Oak Avenue");
    address.setCity("Boston");
    address.setState("MA");
    address.setPostalCode("02101");
    address.setCountry("USA");
    updateRequest.setAddress(address);

    var updatedUser =
        User.builder()
            .id(1L)
            .email("john.doe@example.com")
            .firstName("Jane")
            .lastName("Smith")
            .phoneNumber("+9876543210")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.INACTIVE)
            .street("456 Oak Avenue")
            .city("Boston")
            .state("MA")
            .postalCode("02101")
            .country("USA")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(savedUser));
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);

    var response = userService.updateUser(1L, updateRequest);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals("Jane", response.getFirstName());
    assertEquals("Smith", response.getLastName());
    assertEquals("+9876543210", response.getPhoneNumber());
    assertEquals(UserType.STAFF, response.getUserType());
    assertEquals("456 Oak Avenue", response.getAddress().getStreet());
    assertEquals("Boston", response.getAddress().getCity());
    verify(userRepository).findById(1L);
    verify(userRepository).save(any(User.class));
  }

  @Test
  void updateUserWithPartialDataShouldOnlyUpdateProvidedFields() {
    var updateRequest = new com.nacrondx.suitesync.model.user.UpdateUserRequest();
    updateRequest.setFirstName("Jane");

    when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(savedUser));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    userService.updateUser(1L, updateRequest);

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    var capturedUser = userCaptor.getValue();
    assertEquals("Jane", capturedUser.getFirstName());
    assertEquals("Doe", capturedUser.getLastName()); // Should remain unchanged
  }

  @Test
  void updateUserWithNonExistentIdShouldThrowException() {
    var updateRequest = new com.nacrondx.suitesync.model.user.UpdateUserRequest();
    updateRequest.setFirstName("Jane");

    when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

    var exception =
        assertThrows(
            com.nacrondx.suitesync.exception.ResourceNotFoundException.class,
            () -> userService.updateUser(999L, updateRequest));

    assertEquals("User not found with id: 999", exception.getMessage());
    verify(userRepository).findById(999L);
  }

  @Test
  void deleteUserShouldDeleteExistingUser() {
    when(userRepository.existsById(1L)).thenReturn(true);

    userService.deleteUser(1L);

    verify(userRepository).existsById(1L);
    verify(userRepository).deleteById(1L);
  }

  @Test
  void deleteUserWithNonExistentIdShouldThrowException() {
    when(userRepository.existsById(999L)).thenReturn(false);

    var exception =
        assertThrows(
            com.nacrondx.suitesync.exception.ResourceNotFoundException.class,
            () -> userService.deleteUser(999L));

    assertEquals("User not found with id: 999", exception.getMessage());
    verify(userRepository).existsById(999L);
  }

  @Test
  void getAllUsersShouldReturnPagedResults() {
    var user2 =
        User.builder()
            .id(2L)
            .email("jane.doe@example.com")
            .firstName("Jane")
            .lastName("Doe")
            .phoneNumber("+9876543210")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    var users = java.util.List.of(savedUser, user2);
    var page =
        new org.springframework.data.domain.PageImpl<>(
            users, org.springframework.data.domain.PageRequest.of(0, 20), 2);

    when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(page);

    var response = userService.getAllUsers(null, 0, 20);

    assertNotNull(response);
    assertEquals(2, response.getContent().size());
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());
    assertEquals(2L, response.getTotalElements());
    assertEquals(1, response.getTotalPages());
    verify(userRepository).findAll(any(org.springframework.data.domain.Pageable.class));
  }

  @Test
  void getAllUsersWithDefaultParametersShouldUseDefaults() {
    var page =
        new org.springframework.data.domain.PageImpl<>(
            java.util.List.of(savedUser), org.springframework.data.domain.PageRequest.of(0, 20), 1);

    when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(page);

    var response = userService.getAllUsers(null, null, null);

    assertNotNull(response);
    assertEquals(0, response.getPage());
    assertEquals(20, response.getSize());
    verify(userRepository).findAll(any(org.springframework.data.domain.Pageable.class));
  }

  @Test
  void getAllUsersFilteredByUserTypeShouldReturnFilteredResults() {
    var customerUser =
        User.builder()
            .id(1L)
            .email("customer@example.com")
            .firstName("Customer")
            .lastName("User")
            .phoneNumber("+1234567890")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    var staffUser =
        User.builder()
            .id(2L)
            .email("staff@example.com")
            .firstName("Staff")
            .lastName("User")
            .phoneNumber("+9876543210")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    var users = java.util.List.of(customerUser, staffUser);
    var page =
        new org.springframework.data.domain.PageImpl<>(
            users, org.springframework.data.domain.PageRequest.of(0, 20), 2);

    when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(page);

    var response = userService.getAllUsers(UserType.CUSTOMER, 0, 20);

    assertNotNull(response);
    assertEquals(1, response.getContent().size());
    assertEquals(UserType.CUSTOMER, response.getContent().get(0).getUserType());
    verify(userRepository).findAll(any(org.springframework.data.domain.Pageable.class));
  }
}
