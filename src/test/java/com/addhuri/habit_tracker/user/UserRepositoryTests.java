package com.addhuri.habit_tracker.user;

import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UserRepositoryTests {

	@Autowired
	private UserRepository users;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsExplicitIdAndActiveStatus() {
		UUID id = UUID.randomUUID();
		User user = newUser(id, AccountStatus.ACTIVE);

		users.saveAndFlush(user);
		entityManager.clear();

		User stored = users.findById(id).orElseThrow();
		assertEquals(id, stored.getId());
		assertEquals(AccountStatus.ACTIVE, stored.getAccountStatus());
		assertEquals(stored.getCreatedAt(), stored.getUpdatedAt());
	}

	@ParameterizedTest
	@EnumSource(AccountStatus.class)
	void storesEachAccountStatus(AccountStatus status) {
		UUID id = UUID.randomUUID();
		users.saveAndFlush(newUser(id, status));
		entityManager.clear();

		assertEquals(status, users.findById(id).orElseThrow().getAccountStatus());
	}

	@Test
	void statusChangeKeepsCreatedAt() throws Exception {
		UUID id = UUID.randomUUID();
		User user = users.saveAndFlush(newUser(id, AccountStatus.PENDING));
		Instant createdAt = user.getCreatedAt();

		Thread.sleep(20);
		user.setAccountStatus(AccountStatus.ACTIVE);
		user.setCreatedAt(createdAt.plusSeconds(3600));
		users.saveAndFlush(user);
		entityManager.clear();

		User stored = users.findById(id).orElseThrow();
		assertEquals(AccountStatus.ACTIVE, stored.getAccountStatus());
		assertEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getCreatedAt());
		assertNotEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getUpdatedAt());
	}

	@Test
	void rejectsDuplicateId() {
		UUID id = UUID.randomUUID();
		users.saveAndFlush(newUser(id, AccountStatus.ACTIVE));
		entityManager.clear();

		assertThrows(DataIntegrityViolationException.class, () -> {
			entityManager.persist(newUser(id, AccountStatus.PENDING));
			users.flush();
		});
	}

	@Test
	void rejectsMissingAccountStatus() {
		User user = new User();
		user.setId(UUID.randomUUID());

		assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(user));
	}

	@Test
	void rejectsMissingId() {
		User user = new User();
		user.setAccountStatus(AccountStatus.ACTIVE);

		assertThrows(JpaSystemException.class, () -> users.saveAndFlush(user));
	}

	@Test
	void deletesUserWhoOwnsNothing() {
		UUID id = UUID.randomUUID();
		User user = users.saveAndFlush(newUser(id, AccountStatus.ACTIVE));

		users.delete(user);
		users.flush();
		entityManager.clear();

		assertTrue(users.findById(id).isEmpty());
	}

	private User newUser(UUID id, AccountStatus status) {
		User user = new User();
		user.setId(id);
		user.setAccountStatus(status);
		return user;
	}

}
