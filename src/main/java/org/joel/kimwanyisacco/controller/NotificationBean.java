package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("notificationBean")
@RequestScope
public class NotificationBean {

    private final NotificationService notificationService;
    private final UserSessionBean userSessionBean;

    private List<NotificationDto> recent = List.of();
    private long unreadCount;

    public NotificationBean(NotificationService notificationService, UserSessionBean userSessionBean) {
        this.notificationService = notificationService;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        Long userAccountId = userSessionBean.getLoggedInUser().getId();
        recent = notificationService.getRecent(userAccountId);
        unreadCount = notificationService.getUnreadCount(userAccountId);
    }

    public void markAsRead(Long notificationId) {
        notificationService.markAsRead(notificationId);
        init();
    }

    public void markAllAsRead() {
        notificationService.markAllAsRead(userSessionBean.getLoggedInUser().getId());
        init();
    }

    public String iconClass(NotificationDto notification) {
        if (notification.getType() == null) {
            return "fa-circle-info";
        }
        return switch (notification.getType()) {
            case "LOAN_APPLICATION" -> "fa-file-signature";
            case "LOAN_APPROVED" -> "fa-circle-check";
            case "LOAN_REJECTED" -> "fa-circle-xmark";
            case "LOAN_REPAYMENT" -> "fa-hand-holding-dollar";
            case "DEPOSIT" -> "fa-arrow-down";
            case "WITHDRAWAL" -> "fa-arrow-up";
            default -> "fa-circle-info";
        };
    }

    public List<NotificationDto> getRecent() {
        return recent;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
