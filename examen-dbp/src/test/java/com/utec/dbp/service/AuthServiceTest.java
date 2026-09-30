package com.utec.dbp.service;

import com.utec.dbp.dto.LoginRequest;
import com.utec.dbp.dto.LoginResponse;
import com.utec.dbp.dto.RegisterRequest;
import com.utec.dbp.dto.UserResponse;
import com.utec.dbp.exception.ConflictException;
import com.utec.dbp.model.Role;
import com.utec.dbp.model.User;
import com.utec.dbp.repository.UserRepository;
import com.utec.dbp.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private final RegisterRequest request =
            new RegisterRequest("pablo.vega", "pablo.vega@utec.edu.pe", "pablovega2026");

    @Test
    void register_ok_encriptaPasswordYAsignaStudent() {
        when(passwordEncoder.encode("pablovega2026")).thenReturn("$2a$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse res = authService.register(request);

        assertEquals(1L, res.id());
        assertEquals("pablo.vega", res.username());
        assertEquals("pablo.vega@utec.edu.pe", res.email());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("$2a$hash", captor.getValue().getPassword());
        assertEquals(Role.STUDENT, captor.getValue().getRole());
    }

    @Test
    void register_usernameDuplicado_lanza409() {
        when(userRepository.existsByUsername("pablo.vega")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailDuplicado_lanza409() {
        when(userRepository.existsByEmail("pablo.vega@utec.edu.pe")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ok_devuelveTokenYExpiresIn() {
        User user = new User("pablo.vega", "pablo.vega@utec.edu.pe", "$2a$hash", Role.STUDENT);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse res = authService.login(new LoginRequest("pablo.vega", "pablovega2026"));

        assertEquals("jwt-token", res.token());
        assertEquals(3600L, res.expiresIn());
    }

    @Test
    void login_credencialesInvalidas_propagaExcepcion() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("pablo.vega", "incorrecta")));
    }
}
