package com.addhuri.habit_tracker.category.dto;

import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;

import java.time.Instant;
import java.util.UUID;

public record CategoryView(UUID id, String name, String description, UUID parentId, CategoryStatus status,
		Instant createdAt, Instant updatedAt) {

	public static CategoryView from(Category category) {
		return new CategoryView(category.getId(), category.getName(), category.getDescription(), category.getParentId(),
				category.getStatus(), category.getCreatedAt(), category.getUpdatedAt());
	}

}
