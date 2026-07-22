package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

    public NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType() != null ? notification.getType().name() : null);
        dto.setReadStatus(notification.isReadStatus());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
