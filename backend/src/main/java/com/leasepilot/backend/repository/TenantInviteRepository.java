package com.leasepilot.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.TenantInvite;

public interface TenantInviteRepository extends JpaRepository<TenantInvite, Long> {
    List<TenantInvite> findByUnitIdOrderByCreatedAt(Long unitId);
    // The invite waiting for this email to sign up (at most one, enforced when inviting)
    Optional<TenantInvite> findFirstByEmailAndAcceptedAtIsNull(String email);
    boolean existsByEmailAndAcceptedAtIsNull(String email);
}
