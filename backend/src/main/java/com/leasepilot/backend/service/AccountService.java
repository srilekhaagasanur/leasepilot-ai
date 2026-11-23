package com.leasepilot.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.model.TenantInvite;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.TenantInviteRepository;

// Creates accounts. Owners can always sign up. Tenants can only sign up with an email an owner
// added to a unit, and are connected to that unit right away.
@Service
public class AccountService {

    private final AppUserRepository userRepository;
    private final TenantInviteRepository inviteRepository;
    private final TenantOnboardingService onboarding;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository userRepository, TenantInviteRepository inviteRepository,
                          TenantOnboardingService onboarding, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.inviteRepository = inviteRepository;
        this.onboarding = onboarding;
        this.passwordEncoder = passwordEncoder;
    }

    // One transaction: if connecting the tenant fails, the account isn't created either
    @Transactional
    public AppUser signUp(String fullName, String email, String password, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        TenantInvite invite = null;
        if (role == Role.TENANT) {
            invite = inviteRepository.findFirstByEmailAndAcceptedAtIsNull(normalizedEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "We couldn't find this email on any unit. Ask your landlord to add you in LeasePilot first."));
        }

        AppUser user = new AppUser(normalizedEmail, fullName.trim(), role);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);

        if (invite != null) {
            onboarding.connect(invite, user);
        }
        return user;
    }
}
