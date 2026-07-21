package org.joel.kimwanyisacco.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.joel.kimwanyisacco.common.util.converter.AuditLogConverter;
import org.joel.kimwanyisacco.dto.AuditLogDto;
import org.joel.kimwanyisacco.dto.AuditLogFilterForm;
import org.joel.kimwanyisacco.model.AuditLog;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogConverter auditLogConverter;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               AuditLogConverter auditLogConverter) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogConverter = auditLogConverter;
    }

    @Override
    @Transactional
    public void record(UserAccount actor, AuditAction action,
                       String entityType, Long entityId, String description) {
        AuditLog log = new AuditLog();
        log.setUserAccount(actor);
        log.setAction(action);
        log.setEntityType(entityType != null ? entityType : "");
        log.setEntityId(entityId);
        log.setDescription(description);
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> search(AuditLogFilterForm filter) {
        // Fetch all newest-first and filter in memory to keep it simple
        // (volume is low per spec — no pagination needed)
        List<AuditLog> logs = auditLogRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        return logs.stream()
                .filter(l -> filter.getAction() == null || l.getAction() == filter.getAction())
                .filter(l -> {
                    if (filter.getUsername() == null || filter.getUsername().isBlank()) return true;
                    String kw = filter.getUsername().toLowerCase();
                    return l.getUserAccount() != null
                            && l.getUserAccount().getUsername().toLowerCase().contains(kw);
                })
                .filter(l -> {
                    if (filter.getDateFrom() == null) return true;
                    return l.getCreatedAt() != null
                            && !l.getCreatedAt().toLocalDate().isBefore(filter.getDateFrom());
                })
                .filter(l -> {
                    if (filter.getDateTo() == null) return true;
                    return l.getCreatedAt() != null
                            && !l.getCreatedAt().toLocalDate().isAfter(filter.getDateTo());
                })
                .map(auditLogConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> findRecent(int limit) {
        return auditLogRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .map(auditLogConverter::toDto)
                .collect(Collectors.toList());
    }
}
