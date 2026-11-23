package com.leasepilot.backend.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.leasepilot.backend.model.AppUser;
import com.leasepilot.backend.model.Role;

// The logged-in user, as Spring Security keeps it in the session.
// Controllers get it with @AuthenticationPrincipal, so they never have to trust an id sent by the browser.
public record AppUserPrincipal(Long id, String email, String passwordHash, Role role) implements UserDetails {

    public static AppUserPrincipal of(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getRole());
    }

    // The copy kept in the session doesn't need the password hash
    public AppUserPrincipal withoutPassword() {
        return new AppUserPrincipal(id, email, null, role);
    }

    // Spring Security checks roles as "ROLE_MANAGER" / "ROLE_TENANT"
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
