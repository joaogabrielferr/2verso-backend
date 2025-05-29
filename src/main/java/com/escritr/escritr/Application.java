package com.escritr.escritr;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@SpringBootApplication
public class Application {

	@Autowired
	private Environment env;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@PostConstruct
	public void logActiveProfiles() {

		System.out.println("Active profiles: " + Arrays.toString(env.getActiveProfiles()));
		System.out.println("Attempting to read GOOGLE_CLIENT_ID: " + System.getenv("GOOGLE_CLIENT_ID"));
		System.out.println("Attempting to read GOOGLE_CLIENT_SECRET: " + System.getenv("GOOGLE_CLIENT_SECRET"));
	}


}
