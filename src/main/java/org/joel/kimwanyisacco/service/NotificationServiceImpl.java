package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.NotificationConverter;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.NotificationRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationConverter notificationConverter;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository,
            NotificationConverter notificationConverter
    ) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationConverter = notificationConverter;
    }

    @Override
    @Transactional
    public void notify(UserAccount recipient, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserAccount(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message) {
        List<UserAccount> admins = userAccountRepository.findByRole(Role.ADMIN);
        for (UserAccount admin : admins) {
            notify(admin, type, title, message);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getRecent(Long userAccountId) {
        return notificationRepository.findTop15ByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(notificationConverter::toDto)
                .toList();
    }

    @Override
    public long getUnreadCount(Long userAccountId) {
        return notificationRepository.countByUserAccountIdAndReadStatusFalse(userAccountId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userAccountId) {
        notificationRepository.markAllAsRead(userAccountId);
    }
}
