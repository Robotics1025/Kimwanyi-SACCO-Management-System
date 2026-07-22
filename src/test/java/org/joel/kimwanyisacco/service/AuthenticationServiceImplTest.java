package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(userAccountRepository, passwordEncoder, auditLogService);
    }

    private UserAccount buildEnabledAccount() {
        UserAccount account = new UserAccount();
        account.setUsername("jmugole");
        account.setPasswordHash("hashed-password");
        account.setEmail("joel@example.com");
        account.setFirstName("Joel");
        account.setLastName("Mugole");
        account.setRole(Role.MEMBER);
        account.setEnabled(true);
        return account;
    }

    private LoginForm formFor(String username, String password) {
        LoginForm form = new LoginForm();
        form.setUsername(username);
        form.setPassword(password);
        return form;
    }

    @Test
    void authenticateReturnsLoggedInUserForValidCredentials() {
        UserAccount account = buildEnabledAccount();
        LoginForm form = formFor("jmugole", "correct-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);

        LoggedInUserDto dto = authenticationService.authenticate(form);

        assertEquals("jmugole", dto.getUsername());
        assertEquals(List.of("MEMBER"), dto.getRoles());
    }

    @Test
    void authenticateThrowsWhenUsernameNotFound() {
        LoginForm form = formFor("unknown", "whatever");
        when(userAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }

    @Test
    void authenticateThrowsWhenPasswordDoesNotMatch() {
        UserAccount account = buildEnabledAccount();
        LoginForm form = formFor("jmugole", "wrong-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }

    @Test
    void authenticateThrowsWhenAccountDisabled() {
        UserAccount account = buildEnabledAccount();
        account.setEnabled(false);
        LoginForm form = formFor("jmugole", "correct-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }
}
