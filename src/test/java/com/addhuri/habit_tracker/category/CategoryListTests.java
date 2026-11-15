package com.addhuri.habit_tracker.category;

import com.addhuri.habit_tracker.TestTokens;
import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;
import com.addhuri.habit_tracker.category.repository.CategoryRepository;
import com.addhuri.habit_tracker.security.AuthTokenProperties;
import com.addhuri.habit_tracker.user.WebhookProperties;
import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryListTests {

	private static final String OTHER_SECRET = "another-local-jwt-secret-not-the-same";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private AuthTokenProperties auth;

	@Autowired
	private WebhookProperties webhooks;

	@Autowired
	private UserRepository users;

	@Autowired
	private CategoryRepository categories;

	@Test
	void rejectsMissingToken() throws Exception {
		mvc.perform(get("/api/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void rejectsBadSignature() throws Exception {
		User user = saveUser(AccountStatus.ACTIVE);
		String bearer = "Bearer " + TestTokens.token(OTHER_SECRET, user.getId().toString(), Instant.now(),
				Instant.now().plus(Duration.ofMinutes(15)));
		mvc.perform(get("/api/categories").header("Authorization", bearer))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void rejectsExpiredToken() throws Exception {
		User user = saveUser(AccountStatus.ACTIVE);
		Instant expired = Instant.now().minus(Duration.ofMinutes(5));
		String bearer = "Bearer " + TestTokens.token(auth.jwtSecret(), user.getId().toString(),
				expired.minus(Duration.ofMinutes(15)), expired);
		mvc.perform(get("/api/categories").header("Authorization", bearer))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Token expired"));
	}

	@Test
	void rejectsMissingExpiry() throws Exception {
		User user = saveUser(AccountStatus.ACTIVE);
		String bearer = "Bearer " + TestTokens.token(auth.jwtSecret(), user.getId().toString(), Instant.now(), null);
		mvc.perform(get("/api/categories").header("Authorization", bearer))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void rejectsNonUuidSubject() throws Exception {
		Instant now = Instant.now();
		String bearer = "Bearer " + TestTokens.token(auth.jwtSecret(), "not-a-uuid", now, now.plus(Duration.ofMinutes(15)));
		mvc.perform(get("/api/categories").header("Authorization", bearer))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void rejectsUnknownUser() throws Exception {
		mvc.perform(get("/api/categories").header("Authorization", TestTokens.bearer(auth.jwtSecret(), UUID.randomUUID())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@ParameterizedTest
	@EnumSource(value = AccountStatus.class, names = { "PENDING", "INACTIVE", "DELETED" })
	void rejectsUserWhoIsNotActive(AccountStatus status) throws Exception {
		User user = saveUser(status);
		mvc.perform(get("/api/categories").header("Authorization", TestTokens.bearer(auth.jwtSecret(), user.getId())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void listsOnlyTheCallerCategories() throws Exception {
		User caller = saveUser(AccountStatus.ACTIVE);
		User other = saveUser(AccountStatus.ACTIVE);
		UUID[] ids = { UUID.randomUUID(), UUID.randomUUID() };
		Arrays.sort(ids, Comparator.comparing(UUID::toString));
		UUID lower = ids[0];
		UUID higher = ids[1];

		saveCategory(caller.getId(), higher, "Health", null, CategoryStatus.ACTIVE, null);
		saveCategory(caller.getId(), lower, "Health", higher, CategoryStatus.INACTIVE, "Heart health");
		saveCategory(caller.getId(), UUID.randomUUID(), "Beta", null, CategoryStatus.ARCHIVED, "Daily movement");
		saveCategory(caller.getId(), UUID.randomUUID(), "Aaa", null, CategoryStatus.DELETED, "Removed");
		saveCategory(other.getId(), UUID.randomUUID(), "Aaa", null, CategoryStatus.ACTIVE, "Someone else");

		mvc.perform(get("/api/categories").header("Authorization", TestTokens.bearer(auth.jwtSecret(), caller.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("OK"))
				.andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].name").value("Beta"))
				.andExpect(jsonPath("$.data[0].description").value("Daily movement"))
				.andExpect(jsonPath("$.data[0].status").value("ARCHIVED"))
				.andExpect(jsonPath("$.data[0].parentId").value(nullValue()))
				.andExpect(jsonPath("$.data[0].createdAt").exists())
				.andExpect(jsonPath("$.data[0].updatedAt").exists())
				.andExpect(jsonPath("$.data[1].id").value(lower.toString()))
				.andExpect(jsonPath("$.data[1].name").value("Health"))
				.andExpect(jsonPath("$.data[1].description").value("Heart health"))
				.andExpect(jsonPath("$.data[1].parentId").value(higher.toString()))
				.andExpect(jsonPath("$.data[1].status").value("INACTIVE"))
				.andExpect(jsonPath("$.data[2].id").value(higher.toString()))
				.andExpect(jsonPath("$.data[2].name").value("Health"))
				.andExpect(jsonPath("$.data[2].description").value(nullValue()))
				.andExpect(jsonPath("$.data[2].parentId").value(nullValue()))
				.andExpect(jsonPath("$.data[2].status").value("ACTIVE"));
	}

	@Test
	void publicRoutesStayOpen() throws Exception {
		mvc.perform(get("/api/ping")).andExpect(status().isOk()).andExpect(jsonPath("$.data").value("pong"));

		String id = UUID.randomUUID().toString();
		mvc.perform(post("/api/webhooks/users").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"id":"%s","accountStatus":"PENDING"}
						""".formatted(id)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid webhook secret"));

		mvc.perform(post("/api/webhooks/users").header("X-Webhook-Secret", webhooks.secret())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"id":"%s","accountStatus":"PENDING"}
						""".formatted(id)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accountStatus").value("PENDING"));
	}

	private User saveUser(AccountStatus status) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setAccountStatus(status);
		return users.saveAndFlush(user);
	}

	private void saveCategory(UUID userId, UUID id, String name, UUID parentId, CategoryStatus status,
			String description) {
		Category category = new Category();
		category.setId(id);
		category.setUserId(userId);
		category.setName(name);
		category.setParentId(parentId);
		category.setStatus(status);
		category.setDescription(description);
		categories.saveAndFlush(category);
	}

}
