package org.joel.kimwanyisacco.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.joel.kimwanyisacco.model.AuditLog;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);

    List<AuditLog> findByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
