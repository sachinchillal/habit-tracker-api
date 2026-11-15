package com.addhuri.habit_tracker.security;

import com.addhuri.habit_tracker.user.entity.AccountStatus;
import com.addhuri.habit_tracker.user.entity.User;
import com.addhuri.habit_tracker.user.repository.UserRepository;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ActiveUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UserRepository users;

	public ActiveUserJwtConverter(UserRepository users) {
		this.users = users;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		} catch (IllegalArgumentException ex) {
			throw new InvalidBearerTokenException("Authentication required", ex);
		}
		User user = users.findById(userId).orElse(null);
		if (user == null || user.getAccountStatus() != AccountStatus.ACTIVE) {
			throw new InvalidBearerTokenException("Authentication required");
		}
		return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER")), userId.toString());
	}

}
