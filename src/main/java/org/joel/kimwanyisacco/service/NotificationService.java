package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;

public interface NotificationService {

    void notify(UserAccount recipient, NotificationType type, String title, String message);

    void notifyAdmins(NotificationType type, String title, String message);

    List<NotificationDto> getRecent(Long userAccountId);

    long getUnreadCount(Long userAccountId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userAccountId);
}
