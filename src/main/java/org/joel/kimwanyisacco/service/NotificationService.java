package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;

public interface NotificationService {
    void notify(UserAccount recipient, NotificationType type, String title, String message);
}
