package com.addhuri.habit_tracker.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.addhuri.habit_tracker.category.entity.Category;
import com.addhuri.habit_tracker.category.entity.CategoryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	List<Category> findByUserIdAndStatusNotOrderByNameAscIdAsc(UUID userId, CategoryStatus status);

	Optional<Category> findByIdAndUserIdAndStatusNot(UUID id, UUID userId, CategoryStatus status);

	boolean existsByUserIdAndParentIdAndStatusNot(UUID userId, UUID parentId, CategoryStatus status);

	@Query("""
			select count(c) from Category c
			where c.userId = :userId
			  and c.parentId is null
			  and trim(both from c.name) = :name
			  and c.id <> :excludeId
			""")
	long countRootNames(@Param("userId") UUID userId, @Param("name") String name, @Param("excludeId") UUID excludeId);

	@Query("""
			select count(c) from Category c
			where c.userId = :userId
			  and c.parentId = :parentId
			  and trim(both from c.name) = :name
			  and c.id <> :excludeId
			""")
	long countChildNames(@Param("userId") UUID userId, @Param("parentId") UUID parentId, @Param("name") String name,
			@Param("excludeId") UUID excludeId);

}
