package com.nacrondx.suitesync.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacrondx.suitesync.entity.User;
import com.nacrondx.suitesync.model.user.Address;
import com.nacrondx.suitesync.model.user.CreateUserRequest;
import com.nacrondx.suitesync.model.user.UpdateUserRequest;
import com.nacrondx.suitesync.model.user.UserType;
import com.nacrondx.suitesync.repository.UserRepository;
import com.nacrondx.suitesync.service.EmailService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @MockBean private EmailService emailService;

  private User testUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    testUser =
        User.builder()
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .phoneNumber("+1234567890")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.ACTIVE)
            .street("123 Test St")
            .city("Test City")
            .state("TS")
            .postalCode("12345")
            .country("Test Country")
            .build();

    testUser = userRepository.save(testUser);
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  @Test
  void createUserShouldReturnCreatedUser() throws Exception {
    var createRequest = new CreateUserRequest();
    createRequest.setEmail("newuser@example.com");
    createRequest.setFirstName("New");
    createRequest.setLastName("User");
    createRequest.setPhoneNumber("+9876543210");
    createRequest.setUserType(UserType.CUSTOMER);
    createRequest.setPassword("SecurePass123!");

    var address = new Address();
    address.setStreet("456 New St");
    address.setCity("New City");
    address.setState("NC");
    address.setPostalCode("54321");
    address.setCountry("USA");
    createRequest.setAddress(address);

    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email", is("newuser@example.com")))
        .andExpect(jsonPath("$.firstName", is("New")))
        .andExpect(jsonPath("$.lastName", is("User")))
        .andExpect(jsonPath("$.phoneNumber", is("+9876543210")))
        .andExpect(jsonPath("$.userType", is("CUSTOMER")))
        .andExpect(jsonPath("$.status", is("INACTIVE")))
        .andExpect(jsonPath("$.address.street", is("456 New St")))
        .andExpect(jsonPath("$.address.city", is("New City")))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.updatedAt").exists());
  }

  @Test
  void createUserWithDuplicateEmailShouldReturnConflict() throws Exception {
    var createRequest = new CreateUserRequest();
    createRequest.setEmail("test@example.com");
    createRequest.setFirstName("Duplicate");
    createRequest.setLastName("User");
    createRequest.setPhoneNumber("+1111111111");
    createRequest.setUserType(UserType.CUSTOMER);
    createRequest.setPassword("SecurePass123!");

    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void getUserByIdShouldReturnUser() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users/" + testUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("userId", testUser.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
        .andExpect(jsonPath("$.email", is("test@example.com")))
        .andExpect(jsonPath("$.firstName", is("Test")))
        .andExpect(jsonPath("$.lastName", is("User")))
        .andExpect(jsonPath("$.phoneNumber", is("+1234567890")))
        .andExpect(jsonPath("$.userType", is("CUSTOMER")))
        .andExpect(jsonPath("$.status", is("ACTIVE")))
        .andExpect(jsonPath("$.address.street", is("123 Test St")))
        .andExpect(jsonPath("$.address.city", is("Test City")));
  }

  @Test
  void getUserByIdWithoutAuthShouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users/" + testUser.getId())).andExpect(status().isUnauthorized());
  }

  @Test
  void getUserByIdWithNonExistentIdShouldReturnNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/users/999999").with(jwt().jwt(jwt -> jwt.claim("userId", 999999L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void getUserByIdWithMismatchedUserIdShouldReturnForbidden() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/users/" + testUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("userId", 999L))))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateUserShouldReturnUpdatedUser() throws Exception {
    var updateRequest = new UpdateUserRequest();
    updateRequest.setFirstName("Updated");
    updateRequest.setLastName("Name");
    updateRequest.setPhoneNumber("+9999999999");
    updateRequest.setUserType(UserType.STAFF);

    var address = new Address();
    address.setStreet("789 Updated St");
    address.setCity("Updated City");
    address.setState("UC");
    address.setPostalCode("99999");
    address.setCountry("Updated Country");
    updateRequest.setAddress(address);

    mockMvc
        .perform(
            put("/api/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(jwt().jwt(jwt -> jwt.claim("userId", testUser.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(testUser.getId().intValue())))
        .andExpect(jsonPath("$.email", is("test@example.com")))
        .andExpect(jsonPath("$.firstName", is("Updated")))
        .andExpect(jsonPath("$.lastName", is("Name")))
        .andExpect(jsonPath("$.phoneNumber", is("+9999999999")))
        .andExpect(jsonPath("$.userType", is("STAFF")))
        .andExpect(jsonPath("$.address.street", is("789 Updated St")))
        .andExpect(jsonPath("$.address.city", is("Updated City")));
  }

  @Test
  void updateUserWithPartialDataShouldOnlyUpdateProvidedFields() throws Exception {
    var updateRequest = new UpdateUserRequest();
    updateRequest.setFirstName("OnlyFirstName");

    mockMvc
        .perform(
            put("/api/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(jwt().jwt(jwt -> jwt.claim("userId", testUser.getId()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName", is("OnlyFirstName")))
        .andExpect(jsonPath("$.lastName", is("User")))
        .andExpect(jsonPath("$.phoneNumber", is("+1234567890")));
  }

  @Test
  void updateUserWithoutAuthShouldReturnUnauthorized() throws Exception {
    var updateRequest = new UpdateUserRequest();
    updateRequest.setFirstName("Updated");

    mockMvc
        .perform(
            put("/api/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updateUserWithNonExistentIdShouldReturnNotFound() throws Exception {
    var updateRequest = new UpdateUserRequest();
    updateRequest.setFirstName("Updated");

    mockMvc
        .perform(
            put("/api/v1/users/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(jwt().jwt(jwt -> jwt.claim("userId", 999999L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void updateUserWithMismatchedUserIdShouldReturnForbidden() throws Exception {
    var updateRequest = new UpdateUserRequest();
    updateRequest.setFirstName("Updated");

    mockMvc
        .perform(
            put("/api/v1/users/" + testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(jwt().jwt(jwt -> jwt.claim("userId", 999L))))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteUserShouldReturnNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/users/" + testUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("userId", testUser.getId()))))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            get("/api/v1/users/" + testUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("userId", testUser.getId()))))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteUserWithoutAuthShouldReturnUnauthorized() throws Exception {
    mockMvc
        .perform(delete("/api/v1/users/" + testUser.getId()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deleteUserWithNonExistentIdShouldReturnNotFound() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/users/999999").with(jwt().jwt(jwt -> jwt.claim("userId", 999999L))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void deleteUserWithMismatchedUserIdShouldReturnForbidden() throws Exception {
    mockMvc
        .perform(
            delete("/api/v1/users/" + testUser.getId())
                .with(jwt().jwt(jwt -> jwt.claim("userId", 999L))))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllUsersShouldReturnPagedUsers() throws Exception {
    var user2 =
        User.builder()
            .email("user2@example.com")
            .firstName("User")
            .lastName("Two")
            .phoneNumber("+2222222222")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.ACTIVE)
            .build();

    var user3 =
        User.builder()
            .email("user3@example.com")
            .firstName("User")
            .lastName("Three")
            .phoneNumber("+3333333333")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.ADMIN)
            .status(User.UserStatus.ACTIVE)
            .build();

    userRepository.save(user2);
    userRepository.save(user3);

    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.page", is(0)))
        .andExpect(jsonPath("$.size", is(20)))
        .andExpect(jsonPath("$.totalElements", is(3)))
        .andExpect(jsonPath("$.totalPages", is(1)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllUsersWithPaginationShouldReturnCorrectPage() throws Exception {
    for (int i = 1; i <= 25; i++) {
      var user =
          User.builder()
              .email("user" + i + "@example.com")
              .firstName("User")
              .lastName("Number" + i)
              .phoneNumber("+123456789" + i)
              .passwordHash("$2a$10$hashedpassword")
              .userType(User.UserType.CUSTOMER)
              .status(User.UserStatus.ACTIVE)
              .build();
      userRepository.save(user);
    }

    mockMvc
        .perform(get("/api/v1/users?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.page", is(0)))
        .andExpect(jsonPath("$.size", is(10)))
        .andExpect(jsonPath("$.totalElements", is(26)))
        .andExpect(jsonPath("$.totalPages", is(3)));

    mockMvc
        .perform(get("/api/v1/users?page=1&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.page", is(1)))
        .andExpect(jsonPath("$.size", is(10)));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAllUsersFilteredByUserTypeShouldReturnFilteredResults() throws Exception {
    var staffUser =
        User.builder()
            .email("staff@example.com")
            .firstName("Staff")
            .lastName("User")
            .phoneNumber("+1111111111")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.STAFF)
            .status(User.UserStatus.ACTIVE)
            .build();

    var adminUser =
        User.builder()
            .email("admin@example.com")
            .firstName("Admin")
            .lastName("User")
            .phoneNumber("+2222222222")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.ADMIN)
            .status(User.UserStatus.ACTIVE)
            .build();

    userRepository.save(staffUser);
    userRepository.save(adminUser);

    mockMvc
        .perform(get("/api/v1/users?userType=CUSTOMER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].userType", is("CUSTOMER")));

    mockMvc
        .perform(get("/api/v1/users?userType=STAFF"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].userType", is("STAFF")));
  }

  @Test
  void getAllUsersWithoutAuthShouldReturnUnauthorized() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "CUSTOMER")
  void getAllUsersWithoutAdminRoleShouldReturnForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
  }

  @Test
  void activateUserWithValidTokenShouldActivateAccount() throws Exception {
    var inactiveUser =
        User.builder()
            .email("inactive@example.com")
            .firstName("Inactive")
            .lastName("User")
            .phoneNumber("+5555555555")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .confirmationToken("valid-token-123")
            .confirmationTokenExpiry(LocalDateTime.now().plusHours(24))
            .build();

    inactiveUser = userRepository.save(inactiveUser);

    mockMvc
        .perform(post("/api/v1/users/" + inactiveUser.getId() + "/activate?token=valid-token-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(inactiveUser.getId().intValue())))
        .andExpect(jsonPath("$.email", is("inactive@example.com")))
        .andExpect(jsonPath("$.status", is("ACTIVE")));

    var activatedUser = userRepository.findById(inactiveUser.getId()).orElseThrow();
    assert activatedUser.getStatus() == User.UserStatus.ACTIVE;
    assert activatedUser.getConfirmationToken() == null;
    assert activatedUser.getConfirmationTokenExpiry() == null;
  }

  @Test
  void activateUserWithInvalidTokenShouldReturnBadRequest() throws Exception {
    var inactiveUser =
        User.builder()
            .email("inactive2@example.com")
            .firstName("Inactive")
            .lastName("User")
            .phoneNumber("+5555555556")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .confirmationToken("valid-token-456")
            .confirmationTokenExpiry(LocalDateTime.now().plusHours(24))
            .build();

    inactiveUser = userRepository.save(inactiveUser);

    mockMvc
        .perform(post("/api/v1/users/" + inactiveUser.getId() + "/activate?token=wrong-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void activateUserWithExpiredTokenShouldReturnBadRequest() throws Exception {
    var inactiveUser =
        User.builder()
            .email("inactive3@example.com")
            .firstName("Inactive")
            .lastName("User")
            .phoneNumber("+5555555557")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .confirmationToken("expired-token-789")
            .confirmationTokenExpiry(LocalDateTime.now().minusHours(1))
            .build();

    inactiveUser = userRepository.save(inactiveUser);

    mockMvc
        .perform(
            post("/api/v1/users/" + inactiveUser.getId() + "/activate?token=expired-token-789"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void activateAlreadyActiveUserShouldReturnBadRequest() throws Exception {
    mockMvc
        .perform(post("/api/v1/users/" + testUser.getId() + "/activate?token=any-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void activateUserWithNonExistentIdShouldReturnNotFound() throws Exception {
    mockMvc
        .perform(post("/api/v1/users/999999/activate?token=any-token"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void activateUserEndpointShouldNotRequireAuthentication() throws Exception {
    var inactiveUser =
        User.builder()
            .email("noauth@example.com")
            .firstName("NoAuth")
            .lastName("User")
            .phoneNumber("+5555555558")
            .passwordHash("$2a$10$hashedpassword")
            .userType(User.UserType.CUSTOMER)
            .status(User.UserStatus.INACTIVE)
            .confirmationToken("public-token-123")
            .confirmationTokenExpiry(LocalDateTime.now().plusHours(24))
            .build();

    inactiveUser = userRepository.save(inactiveUser);

    mockMvc
        .perform(post("/api/v1/users/" + inactiveUser.getId() + "/activate?token=public-token-123"))
        .andExpect(status().isOk());
  }
}
