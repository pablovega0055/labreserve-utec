package com.utec.dbp.config;

import com.utec.dbp.model.Role;
import com.utec.dbp.model.User;
import com.utec.dbp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// /auth/register siempre crea STUDENT. Aqui se crean un ADMIN y un TECHNICIAN iniciales
// para poder registrar laboratorios y publicar turnos.
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String technicianPassword;

    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin-password:admin12345}") String adminPassword,
                           @Value("${app.seed.technician-password:tech12345}") String technicianPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.technicianPassword = technicianPassword;
    }

    @Override
    public void run(String... args) {
        createIfMissing("admin", "admin@utec.edu.pe", adminPassword, Role.ADMIN);
        createIfMissing("technician", "technician@utec.edu.pe", technicianPassword, Role.TECHNICIAN);
    }

    private void createIfMissing(String username, String email, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(new User(username, email, passwordEncoder.encode(rawPassword), role));
        log.info("Usuario inicial creado: {} ({})", username, role);
    }
}
