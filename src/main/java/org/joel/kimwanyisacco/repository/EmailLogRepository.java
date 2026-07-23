package org.joel.kimwanyisacco.repository;

import org.joel.kimwanyisacco.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
