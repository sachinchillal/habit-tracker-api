package com.addhuri.habit_tracker.category.controller;

import com.addhuri.habit_tracker.api.ApiResponse;
import com.addhuri.habit_tracker.category.CategoryService;
import com.addhuri.habit_tracker.category.dto.CategoryCreateRequest;
import com.addhuri.habit_tracker.category.dto.CategoryStatusRequest;
import com.addhuri.habit_tracker.category.dto.CategoryUpdateRequest;
import com.addhuri.habit_tracker.category.dto.CategoryView;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	private final CategoryService categories;

	public CategoryController(CategoryService categories) {
		this.categories = categories;
	}

	@GetMapping
	public ApiResponse<List<CategoryView>> list(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(categories.list(userId(jwt)));
	}

	@PostMapping
	public ApiResponse<CategoryView> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CategoryCreateRequest request) {
		return ApiResponse.ok(categories.create(userId(jwt), request));
	}

	@GetMapping("/{id}")
	public ApiResponse<CategoryView> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ApiResponse.ok(categories.get(userId(jwt), id));
	}

	@PatchMapping("/{id}")
	public ApiResponse<CategoryView> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody CategoryUpdateRequest request) {
		return ApiResponse.ok(categories.update(userId(jwt), id, request));
	}

	@PatchMapping("/{id}/status")
	public ApiResponse<CategoryView> updateStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody CategoryStatusRequest request) {
		return ApiResponse.ok(categories.updateStatus(userId(jwt), id, request));
	}

	@DeleteMapping("/{id}")
	public ApiResponse<CategoryView> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return ApiResponse.ok(categories.delete(userId(jwt), id));
	}

	private UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
