package com.addhuri.habit_tracker.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String message, T data, List<FieldError> errors) {

	public record FieldError(String field, String message) {
	}

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>("OK", data, null);
	}

	public static <T> ApiResponse<T> ok(String message, T data) {
		return new ApiResponse<>(message, data, null);
	}

	public static <T> ApiResponse<T> fail(String message) {
		return new ApiResponse<>(message, null, null);
	}

	public static <T> ApiResponse<T> fail(String message, List<FieldError> errors) {
		return new ApiResponse<>(message, null, errors);
	}

}
