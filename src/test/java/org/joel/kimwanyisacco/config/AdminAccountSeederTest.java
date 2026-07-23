package org.joel.kimwanyisacco.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountSeederTest {

    @Test
    void doesNotCreateAnAdminWithoutAnEnvironmentPassword() {
        UserAccountRepository repository = org.mockito.Mockito.mock(UserAccountRepository.class);
        PasswordEncoder encoder = org.mockito.Mockito.mock(PasswordEncoder.class);

        new AdminAccountSeeder(repository, encoder, "admin", "", "admin@example.com").seedAdmin();

        verify(repository, never()).save(any(UserAccount.class));
    }

    @Test
    void createsAdminFromConfiguredValuesWithoutLoggingThePassword() {
        UserAccountRepository repository = org.mockito.Mockito.mock(UserAccountRepository.class);
        PasswordEncoder encoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        when(repository.existsByUsername("sacco-admin")).thenReturn(false);
        when(encoder.encode("strong-secret")).thenReturn("bcrypt-hash");

        new AdminAccountSeeder(repository, encoder, "sacco-admin", "strong-secret", "ops@example.com").seedAdmin();

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(captor.capture());
        UserAccount saved = captor.getValue();
        assertEquals("sacco-admin", saved.getUsername());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertEquals("ops@example.com", saved.getEmail());
        assertEquals(Role.ADMIN, saved.getRole());
    }
}
