package com.safrapay.backend.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "app", "SafraPay backend",
                "status", "ok",
                "timestamp", Instant.now().toString()
        );
    }

}
