package com.addhuri.habit_tracker.controller;

import com.addhuri.habit_tracker.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class App {
    record Point(int x, int y) {
    }

    @GetMapping()
    public ApiResponse<Point> getPoints() {
        return ApiResponse.ok(new Point(1, 1));
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }
}
