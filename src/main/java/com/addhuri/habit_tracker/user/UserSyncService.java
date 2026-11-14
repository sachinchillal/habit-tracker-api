package com.addhuri.habit_tracker.user;

import com.addhuri.habit_tracker.user.dto.UserView;
import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserSyncService {

	private final UserRepository users;

	public UserSyncService(UserRepository users) {
		this.users = users;
	}

	@Transactional
	public UserView upsert(UUID id, AccountStatus accountStatus) {
		Instant now = Instant.now();
		User user = users.findById(id).orElse(null);
		if (user == null) {
			user = new User();
			user.setId(id);
			user.setAccountStatus(accountStatus);
			user.setCreatedAt(now);
			user.setUpdatedAt(now);
			users.save(user);
		} else {
			user.setAccountStatus(accountStatus);
			user.setUpdatedAt(now);
		}
		return new UserView(user.getId(), user.getAccountStatus(), user.getCreatedAt(), user.getUpdatedAt());
	}

}
