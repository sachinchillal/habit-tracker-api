package com.addhuri.habit_tracker.category.dto;

import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.UUID;

public class CategoryUpdateRequest {

	private String name;

	private String description;

	private UUID parentId;

	private boolean namePresent;

	private boolean descriptionPresent;

	private boolean parentIdPresent;

	@JsonSetter("name")
	public void setName(String name) {
		this.name = name;
		this.namePresent = true;
	}

	@JsonSetter("description")
	public void setDescription(String description) {
		this.description = description;
		this.descriptionPresent = true;
	}

	@JsonSetter("parentId")
	public void setParentId(UUID parentId) {
		this.parentId = parentId;
		this.parentIdPresent = true;
	}

	public boolean hasName() {
		return namePresent;
	}

	public boolean hasDescription() {
		return descriptionPresent;
	}

	public boolean hasParentId() {
		return parentIdPresent;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public UUID parentId() {
		return parentId;
	}

}
