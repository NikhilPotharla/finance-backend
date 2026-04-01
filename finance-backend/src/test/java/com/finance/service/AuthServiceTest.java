package com.finance.service;

import com.finance.dto.AuthResponse;
import com.finance.dto.LoginRequest;
import com.finance.dto.RegisterRequest;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("encodedPass")
                .role(Role.VIEWER)
                .build();
    }

    // ─────────────── Register Tests ───────────────

    @Test
    @DisplayName("Register: success with default VIEWER role")
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo("VIEWER");
        assertThat(response.getMessage()).contains("successful");

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Register: success with ADMIN role")
    void register_adminRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Admin User");
        request.setEmail("admin@example.com");
        request.setPassword("admin123");
        request.setRole("ADMIN");

        User adminUser = User.builder().id(2L).name("Admin User").email("admin@example.com")
                .password("enc").role(Role.ADMIN).build();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("admin-token");

        AuthResponse response = authService.register(request);
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Register: throws exception if email already exists")
    void register_emailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("john@example.com");
        request.setPassword("pass");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    // ─────────────── Login Tests ───────────────

    @Test
    @DisplayName("Login: success returns token")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("login-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("login-token");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Login: throws BadCredentialsException for wrong password")
    void login_badCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
