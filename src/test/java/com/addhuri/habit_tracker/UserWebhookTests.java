package com.addhuri.habit_tracker;

import com.addhuri.habit_tracker.user.WebhookProperties;
import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserWebhookTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository users;

	@Autowired
	private WebhookProperties properties;

	@Test
	public void testWebhookSecret() {
		assertEquals("local-dev-webhook-secret", properties.secret());
	}

	@Test
	void syncsUserFromWebhook() throws Exception {
		String id = UUID.randomUUID().toString();
		System.out.println(body(id, "PENDING"));

		mvc.perform(post("/api/webhooks/users").contentType(MediaType.APPLICATION_JSON)
				.content(body(id, "PENDING")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid webhook secret"));

		mvc.perform(post("/api/webhooks/users").header("X-Webhook-Secret", "wrong-secret")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(id, "PENDING")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid webhook secret"));

		mvc.perform(post("/api/webhooks/users").header("X-Webhook-Secret", properties.secret())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(id, "PENDING")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id))
				.andExpect(jsonPath("$.data.accountStatus").value("PENDING"));
		Instant createdAt = users.findById(UUID.fromString(id)).orElseThrow().getCreatedAt();

		Thread.sleep(20);

		mvc.perform(post("/api/webhooks/users").header("X-Webhook-Secret", properties.secret())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(id, "ACTIVE")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
		var updated = users.findById(UUID.fromString(id)).orElseThrow();
		assertEquals(createdAt, updated.getCreatedAt());
		assertEquals(AccountStatus.ACTIVE, updated.getAccountStatus());
		assertNotEquals(createdAt, updated.getUpdatedAt());

		mvc.perform(post("/api/webhooks/users").header("X-Webhook-Secret", properties.secret())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body(id, "BANNED")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	private static String body(String id, String accountStatus) {
		return """
				{"id":"%s","accountStatus":"%s"}
				""".formatted(id, accountStatus);
	}

}
