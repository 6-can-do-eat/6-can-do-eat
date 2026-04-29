package com.team6.backend.ai.domain.repository;

import com.team6.backend.ai.domain.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, UUID> {
}
