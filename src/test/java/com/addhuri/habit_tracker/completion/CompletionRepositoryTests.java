package com.addhuri.habit_tracker.completion;

import com.addhuri.habit_tracker.completion.entity.Completion;
import com.addhuri.habit_tracker.completion.entity.CompletionPolicy;
import com.addhuri.habit_tracker.completion.repository.CompletionRepository;
import com.addhuri.habit_tracker.task.entity.Task;
import com.addhuri.habit_tracker.task.repository.TaskRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
// @Transactional
class CompletionRepositoryTests {

	@Autowired
	private CompletionRepository completions;

	@Autowired
	private TaskRepository tasks;

	@Autowired
	private UserRepository users;

	@Autowired
	private EntityManager entityManager;

	@Test
	void persistsWithGeneratedIdAndMatchingTimestamps() {
		UUID taskId = saveTask().getId();

		Completion completion = completions.saveAndFlush(
				newCompletion(taskId, LocalDate.of(2026, 11, 14), CompletionPolicy.ONCE_PER_DAY));
		entityManager.clear();

		Completion stored = completions.findById(completion.getId()).orElseThrow();
		assertNotNull(stored.getId());
		assertEquals(taskId, stored.getTaskId());
		assertEquals(LocalDate.of(2026, 11, 14), stored.getMarkedOn());
		assertEquals(CompletionPolicy.ONCE_PER_DAY, stored.getPolicy());
		assertEquals(stored.getCreatedAt(), stored.getUpdatedAt());
		assertEquals(stored.getCreatedAt(), stored.getMarkedAt());
	}

	@Test
	void storesExplicitMarkedAt() {
		UUID taskId = saveTask().getId();
		Instant markedAt = Instant.parse("2026-11-14T04:30:00Z");

		Completion completion = newCompletion(taskId, LocalDate.of(2026, 11, 14), CompletionPolicy.UNLIMITED);
		completion.setMarkedAt(markedAt);
		completions.saveAndFlush(completion);
		entityManager.clear();

		Completion stored = completions.findById(completion.getId()).orElseThrow();
		assertEquals(markedAt.truncatedTo(ChronoUnit.MICROS), stored.getMarkedAt().truncatedTo(ChronoUnit.MICROS));
		assertEquals(stored.getCreatedAt(), stored.getUpdatedAt());
	}

	@ParameterizedTest
	@EnumSource(CompletionPolicy.class)
	void storesEachPolicy(CompletionPolicy policy) {
		Completion completion = newCompletion(saveTask().getId(), LocalDate.of(2026, 11, 14), policy);
		completions.saveAndFlush(completion);
		entityManager.clear();

		assertEquals(policy, completions.findById(completion.getId()).orElseThrow().getPolicy());
	}

	@Test
	void changingMarkedOnKeepsCreatedAtMarkedAtAndTaskId() throws Exception {
		UUID taskId = saveTask().getId();
		UUID otherTaskId = saveTask().getId();
		Instant markedAt = Instant.parse("2026-11-14T02:30:00Z");
		Completion completion = newCompletion(taskId, LocalDate.of(2026, 11, 14), CompletionPolicy.UNLIMITED);
		completion.setMarkedAt(markedAt);
		completions.saveAndFlush(completion);
		Instant createdAt = completion.getCreatedAt();

		Thread.sleep(20);
		completion.setMarkedOn(LocalDate.of(2026, 11, 15));
		completion.setTaskId(otherTaskId);
		completion.setMarkedAt(Instant.parse("2026-11-14T08:00:00Z"));
		completions.saveAndFlush(completion);
		entityManager.clear();

		Completion stored = completions.findById(completion.getId()).orElseThrow();
		assertEquals(LocalDate.of(2026, 11, 15), stored.getMarkedOn());
		assertEquals(taskId, stored.getTaskId());
		assertEquals(markedAt.truncatedTo(ChronoUnit.MICROS), stored.getMarkedAt().truncatedTo(ChronoUnit.MICROS));
		assertEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getCreatedAt());
		assertNotEquals(createdAt.truncatedTo(ChronoUnit.MICROS), stored.getUpdatedAt());
	}

	@Test
	void allowsMultipleUnlimitedMarksOnTheSameDay() {
		UUID taskId = saveTask().getId();
		LocalDate markedOn = LocalDate.of(2026, 11, 14);
		Completion first = newCompletion(taskId, markedOn, CompletionPolicy.UNLIMITED);
		first.setMarkedAt(Instant.parse("2026-11-14T04:30:00Z"));
		Completion second = newCompletion(taskId, markedOn, CompletionPolicy.UNLIMITED);
		second.setMarkedAt(Instant.parse("2026-11-14T05:00:00Z"));
		completions.saveAndFlush(first);
		completions.saveAndFlush(second);
		entityManager.clear();

		assertEquals(2, completions.countByTaskIdAndMarkedOn(taskId, markedOn));
		Completion latest = completions.findFirstByTaskIdOrderByMarkedAtDesc(taskId).orElseThrow();
		assertEquals(second.getId(), latest.getId());
		assertEquals(Instant.parse("2026-11-14T05:00:00Z").truncatedTo(ChronoUnit.MICROS),
				latest.getMarkedAt().truncatedTo(ChronoUnit.MICROS));
	}

	@Test
	void rejectsSecondOncePerDayMarkOnTheSameDate() {
		UUID taskId = saveTask().getId();
		LocalDate markedOn = LocalDate.of(2026, 11, 14);
		completions.saveAndFlush(newCompletion(taskId, markedOn, CompletionPolicy.ONCE_PER_DAY));

		Completion duplicate = newCompletion(taskId, markedOn, CompletionPolicy.ONCE_PER_DAY);
		assertThrows(DataIntegrityViolationException.class, () -> completions.saveAndFlush(duplicate));
	}

	@Test
	void allowsOncePerDayMarkOnTheNextDate() {
		UUID taskId = saveTask().getId();
		completions.saveAndFlush(newCompletion(taskId, LocalDate.of(2026, 11, 14), CompletionPolicy.ONCE_PER_DAY));
		Completion next = completions.saveAndFlush(
				newCompletion(taskId, LocalDate.of(2026, 11, 15), CompletionPolicy.ONCE_PER_DAY));
		entityManager.clear();

		assertEquals(LocalDate.of(2026, 11, 15), completions.findById(next.getId()).orElseThrow().getMarkedOn());
	}

	@Test
	void allowsOncePerDayMarksForTwoTasksOnTheSameDate() {
		LocalDate markedOn = LocalDate.of(2026, 11, 14);
		Completion first = completions.saveAndFlush(
				newCompletion(saveTask().getId(), markedOn, CompletionPolicy.ONCE_PER_DAY));
		Completion second = completions.saveAndFlush(
				newCompletion(saveTask().getId(), markedOn, CompletionPolicy.ONCE_PER_DAY));

		assertNotEquals(first.getTaskId(), second.getTaskId());
		assertNotNull(completions.findById(first.getId()).orElse(null));
		assertNotNull(completions.findById(second.getId()).orElse(null));
	}

	@Test
	void listsCompletionsForTheRequestedTasksAndDate() {
		UUID water = saveTask().getId();
		UUID meditate = saveTask().getId();
		LocalDate saturday = LocalDate.of(2026, 11, 14);
		LocalDate sunday = LocalDate.of(2026, 11, 15);
		Completion waterSaturday = completions.saveAndFlush(
				newCompletion(water, saturday, CompletionPolicy.UNLIMITED));
		completions.saveAndFlush(newCompletion(water, sunday, CompletionPolicy.UNLIMITED));
		Completion meditateSaturday = completions.saveAndFlush(
				newCompletion(meditate, saturday, CompletionPolicy.ONCE_PER_DAY));

		List<Completion> stored = completions.findByTaskIdInAndMarkedOn(List.of(water, meditate), saturday);

		assertEquals(Set.of(waterSaturday.getId(), meditateSaturday.getId()),
				stored.stream().map(Completion::getId).collect(Collectors.toSet()));
	}

	@Test
	void rejectsUnknownTask() {
		Completion completion = newCompletion(UUID.randomUUID(), LocalDate.of(2026, 11, 14),
				CompletionPolicy.ONCE_PER_DAY);

		assertThrows(DataIntegrityViolationException.class, () -> completions.saveAndFlush(completion));
	}

	@Test
	void rejectsMissingMarkedOn() {
		Completion completion = newCompletion(saveTask().getId(), null, CompletionPolicy.ONCE_PER_DAY);

		assertThrows(DataIntegrityViolationException.class, () -> completions.saveAndFlush(completion));
	}

	@Test
	void rejectsDeletingTaskThatHasCompletion() {
		Task task = saveTask();
		completions.saveAndFlush(newCompletion(task.getId(), LocalDate.of(2026, 11, 14), CompletionPolicy.ONCE_PER_DAY));

		assertThrows(DataIntegrityViolationException.class, () -> {
			tasks.delete(task);
			tasks.flush();
		});
	}

	private Task saveTask() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setAccountStatus(AccountStatus.ACTIVE);
		users.saveAndFlush(user);

		Task task = new Task();
		task.setUserId(user.getId());
		task.setName("Walk");
		return tasks.saveAndFlush(task);
	}

	private Completion newCompletion(UUID taskId, LocalDate markedOn, CompletionPolicy policy) {
		Completion completion = new Completion();
		completion.setTaskId(taskId);
		completion.setMarkedOn(markedOn);
		completion.setPolicy(policy);
		return completion;
	}

}
