package org.joel.kimwanyisacco.service;

import java.util.List;
import java.util.stream.Collectors;

import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.UserAccountConverter;
import org.joel.kimwanyisacco.dto.UserAccountDto;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementServiceImpl implements UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final UserAccountConverter userAccountConverter;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementServiceImpl(UserAccountRepository userAccountRepository,
                                     UserAccountConverter userAccountConverter,
                                     AuditLogService auditLogService,
                                     PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountConverter = userAccountConverter;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserAccountDto> search(String keyword) {
        List<UserAccount> results;
        if (keyword == null || keyword.isBlank()) {
            results = userAccountRepository.findAll();
        } else {
            results = userAccountRepository.search(keyword.trim());
        }
        return results.stream()
                .map(userAccountConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setEnabled(Long userId, boolean enabled, UserAccount adminAccount) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        if (account.isEnabled() == enabled) {
            return; // No-op
        }

        account.setEnabled(enabled);
        userAccountRepository.save(account);

        AuditAction action = enabled ? AuditAction.USER_ACCOUNT_ENABLED : AuditAction.USER_ACCOUNT_DISABLED;
        auditLogService.record(adminAccount, action, "UserAccount", userId, null);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword, UserAccount adminAccount) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);

        auditLogService.record(adminAccount, AuditAction.PASSWORD_CHANGED, "UserAccount", userId, "Reset by admin");
    }
}
