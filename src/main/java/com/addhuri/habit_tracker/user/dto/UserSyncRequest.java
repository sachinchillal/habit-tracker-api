package com.addhuri.habit_tracker.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

import com.addhuri.habit_tracker.user.entity.AccountStatus;

public record UserSyncRequest(@NotNull UUID id, @NotNull AccountStatus accountStatus) {
}
