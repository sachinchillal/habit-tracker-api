package com.addhuri.habit_tracker.category.dto;

import com.addhuri.habit_tracker.category.entity.CategoryStatus;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(@NotNull(message = "Status is required") CategoryStatus status) {
}
