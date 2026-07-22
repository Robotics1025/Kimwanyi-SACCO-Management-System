package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AuthenticationServiceImpl(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        UserAccount userAccount = userAccountRepository.findByUsername(loginForm.getUsername())
                .orElse(null);
                
        if (userAccount == null) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!userAccount.isEnabled()) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(loginForm.getPassword(), userAccount.getPasswordHash())) {
            auditLogService.record(null, AuditAction.LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername());
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        auditLogService.record(userAccount, AuditAction.LOGIN_SUCCESS, "UserAccount", userAccount.getId(), null);

        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccount.getId());
        dto.setUsername(userAccount.getUsername());
        dto.setRoles(List.of(userAccount.getRole().name()));
        return dto;
    }
}
