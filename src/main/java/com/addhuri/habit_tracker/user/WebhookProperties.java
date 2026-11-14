package com.addhuri.habit_tracker.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ConfigurationProperties(prefix = "app.webhook")
public record WebhookProperties(String secret) {

	public boolean matches(String provided) {
		if (secret == null || secret.isBlank() || provided == null) {
			return false;
		}
		byte[] expected = secret.getBytes(StandardCharsets.UTF_8);
		byte[] actual = provided.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, actual);
	}

}
