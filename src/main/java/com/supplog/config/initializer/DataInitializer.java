package com.supplog.config.initializer;
import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.enabled:false}")
    private boolean adminSeedEnabled;

    @Value("${app.seed.admin.username:}")
    private String adminUsername;

    @Value("${app.seed.admin.email:}")
    private String adminEmail;

    @Value("${app.seed.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        getOrCreateRole(RoleName.ROLE_USER);
        Role adminRole = getOrCreateRole(RoleName.ROLE_ADMIN);

        if (!adminSeedEnabled) {
            return;
        }

        if (adminUsername.isBlank()
                || adminEmail.isBlank()
                || adminPassword.isBlank()) {

            throw new IllegalStateException(
                    "Admin seed is enabled but admin credentials are missing."
            );
        }

        String normalizedUsername = adminUsername.trim().toLowerCase();
        String normalizedEmail = adminEmail.trim().toLowerCase();

        if (userRepository.findByUsername(normalizedUsername).isPresent()
                || userRepository.findByEmail(normalizedEmail).isPresent()) {
            return;
        }

        User admin = new User();

        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setUsername(normalizedUsername);
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setBirthDate(LocalDate.of(1990, 1, 1));
        admin.setScore(0);
        admin.setDeleted(false);

        admin.getRoles().add(adminRole);

        userRepository.save(admin);
    }

    private Role getOrCreateRole(RoleName roleName) {

        return roleRepository.findByName(roleName)
                .orElseGet(() -> {

                    Role role = new Role();
                    role.setName(roleName);

                    return roleRepository.save(role);
                });
    }
}