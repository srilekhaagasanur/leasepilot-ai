package com.leasepilot.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.leasepilot.backend.model.ApprovalStatus;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.Tenancy;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.service.DemoResetService;

// Lists the hard-coded demo users so the frontend can offer "view as..." buttons (there's no login),
// and resets the demo data
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final AppUserRepository userRepository;
    private final TenancyRepository tenancyRepository;
    private final DemoResetService demoResetService;

    public DemoController(AppUserRepository userRepository, TenancyRepository tenancyRepository,
                          DemoResetService demoResetService) {
        this.userRepository = userRepository;
        this.tenancyRepository = tenancyRepository;
        this.demoResetService = demoResetService;
    }

    public record DemoUser(Long id, String fullName, Role role, String unitNumber, ApprovalStatus tenancyStatus) {
    }

    // GET /api/demo/users
    @GetMapping("/users")
    public List<DemoUser> users() {
        return userRepository.findAll().stream()
                .map(u -> {
                    Tenancy tenancy = tenancyRepository.findByTenantId(u.getId()).stream().findFirst().orElse(null);
                    return new DemoUser(u.getId(), u.getFullName(), u.getRole(),
                            tenancy != null ? tenancy.getUnit().getUnitNumber() : null,
                            tenancy != null ? tenancy.getStatus() : null);
                })
                .toList();
    }

    // POST /api/demo/reset
    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        demoResetService.reset();
    }
}
