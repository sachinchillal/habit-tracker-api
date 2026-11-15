package com.addhuri.habit_tracker.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "app.auth")
public record AuthTokenProperties(String jwtSecret) {

	public AuthTokenProperties {
		if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalArgumentException("app.auth.jwt-secret must be at least 32 bytes");
		}
	}

}
