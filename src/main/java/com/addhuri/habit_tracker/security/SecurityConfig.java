package com.addhuri.habit_tracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiErrorWriter errors) throws Exception {
		var entryPoint = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, exception) -> errors
			.write(response, HttpStatus.UNAUTHORIZED, "Authentication required");
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
			.exceptionHandling(handler -> handler.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied));
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Authentication required");
		};
	}

}
