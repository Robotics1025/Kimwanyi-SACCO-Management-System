package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.common.util.converter.NotificationConverter;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.NotificationRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    private final NotificationConverter notificationConverter = new NotificationConverter();

    private NotificationServiceImpl service() {
        return new NotificationServiceImpl(notificationRepository, userAccountRepository, notificationConverter);
    }

    @Test
    void notifySavesANotificationForTheGivenRecipient() {
        UserAccount recipient = new UserAccount();
        recipient.setUsername("jkamau");

        service().notify(recipient, NotificationType.DEPOSIT, "Deposit Received", "UGX 5000 deposited.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(recipient, saved.getUserAccount());
        assertEquals(NotificationType.DEPOSIT, saved.getType());
        assertEquals("Deposit Received", saved.getTitle());
        assertEquals("UGX 5000 deposited.", saved.getMessage());
    }

    @Test
    void notifyAdminsSendsToEveryAdminAccount() {
        UserAccount admin1 = new UserAccount();
        admin1.setUsername("admin1");
        UserAccount admin2 = new UserAccount();
        admin2.setUsername("admin2");
        when(userAccountRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

        service().notifyAdmins(NotificationType.LOAN_APPLICATION, "New Loan Application", "jkamau applied for UGX 50000");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void getRecentMapsEntitiesToDtos() {
        Notification n = new Notification();
        n.setTitle("Deposit Received");
        n.setMessage("UGX 5000 deposited.");
        n.setType(NotificationType.DEPOSIT);
        when(notificationRepository.findTop15ByUserAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        List<NotificationDto> result = service().getRecent(1L);

        assertEquals(1, result.size());
        assertEquals("Deposit Received", result.get(0).getTitle());
        assertEquals("DEPOSIT", result.get(0).getType());
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByUserAccountIdAndReadStatusFalse(1L)).thenReturn(3L);

        assertEquals(3L, service().getUnreadCount(1L));
    }

    @Test
    void markAsReadSetsReadStatusAndSaves() {
        Notification n = new Notification();
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(n));

        service().markAsRead(9L);

        assertEquals(true, n.isReadStatus());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsReadDelegatesToRepositoryBulkUpdate() {
        service().markAllAsRead(1L);

        verify(notificationRepository).markAllAsRead(1L);
    }
}
