package com.addhuri.habit_tracker.task;

import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.repository.CategoryRepository;
import com.addhuri.habit_tracker.task.entity.Task;
import com.addhuri.habit_tracker.task.entity.TaskStatus;
import com.addhuri.habit_tracker.task.repository.TaskRepository;
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
// @Transactional
class TaskRepositoryTests {

	@Autowired
	private TaskRepository tasks;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private UserRepository users;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsUncategorizedWithGeneratedIdAndActiveStatus() {
		UUID userId = saveUser().getId();

		Task task = tasks.saveAndFlush(newTask(userId, "Health"));
		entityManager.clear();

		Task stored = tasks.findById(task.getId()).orElseThrow();
		assertNotNull(stored.getId());
		assertEquals(userId, stored.getUserId());
		assertEquals("Health", stored.getName());
		assertEquals(TaskStatus.ACTIVE, stored.getStatus());
		assertEquals(stored.getCreatedAt(), stored.getUpdatedAt());
		assertNull(stored.getCategoryId());
		assertNull(stored.getDescription());
	}

	@Test
	void storesExplicitIdDescriptionAndCategory() {
		UUID userId = saveUser().getId();
		Category category = categories.saveAndFlush(newCategory(userId, "Health"));
		UUID id = UUID.randomUUID();

		Task task = newTask(userId, "Walk");
		task.setId(id);
		task.setDescription("Morning walk");
		task.setCategoryId(category.getId());
		tasks.saveAndFlush(task);
		entityManager.clear();

		Task stored = tasks.findById(id).orElseThrow();
		assertEquals(id, stored.getId());
		assertEquals("Morning walk", stored.getDescription());
		assertEquals(category.getId(), stored.getCategoryId());
	}

	@ParameterizedTest
	@EnumSource(TaskStatus.class)
	void storesEachStatus(TaskStatus status) {
		Task task = newTask(saveUser().getId(), "Health");
		task.setStatus(status);
		tasks.saveAndFlush(task);
		entityManager.clear();

		assertEquals(status, tasks.findById(task.getId()).orElseThrow().getStatus());
	}

	@Test
	void renameKeepsCreatedAtAndUserId() throws Exception {
		UUID userId = saveUser().getId();
		UUID otherUserId = saveUser().getId();
		Task task = tasks.saveAndFlush(newTask(userId, "Health"));
		Instant createdAt = task.getCreatedAt();

		Thread.sleep(20);
		task.setName("Fitness");
		task.setUserId(otherUserId);
		tasks.saveAndFlush(task);
		entityManager.clear();

		Task stored = tasks.findById(task.getId()).orElseThrow();
		assertEquals("Fitness", stored.getName());
		assertEquals(userId, stored.getUserId());
		assertEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getCreatedAt());
		assertNotEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getUpdatedAt());
	}

	@Test
	void allowsSameUncategorizedNameForTwoUsers() {
		Task first = tasks.saveAndFlush(newTask(saveUser().getId(), "Health"));
		Task second = tasks.saveAndFlush(newTask(saveUser().getId(), "Health"));

		assertNotEquals(first.getUserId(), second.getUserId());
		assertNotNull(tasks.findById(first.getId()).orElse(null));
		assertNotNull(tasks.findById(second.getId()).orElse(null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Health", "health", " Health " })
	void rejectsDuplicateUncategorizedName(String duplicateName) {
		UUID userId = saveUser().getId();
		tasks.saveAndFlush(newTask(userId, "Health"));

		Task duplicate = newTask(userId, duplicateName);
		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(duplicate));
	}

	@Test
	void allowsCategorizedTaskToReuseUncategorizedName() {
		UUID userId = saveUser().getId();
		tasks.saveAndFlush(newTask(userId, "Health"));
		Category category = categories.saveAndFlush(newCategory(userId, "Health"));

		Task task = newTask(userId, "Health");
		task.setCategoryId(category.getId());
		tasks.saveAndFlush(task);
		entityManager.clear();

		assertEquals(category.getId(), tasks.findById(task.getId()).orElseThrow().getCategoryId());
	}

	@Test
	void allowsSameNameInDifferentCategories() {
		UUID userId = saveUser().getId();
		Category firstCategory = categories.saveAndFlush(newCategory(userId, "Health"));
		Category secondCategory = categories.saveAndFlush(newCategory(userId, "Work"));

		Task first = newTask(userId, "Review");
		first.setCategoryId(firstCategory.getId());
		tasks.saveAndFlush(first);

		Task second = newTask(userId, "Review");
		second.setCategoryId(secondCategory.getId());
		tasks.saveAndFlush(second);
		entityManager.clear();

		assertEquals(firstCategory.getId(), tasks.findById(first.getId()).orElseThrow().getCategoryId());
		assertEquals(secondCategory.getId(), tasks.findById(second.getId()).orElseThrow().getCategoryId());
	}

	@Test
	void rejectsDuplicateNameInSameCategory() {
		UUID userId = saveUser().getId();
		Category category = categories.saveAndFlush(newCategory(userId, "Health"));
		Task first = newTask(userId, "Walk");
		first.setCategoryId(category.getId());
		tasks.saveAndFlush(first);

		Task second = newTask(userId, "Walk");
		second.setCategoryId(category.getId());
		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(second));
	}

	@Test
	void rejectsCategoryOwnedByAnotherUser() {
		Category category = categories.saveAndFlush(newCategory(saveUser().getId(), "Health"));
		Task task = newTask(saveUser().getId(), "Walk");
		task.setCategoryId(category.getId());

		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(task));
	}

	@Test
	void rejectsBlankName() {
		Task task = newTask(saveUser().getId(), "   ");

		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(task));
	}

	@Test
	void rejectsUnknownUser() {
		Task task = newTask(UUID.randomUUID(), "Health");

		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(task));
	}

	@Test
	void rejectsUnknownCategory() {
		Task task = newTask(saveUser().getId(), "Walk");
		task.setCategoryId(UUID.randomUUID());

		assertThrows(DataIntegrityViolationException.class, () -> tasks.saveAndFlush(task));
	}

	@Test
	void rejectsDeletingCategoryThatHasTask() {
		UUID userId = saveUser().getId();
		Category category = categories.saveAndFlush(newCategory(userId, "Health"));
		Task task = newTask(userId, "Walk");
		task.setCategoryId(category.getId());
		tasks.saveAndFlush(task);

		assertThrows(DataIntegrityViolationException.class, () -> {
			categories.delete(category);
			categories.flush();
		});
	}

	@Test
	void rejectsDeletingUserWhoOwnsTask() {
		User user = saveUser();
		tasks.saveAndFlush(newTask(user.getId(), "Health"));

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

	private Task newTask(UUID userId, String name) {
		Task task = new Task();
		task.setUserId(userId);
		task.setName(name);
		return task;
	}

}
