package com.addhuri.habit_tracker.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.addhuri.habit_tracker.user.entity.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
