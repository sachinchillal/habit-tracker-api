package com.addhuri.habit_tracker.category;

import com.addhuri.habit_tracker.api.ApiException;
import com.addhuri.habit_tracker.category.dto.CategoryCreateRequest;
import com.addhuri.habit_tracker.category.dto.CategoryStatusRequest;
import com.addhuri.habit_tracker.category.dto.CategoryUpdateRequest;
import com.addhuri.habit_tracker.category.dto.CategoryView;
import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;
import com.addhuri.habit_tracker.category.repository.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CategoryService {

	// No category can use this id, so a create check excludes nothing.
	private static final UUID NO_ROW = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private final CategoryRepository categories;

	public CategoryService(CategoryRepository categories) {
		this.categories = categories;
	}

	@Transactional(readOnly = true)
	public List<CategoryView> list(UUID userId) {
		return categories.findByUserIdAndStatusNotOrderByNameAscIdAsc(userId, CategoryStatus.DELETED)
			.stream()
			.map(CategoryView::from)
			.toList();
	}

	@Transactional(noRollbackFor = ApiException.class)
	public CategoryView create(UUID userId, CategoryCreateRequest request) {
		String name = requiredName(request.name());
		String description = optionalDescription(request.description());
		ensureParent(userId, null, request.parentId());
		ensureNameAvailable(userId, request.parentId(), name, null);
		Category category = new Category();
		category.setUserId(userId);
		category.setName(name);
		category.setDescription(description);
		category.setParentId(request.parentId());
		category.setStatus(CategoryStatus.ACTIVE);
		return CategoryView.from(categories.saveAndFlush(category));
	}

	@Transactional(readOnly = true, noRollbackFor = ApiException.class)
	public CategoryView get(UUID userId, UUID id) {
		return CategoryView.from(visible(userId, id));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public CategoryView update(UUID userId, UUID id, CategoryUpdateRequest request) {
		Category category = visible(userId, id);
		String name = request.hasName() ? requiredName(request.name()) : category.getName();
		String description = request.hasDescription() ? optionalDescription(request.description())
				: category.getDescription();
		UUID parentId = request.hasParentId() ? request.parentId() : category.getParentId();
		if (request.hasParentId()) {
			ensureParent(userId, id, parentId);
		}
		if (request.hasName() || request.hasParentId()) {
			ensureNameAvailable(userId, parentId, name, id);
		}
		category.setName(name);
		category.setDescription(description);
		category.setParentId(parentId);
		return CategoryView.from(categories.saveAndFlush(category));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public CategoryView updateStatus(UUID userId, UUID id, CategoryStatusRequest request) {
		CategoryStatus status = request.status();
		if (status == CategoryStatus.DELETED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Status cannot be set to DELETED");
		}
		Category category = visible(userId, id);
		if (category.getStatus() == status) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Category already has this status");
		}
		if (category.getStatus() == CategoryStatus.ARCHIVED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Archived category cannot change status");
		}
		category.setStatus(status);
		return CategoryView.from(categories.saveAndFlush(category));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public CategoryView delete(UUID userId, UUID id) {
		Category category = visible(userId, id);
		if (categories.existsByUserIdAndParentIdAndStatusNot(userId, id, CategoryStatus.DELETED)) {
			throw new ApiException(HttpStatus.CONFLICT, "Category has child categories");
		}
		category.setStatus(CategoryStatus.DELETED);
		return CategoryView.from(categories.saveAndFlush(category));
	}

	private Category visible(UUID userId, UUID id) {
		return categories.findByIdAndUserIdAndStatusNot(id, userId, CategoryStatus.DELETED)
			.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
	}

	private void ensureParent(UUID userId, UUID categoryId, UUID parentId) {
		if (parentId == null) {
			return;
		}
		if (categories.findByIdAndUserIdAndStatusNot(parentId, userId, CategoryStatus.DELETED).isEmpty()) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Parent category not found");
		}
		UUID cursor = parentId;
		Set<UUID> seen = new HashSet<>();
		while (cursor != null) {
			if ((categoryId != null && cursor.equals(categoryId)) || !seen.add(cursor)) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "Parent category would create a cycle");
			}
			UUID current = cursor;
			cursor = categories.findById(current)
				.filter(category -> userId.equals(category.getUserId()))
				.map(Category::getParentId)
				.orElse(null);
		}
	}

	private void ensureNameAvailable(UUID userId, UUID parentId, String name, UUID excludeId) {
		UUID excluded = excludeId == null ? NO_ROW : excludeId;
		long found = parentId == null ? categories.countRootNames(userId, name, excluded)
				: categories.countChildNames(userId, parentId, name, excluded);
		if (found > 0) {
			throw new ApiException(HttpStatus.CONFLICT, "A category with this name already exists");
		}
	}

	private String requiredName(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Name is required");
		}
		String name = raw.trim();
		if (name.length() > 100) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Name must be at most 100 characters");
		}
		return name;
	}

	private String optionalDescription(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		String description = raw.trim();
		if (description.length() > 500) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Description must be at most 500 characters");
		}
		return description;
	}

}
