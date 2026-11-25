package com.leasepilot.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.leasepilot.backend.model.WorkOrderUpdate;

public interface WorkOrderUpdateRepository extends JpaRepository<WorkOrderUpdate, Long> {
    // A request's timeline, oldest first (with each author's name loaded)
    @EntityGraph(attributePaths = {"author"})
    List<WorkOrderUpdate> findByRequestIdOrderByCreatedAt(Long requestId);
}
