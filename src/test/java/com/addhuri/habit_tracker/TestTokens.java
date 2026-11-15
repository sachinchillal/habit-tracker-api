package com.addhuri.habit_tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.addhuri.habit_tracker.security.AuthTokenProperties;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
public final class TestTokens {

	@Autowired
	private AuthTokenProperties auth;

	private TestTokens() {
	}

	@Test
	void printBearerToken() {
		UUID userId = UUID.fromString("06a7d026-652c-4549-8214-448e428246b2");
		String secret = auth.jwtSecret();
		String token = bearer(secret, userId);
		System.out.println();
		System.out.println(token);
		System.out.println();
	}

	public static String bearer(String secret, UUID userId) {
		Instant now = Instant.now();
		return "Bearer " + token(secret, userId.toString(), now, now.plus(Duration.ofMinutes(150)));
	}

	public static String token(String secret, String subject, Instant issuedAt, Instant expiresAt) {
		var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		var encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(subject).issuedAt(issuedAt);
		if (expiresAt != null) {
			claims.expiresAt(expiresAt);
		}
		var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}

}
