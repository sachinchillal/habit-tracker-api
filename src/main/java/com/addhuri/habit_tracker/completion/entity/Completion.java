package com.addhuri.habit_tracker.completion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "completions")
public class Completion {

	@Id
	@Column(length = 36, nullable = false)
	private UUID id;

	@Column(name = "task_id", length = 36, nullable = false, updatable = false)
	private UUID taskId;

	@Column(name = "marked_at", nullable = false, updatable = false)
	private Instant markedAt;

	@Column(name = "marked_on", nullable = false)
	private LocalDate markedOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CompletionPolicy policy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		Instant now = Instant.now();
		if (markedAt == null) {
			markedAt = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

}
