package org.joel.kimwanyisacco.service;

import java.util.List;

import org.joel.kimwanyisacco.dto.AuditLogDto;
import org.joel.kimwanyisacco.dto.AuditLogFilterForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;

public interface AuditLogService {

    void record(UserAccount actor, AuditAction action, String entityType, Long entityId, String description);

    List<AuditLogDto> search(AuditLogFilterForm filter);

    List<AuditLogDto> findRecent(int limit);
}
