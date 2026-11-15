package com.addhuri.habit_tracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Configuration
public class AuthTokenConfig {

	@Bean
	public JwtDecoder jwtDecoder(AuthTokenProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey(properties.jwtSecret()))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
				new ExpAndSubjectValidator()));
		return decoder;
	}

	private static SecretKey secretKey(String secret) {
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	static final class ExpAndSubjectValidator implements OAuth2TokenValidator<Jwt> {

		@Override
		public OAuth2TokenValidatorResult validate(Jwt jwt) {
			if (jwt.getExpiresAt() == null) {
				return failure("exp is required");
			}
			if (!isUuid(jwt.getSubject())) {
				return failure("sub must be a UUID");
			}
			return OAuth2TokenValidatorResult.success();
		}

		private static boolean isUuid(String subject) {
			if (subject == null) {
				return false;
			}
			try {
				UUID.fromString(subject);
				return true;
			} catch (IllegalArgumentException ex) {
				return false;
			}
		}

		private static OAuth2TokenValidatorResult failure(String description) {
			return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
		}

	}

}
