package com.supplog;

import com.supplog.entity.User;
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
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder encoder){
        return args  ->{
            // Seed
            if(userRepository.findByUsername("admin").isEmpty()) {
                User user = new User();
                user.setUsername("admin");
                user.setPassword(encoder.encode("123"));

                userRepository.save(user);
            }
        };
    }

}
