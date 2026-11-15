package com.addhuri.habit_tracker.completion.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.addhuri.habit_tracker.completion.entity.Completion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompletionRepository extends JpaRepository<Completion, UUID> {

	Optional<Completion> findFirstByTaskIdOrderByMarkedAtDesc(UUID taskId);

	long countByTaskIdAndMarkedOn(UUID taskId, LocalDate markedOn);

	List<Completion> findByTaskIdInAndMarkedOn(Collection<UUID> taskIds, LocalDate markedOn);

}
