package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.dto.UserResponse;
import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.service.DemoResetService;

// Demo mode: lists the demo accounts for the "Try as..." buttons and resets the demo data.
// Turned off with app.demo-enabled=false.
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final AppUserRepository userRepository;
    private final TenancyRepository tenancyRepository;
    private final DemoResetService demoResetService;
    private final boolean demoEnabled;

    public DemoController(AppUserRepository userRepository, TenancyRepository tenancyRepository,
                          DemoResetService demoResetService, @Value("${app.demo-enabled}") boolean demoEnabled) {
        this.userRepository = userRepository;
        this.tenancyRepository = tenancyRepository;
        this.demoResetService = demoResetService;
        this.demoEnabled = demoEnabled;
    }

    // GET /api/demo/users -> the demo accounts (empty when demo mode is off)
    @GetMapping("/users")
    public List<UserResponse> users() {
        if (!demoEnabled) {
            return List.of();
        }
        return userRepository.findAll().stream()
                .filter(AppUser::isDemo)
                .map(u -> UserResponse.from(u, tenancyRepository.findByTenantId(u.getId()).stream().findFirst().orElse(null)))
                .toList();
    }

    // POST /api/demo/reset
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        if (!demoEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        demoResetService.reset();
    }
}
