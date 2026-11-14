package com.addhuri.habit_tracker.user.controller;

import com.addhuri.habit_tracker.api.ApiException;
import com.addhuri.habit_tracker.api.ApiResponse;
import com.addhuri.habit_tracker.user.UserSyncService;
import com.addhuri.habit_tracker.user.WebhookProperties;
import com.addhuri.habit_tracker.user.dto.UserSyncRequest;
import com.addhuri.habit_tracker.user.dto.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class UserWebhookController {

	public static final String SECRET_HEADER = "X-Webhook-Secret";

	private final WebhookProperties properties;

	private final UserSyncService userSyncService;

	public UserWebhookController(WebhookProperties properties, UserSyncService userSyncService) {
		this.properties = properties;
		this.userSyncService = userSyncService;
	}

	@PostMapping("/users")
	public ApiResponse<UserView> sync(
			@RequestHeader(value = SECRET_HEADER, required = false) String secret,
			@Valid @RequestBody UserSyncRequest request) {
		if (!properties.matches(secret)) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
		}
		return ApiResponse.ok("User updated", userSyncService.upsert(request.id(), request.accountStatus()));
	}

}
