package com.leasepilot.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.PasswordResetToken;
import com.leasepilot.backend.repository.AppUserRepository;
import com.leasepilot.backend.repository.PasswordResetTokenRepository;

// "Forgot password": emails a one-time link that lets the user choose a new password.
// - The link holds a long random token; the database stores only its SHA-256 hash.
// - A link works once and expires after 30 minutes. Asking again replaces the old link.
// - Demo accounts have no password, so they never get a link.
@Service
public class PasswordResetService {

    private static final Duration LINK_LIFETIME = Duration.ofMinutes(30);
    // Ignore repeat requests for the same account within a minute (stops someone flooding an inbox)
    private static final Duration MIN_TIME_BETWEEN_EMAILS = Duration.ofMinutes(1);

    private final SecureRandom random = new SecureRandom();
    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public PasswordResetService(AppUserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, EmailService emailService,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    // Does nothing visible if the email has no account, so the form can't be used to find out who has one
    @Transactional
    public void requestReset(String email) {
        AppUser user = userRepository.findByEmail(email.trim().toLowerCase())
                .filter(u -> !u.isDemo() && u.getPasswordHash() != null)
                .orElse(null);
        if (user == null) {
            return;
        }
        List<PasswordResetToken> existing = tokenRepository.findByUserId(user.getId());
        boolean sentRecently = existing.stream()
                .anyMatch(t -> t.getCreatedAt().isAfter(Instant.now().minus(MIN_TIME_BETWEEN_EMAILS)));
        if (sentRecently) {
            return;
        }
        tokenRepository.deleteAll(existing);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new PasswordResetToken(user, sha256(token), Instant.now().plus(LINK_LIFETIME)));

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.send(user.getEmail(), "Reset your LeasePilot password", """
                Hi %s,

                Someone asked to reset the password for your LeasePilot account.
                To choose a new password, open this link (it works once and expires in 30 minutes):

                %s

                If this wasn't you, you can ignore this email. Your password stays the same.
                """.formatted(user.getFullName(), link));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(sha256(token))
                .filter(t -> !t.isExpired())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This reset link is invalid or has expired. Please request a new one."));
        AppUser user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Used up: delete this link and any other open links for the account
        tokenRepository.deleteAll(tokenRepository.findByUserId(user.getId()));
    }

    private static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
