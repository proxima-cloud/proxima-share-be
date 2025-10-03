package com.proximashare.service;

import com.proximashare.dto.RegistrationRequest;
import com.proximashare.entity.Role;
import com.proximashare.entity.User;
import com.proximashare.repository.RoleRepository;
import com.proximashare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");
    }

    @Nested
    @DisplayName("registerUser() Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("Should successfully register a new user with single role")
        void shouldRegisterNewUserWithSingleRole() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

            // Act
            User result = authService.registerUser(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getPassword()).isEqualTo("$2a$10$encodedpassword");
            assertThat(result.getRoles()).hasSize(1);
            assertThat(result.getRoles()).contains(userRole);

            verify(userRepository).existsByUsername("newuser");
            verify(roleRepository).findByName("USER");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should successfully register a new user with multiple roles")
        void shouldRegisterNewUserWithMultipleRoles() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("adminuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER", "ADMIN"));

            when(userRepository.existsByUsername("adminuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedpassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // Act
            User result = authService.registerUser(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getUsername()).isEqualTo("adminuser");
            assertThat(result.getRoles()).hasSize(2);
            assertThat(result.getRoles()).containsExactlyInAnyOrder(userRole, adminRole);

            verify(userRepository).existsByUsername("adminuser");
            verify(roleRepository).findByName("USER");
            verify(roleRepository).findByName("ADMIN");
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("existinguser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            when(userRepository.existsByUsername("existinguser")).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username is already taken");

            verify(userRepository).existsByUsername("existinguser");
            verify(roleRepository, never()).findByName(anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when role not found")
        void shouldThrowExceptionWhenRoleNotFound() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("INVALID_ROLE"));

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Role not found: INVALID_ROLE");

            verify(userRepository).existsByUsername("newuser");
            verify(roleRepository).findByName("INVALID_ROLE");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when one of multiple roles not found")
        void shouldThrowExceptionWhenOneRoleNotFound() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER", "INVALID_ROLE"));

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.registerUser(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Role not found: INVALID_ROLE");

            verify(userRepository).existsByUsername("newuser");
            verify(roleRepository).findByName("USER");
            verify(roleRepository).findByName("INVALID_ROLE");
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("plainPassword");
            request.setRoles(List.of("USER"));

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("plainPassword")).thenReturn("$2a$10$encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            authService.registerUser(request);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPassword()).isEqualTo("$2a$10$encodedPassword");
            assertThat(savedUser.getPassword()).isNotEqualTo("plainPassword");

            verify(passwordEncoder).encode("plainPassword");
        }

        @Test
        @DisplayName("Should save user with correct username and roles")
        void shouldSaveUserWithCorrectData() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            authService.registerUser(request);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("testuser");
            assertThat(savedUser.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(savedUser.getRoles()).hasSize(1);
            assertThat(savedUser.getRoles()).contains(userRole);
        }

        @Test
        @DisplayName("Should handle empty roles list")
        void shouldHandleEmptyRolesList() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of());

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            User result = authService.registerUser(request);

            // Assert
            assertThat(result.getRoles()).isEmpty();
            verify(roleRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("Should handle duplicate roles in request")
        void shouldHandleDuplicateRolesInRequest() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER", "USER", "USER"));

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

            // Act
            User result = authService.registerUser(request);

            // Assert
            assertThat(result.getRoles()).hasSize(1);
            assertThat(result.getRoles()).contains(userRole);
            verify(roleRepository, times(3)).findByName("USER");
        }

        @Test
        @DisplayName("Should return saved user from repository")
        void shouldReturnSavedUserFromRepository() {
            // Arrange
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setRoles(List.of("USER"));

            User savedUser = new User();
            savedUser.setId(10L);
            savedUser.setUsername("newuser");
            savedUser.setPassword("$2a$10$encoded");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(10L);
                return user;
            });

            // Act
            User result = authService.registerUser(request);
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getPassword()).isEqualTo("$2a$10$encoded");
            assertThat(result.getRoles()).contains(userRole);

            verify(userRepository).save(any(User.class));
        }
    }
}