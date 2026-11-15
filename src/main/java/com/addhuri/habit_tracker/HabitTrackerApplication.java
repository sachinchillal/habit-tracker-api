package com.addhuri.habit_tracker;

import com.addhuri.habit_tracker.security.AuthTokenProperties;
import com.addhuri.habit_tracker.user.WebhookProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({ WebhookProperties.class, AuthTokenProperties.class })
public class HabitTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabitTrackerApplication.class, args);
	}

}
