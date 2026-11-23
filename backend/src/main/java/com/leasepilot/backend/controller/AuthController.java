package com.leasepilot.backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.dto.UserResponse;
import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.Role;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.TenancyRepository;
import com.leasepilot.backend.security.AppUserPrincipal;
import com.leasepilot.backend.service.AccountService;
import com.leasepilot.backend.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Sign up, log in, and "who am I?". Logging out is handled by Spring Security (POST /api/auth/logout).
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final TenancyRepository tenancyRepository;
    private final AccountService accountService;
    private final PasswordResetService passwordResetService;
    private final boolean demoEnabled;
    // Keeps the logged-in user in the HTTP session, so later requests know who it is
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, AppUserRepository userRepository,
                          TenancyRepository tenancyRepository, AccountService accountService,
                          PasswordResetService passwordResetService,
                          @Value("${app.demo-enabled}") boolean demoEnabled) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tenancyRepository = tenancyRepository;
        this.accountService = accountService;
        this.passwordResetService = passwordResetService;
        this.demoEnabled = demoEnabled;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record SignupRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt only looks at the first 72 bytes
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password,
            // MANAGER (property owner) or TENANT
            @NotNull Role role) {}

    public record DemoLoginRequest(@NotBlank String email) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String password) {}

    // GET /api/auth/me -> the logged-in user, or 401 if nobody is logged in.
    // The frontend calls this first, which also hands it the CSRF cookie it needs for POSTs.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AppUserPrincipal me, CsrfToken csrfToken) {
        csrfToken.getToken();
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return userRepository.findById(me.id())
                .map(user -> ResponseEntity.ok(describe(user)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // POST /api/auth/login   body: {"email": "...", "password": "..."}
    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                              HttpServletRequest request, HttpServletResponse response) {
        String email = normalize(body.email());
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
        } catch (AuthenticationException e) {
            // Same message whether the email or the password was wrong, so nobody can probe which emails exist
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        AppUser user = userRepository.findByEmail(email).orElseThrow();
        startSession(user, request, response);
        return describe(user);
    }

    // POST /api/auth/signup   body: {"fullName": "...", "email": "...", "password": "...", "role": "MANAGER" | "TENANT"}
    // Tenants can only sign up with an email their landlord added to a unit.
    @PostMapping("/signup")
    public UserResponse signup(@Valid @RequestBody SignupRequest body,
                               HttpServletRequest request, HttpServletResponse response) {
        AppUser user = accountService.signUp(body.fullName(), body.email(), body.password(), body.role());
        startSession(user, request, response);
        return describe(user);
    }

    // POST /api/auth/demo-login   body: {"email": "priya.shah@example.com"}
    // Logs into a demo account without a password ("Try as..." buttons). Only works for demo accounts.
    @PostMapping("/demo-login")
    public UserResponse demoLogin(@Valid @RequestBody DemoLoginRequest body,
                                  HttpServletRequest request, HttpServletResponse response) {
        AppUser user = userRepository.findByEmail(normalize(body.email()))
                .filter(u -> demoEnabled && u.isDemo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not a demo account"));
        startSession(user, request, response);
        return describe(user);
    }

    // POST /api/auth/forgot-password   body: {"email": "..."}
    // Always answers the same way, whether or not the email has an account
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        passwordResetService.requestReset(body.email());
        return Map.of("message", "If an account exists for that email, we've sent a link to reset the password.");
    }

    // POST /api/auth/reset-password   body: {"token": "<from the emailed link>", "password": "new password"}
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        passwordResetService.resetPassword(body.token(), body.password());
    }

    private void startSession(AppUser user, HttpServletRequest request, HttpServletResponse response) {
        // New session id on login, so an id someone planted before login is useless afterwards
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        AppUserPrincipal principal = AppUserPrincipal.of(user).withoutPassword();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    private UserResponse describe(AppUser user) {
        return UserResponse.from(user, tenancyRepository.findByTenantId(user.getId()).stream().findFirst().orElse(null));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
