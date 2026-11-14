package com.addhuri.habit_tracker.category;

import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;
import com.addhuri.habit_tracker.category.repository.CategoryRepository;
import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CategoryRepositoryTests {

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private UserRepository users;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsRootWithGeneratedIdAndActiveStatus() {
		UUID userId = saveUser().getId();

		Category category = categories.saveAndFlush(newCategory(userId, "Health"));
		entityManager.clear();

		Category stored = categories.findById(category.getId()).orElseThrow();
		assertNotNull(stored.getId());
		assertEquals(userId, stored.getUserId());
		assertEquals("Health", stored.getName());
		assertEquals(CategoryStatus.ACTIVE, stored.getStatus());
		assertEquals(stored.getCreatedAt(), stored.getUpdatedAt());
		assertNull(stored.getParentId());
		assertNull(stored.getDescription());
	}

	@Test
	void storesExplicitIdDescriptionAndParent() {
		UUID userId = saveUser().getId();
		Category parent = categories.saveAndFlush(newCategory(userId, "Health"));
		UUID id = UUID.randomUUID();

		Category category = newCategory(userId, "Cardio");
		category.setId(id);
		category.setDescription("Heart health");
		category.setParentId(parent.getId());
		categories.saveAndFlush(category);
		entityManager.clear();

		Category stored = categories.findById(id).orElseThrow();
		assertEquals(id, stored.getId());
		assertEquals("Heart health", stored.getDescription());
		assertEquals(parent.getId(), stored.getParentId());
	}

	@ParameterizedTest
	@EnumSource(CategoryStatus.class)
	void storesEachStatus(CategoryStatus status) {
		Category category = newCategory(saveUser().getId(), "Health");
		category.setStatus(status);
		categories.saveAndFlush(category);
		entityManager.clear();

		assertEquals(status, categories.findById(category.getId()).orElseThrow().getStatus());
	}

	@Test
	void renameKeepsCreatedAtAndUserId() throws Exception {
		UUID userId = saveUser().getId();
		UUID otherUserId = saveUser().getId();
		Category category = categories.saveAndFlush(newCategory(userId, "Health"));
		Instant createdAt = category.getCreatedAt();

		Thread.sleep(20);
		category.setName("Fitness");
		category.setUserId(otherUserId);
		categories.saveAndFlush(category);
		entityManager.clear();

		Category stored = categories.findById(category.getId()).orElseThrow();
		assertEquals("Fitness", stored.getName());
		assertEquals(userId, stored.getUserId());
		assertEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getCreatedAt());
		assertNotEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getUpdatedAt());
	}

	@Test
	void allowsSameRootNameForTwoUsers() {
		Category first = categories.saveAndFlush(newCategory(saveUser().getId(), "Health"));
		Category second = categories.saveAndFlush(newCategory(saveUser().getId(), "Health"));

		assertNotEquals(first.getUserId(), second.getUserId());
		assertNotNull(categories.findById(first.getId()).orElse(null));
		assertNotNull(categories.findById(second.getId()).orElse(null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Health", "health", " Health " })
	void rejectsDuplicateRootName(String duplicateName) {
		UUID userId = saveUser().getId();
		categories.saveAndFlush(newCategory(userId, "Health"));

		Category duplicate = newCategory(userId, duplicateName);
		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(duplicate));
	}

	@Test
	void allowsChildToReuseParentName() {
		UUID userId = saveUser().getId();
		Category parent = categories.saveAndFlush(newCategory(userId, "Health"));
		Category child = newCategory(userId, "Health");
		child.setParentId(parent.getId());
		categories.saveAndFlush(child);
		entityManager.clear();

		assertEquals(parent.getId(), categories.findById(child.getId()).orElseThrow().getParentId());
	}

	@Test
	void rejectsDuplicateSiblingName() {
		UUID userId = saveUser().getId();
		Category parent = categories.saveAndFlush(newCategory(userId, "Health"));
		Category firstChild = newCategory(userId, "Cardio");
		firstChild.setParentId(parent.getId());
		categories.saveAndFlush(firstChild);

		Category secondChild = newCategory(userId, "Cardio");
		secondChild.setParentId(parent.getId());
		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(secondChild));
	}

	@Test
	void rejectsParentOwnedByAnotherUser() {
		Category parent = categories.saveAndFlush(newCategory(saveUser().getId(), "Health"));
		Category child = newCategory(saveUser().getId(), "Cardio");
		child.setParentId(parent.getId());

		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(child));
	}

	@Test
	void rejectsSelfParent() {
		UUID id = UUID.randomUUID();
		Category category = newCategory(saveUser().getId(), "Health");
		category.setId(id);
		category.setParentId(id);

		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(category));
	}

	@Test
	void rejectsBlankName() {
		Category category = newCategory(saveUser().getId(), "   ");

		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(category));
	}

	@Test
	void rejectsUnknownUser() {
		Category category = newCategory(UUID.randomUUID(), "Health");

		assertThrows(DataIntegrityViolationException.class, () -> categories.saveAndFlush(category));
	}

	@Test
	void rejectsDeletingParentThatHasChild() {
		UUID userId = saveUser().getId();
		Category parent = categories.saveAndFlush(newCategory(userId, "Health"));
		Category child = newCategory(userId, "Cardio");
		child.setParentId(parent.getId());
		categories.saveAndFlush(child);

		assertThrows(DataIntegrityViolationException.class, () -> {
			categories.delete(parent);
			categories.flush();
		});
	}

	@Test
	void rejectsDeletingUserWhoOwnsCategory() {
		User user = saveUser();
		categories.saveAndFlush(newCategory(user.getId(), "Health"));

		assertThrows(DataIntegrityViolationException.class, () -> {
			users.delete(user);
			users.flush();
		});
	}

	private User saveUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setAccountStatus(AccountStatus.ACTIVE);
		return users.saveAndFlush(user);
	}

	private Category newCategory(UUID userId, String name) {
		Category category = new Category();
		category.setUserId(userId);
		category.setName(name);
		return category;
	}

}
