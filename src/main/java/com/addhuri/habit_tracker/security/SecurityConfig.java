package com.addhuri.habit_tracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiErrorWriter errors,
			ActiveUserJwtConverter jwtAuthenticationConverter) throws Exception {
		System.out.println("SecurityConfig: securityFilterChain");
		var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response,
				exception) -> {
			String message = tokenExpired(exception) ? "Token expired" : "Authentication required";
			errors.write(response, HttpStatus.UNAUTHORIZED, message);
		};
		System.out.println("SecurityConfig: entryPoint");
		var denied = (org.springframework.security.web.access.AccessDeniedHandler) (request, response, exception) -> errors
				.write(response, HttpStatus.FORBIDDEN, "Access denied");
		http.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api", "/api/ping").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/webhooks/users").permitAll()
						.requestMatchers("/api/**").authenticated()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(denied)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.exceptionHandling(handler -> handler.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied));
		return http.build();
	}

	private static boolean tokenExpired(Throwable exception) {
		for (Throwable current = exception; current != null; current = current.getCause()) {
			if (current instanceof JwtValidationException validation) {
				for (OAuth2Error error : validation.getErrors()) {
					String description = error.getDescription();
					if (description != null && description.startsWith("Jwt expired")) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
