package com.addhuri.habit_tracker.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.addhuri.habit_tracker.task.entity.Task;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
}
