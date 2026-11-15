package com.addhuri.habit_tracker.task.entity;

public enum TaskStatus {
	ACTIVE, // offered for use. Default for a new row.
	INACTIVE, // temporarily hidden; it can return to ACTIVE. like pause and unpause.
	ARCHIVED, // retired and kept for history; not offered again.
	DELETED // permanently deleted; no longer available. soft-deleted. The row stays so a
					// later foreign key does not dangle.
}
