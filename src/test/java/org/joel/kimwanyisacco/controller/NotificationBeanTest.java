package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationBeanTest {

    @Mock private NotificationService notificationService;
    @Mock private UserSessionBean userSessionBean;

    private NotificationDto notificationOfType(String type) {
        NotificationDto dto = new NotificationDto();
        dto.setType(type);
        return dto;
    }

    private void loggedInAs(long userAccountId) {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccountId);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);
    }

    @Test
    void initLoadsRecentNotificationsAndUnreadCount() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of(notificationOfType("DEPOSIT")));
        when(notificationService.getUnreadCount(1L)).thenReturn(2L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();

        assertEquals(1, bean.getRecent().size());
        assertEquals(2L, bean.getUnreadCount());
    }

    @Test
    void markAsReadDelegatesAndReloads() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of());
        when(notificationService.getUnreadCount(1L)).thenReturn(0L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();
        bean.markAsRead(9L);

        verify(notificationService).markAsRead(9L);
    }

    @Test
    void markAllAsReadDelegatesAndReloads() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of());
        when(notificationService.getUnreadCount(1L)).thenReturn(0L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();
        bean.markAllAsRead();

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    void iconClassMapsEachNotificationType() {
        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);

        assertEquals("fa-file-signature", bean.iconClass(notificationOfType("LOAN_APPLICATION")));
        assertEquals("fa-circle-check", bean.iconClass(notificationOfType("LOAN_APPROVED")));
        assertEquals("fa-circle-xmark", bean.iconClass(notificationOfType("LOAN_REJECTED")));
        assertEquals("fa-hand-holding-dollar", bean.iconClass(notificationOfType("LOAN_REPAYMENT")));
        assertEquals("fa-arrow-down", bean.iconClass(notificationOfType("DEPOSIT")));
        assertEquals("fa-arrow-up", bean.iconClass(notificationOfType("WITHDRAWAL")));
        assertEquals("fa-circle-info", bean.iconClass(notificationOfType("SYSTEM")));
    }
}
