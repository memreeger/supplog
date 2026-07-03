package com.supplog;

import com.supplog.entity.Role;
import com.supplog.entity.User;
import com.supplog.enums.RoleName;
import com.supplog.repository.RoleRepository;
import com.supplog.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
public class SupplogApplication {

    public static void main(String[] args) {

        SpringApplication.run(SupplogApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder encoder, RoleRepository roleRepository) {
        return args -> {
            Role adminRole = null;

            if (roleRepository.findAll().isEmpty()) {
                adminRole = new Role();
                adminRole.setName(RoleName.ROLE_ADMIN);

                roleRepository.save(adminRole);

                Role userRole = new Role();
                userRole.setName(RoleName.ROLE_USER);

                roleRepository.save(userRole);
            }

            if (userRepository.findByUsername("admin").isEmpty()) {

                User user = new User();

                user.setFirstName("Admin");
                user.setLastName("Eğer");
                user.setUsername("admin");
                user.setEmail("admin@suplog.com");
                user.setPassword(encoder.encode("123456"));
                adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
                user.getRoles().add(adminRole);

                user.setScore(0);
                user.setDeleted(false);
                // user.setBirthDate(LocalDate.of(2000, 1, 1));

                userRepository.save(user);
            }
        };
    }

}