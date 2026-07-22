package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.AuditLogDto;
import org.joel.kimwanyisacco.model.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogConverter {

    public AuditLogDto toDto(AuditLog log) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(log.getId());
        dto.setActorUsername(
                log.getUserAccount() != null
                        ? log.getUserAccount().getUsername()
                        : null
        );
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setDescription(log.getDescription());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
