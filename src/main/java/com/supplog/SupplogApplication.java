package com.supplog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SupplogApplication {

	public static void main(String[] args) {

        SpringApplication.run(SupplogApplication.class, args);
	}

}
