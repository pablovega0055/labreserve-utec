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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Todo usuario que se registra es STUDENT (TECHNICIAN y ADMIN se crean en DataInitializer)
    @Transactional
    public UserResponse register(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("El username ya esta registrado: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("El email ya esta registrado: " + email);
        }

        User user = new User(username, email, passwordEncoder.encode(req.password()), Role.STUDENT);
        return UserResponse.from(userRepository.save(user));
    }

    // Lanza BadCredentialsException (-> 401) si el username o password no coinciden
    public LoginResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        UserDetails user = (UserDetails) auth.getPrincipal();
        return new LoginResponse(jwtService.generateToken(user), jwtService.getExpirationSeconds());
    }
}
