package com.addhuri.habit_tracker.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.addhuri.habit_tracker.category.entity.Category;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
