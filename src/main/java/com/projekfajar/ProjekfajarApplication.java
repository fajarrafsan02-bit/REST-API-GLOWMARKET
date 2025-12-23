package com.projekfajar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.projekfajar.repository")
public class ProjekfajarApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjekfajarApplication.class, args);
	}

}
