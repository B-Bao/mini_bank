package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@EnableWebSecurity
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						// 1. Health check công khai
						.requestMatchers("/actuator/health").permitAll()

						// 2. Swagger & OpenAPI docs công khai
						.requestMatchers(
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs",
								"/v3/api-docs/**"
						).permitAll()

						// 3. Các Actuator còn lại yêu cầu authenticated (hoặc .hasRole("ACTUATOR_ADMIN"))
						.requestMatchers("/actuator/**").authenticated()

						// 4. Mọi request nghiệp vụ khác
						.anyRequest().authenticated()
				)
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}
}