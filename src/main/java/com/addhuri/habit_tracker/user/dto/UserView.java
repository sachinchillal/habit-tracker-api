package com.addhuri.habit_tracker.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.addhuri.habit_tracker.user.entity.AccountStatus;

public record UserView(UUID id, AccountStatus accountStatus, Instant createdAt, Instant updatedAt) {
}
