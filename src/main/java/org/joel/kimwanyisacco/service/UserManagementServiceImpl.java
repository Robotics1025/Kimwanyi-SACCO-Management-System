package org.joel.kimwanyisacco.service;

import java.util.List;
import java.util.stream.Collectors;

import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.UserAccountConverter;
import org.joel.kimwanyisacco.dto.UserAccountDto;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    public UserManagementServiceImpl(UserAccountRepository userAccountRepository,
                                     UserAccountConverter userAccountConverter,
                                     AuditLogService auditLogService,
                                     PasswordEncoder passwordEncoder,
                                     MemberRepository memberRepository,
                                     EmailService emailService) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountConverter = userAccountConverter;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.emailService = emailService;
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
                .map(account -> {
                    UserAccountDto dto = userAccountConverter.toDto(account);
                    if (account.getRole() == Role.MEMBER) {
                        memberRepository.findByUserAccountId(account.getId())
                                .ifPresent(member -> dto.setMemberStatus(member.getStatus().name()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void setEnabled(Long userId, boolean enabled, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        if (!enabled && (account == adminAccount
                || (account.getId() != null && account.getId().equals(adminAccount.getId())))) {
            throw new IllegalArgumentException("An administrator cannot disable their own account");
        }

        if (account.isEnabled() == enabled) {
            return; // No-op
        }

        account.setEnabled(enabled);
        userAccountRepository.save(account);

        if (account.getRole() == Role.MEMBER) {
            memberRepository.findByUserAccountId(account.getId()).ifPresent(member -> {
                member.setStatus(enabled ? MemberStatus.ACTIVE : MemberStatus.INACTIVE);
                memberRepository.save(member);
            });
        }

        AuditAction action = enabled ? AuditAction.USER_ACCOUNT_ENABLED : AuditAction.USER_ACCOUNT_DISABLED;
        auditLogService.record(adminAccount, action, "UserAccount", userId, null);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserAccount not found with ID: " + userId));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        userAccountRepository.save(account);

        auditLogService.record(adminAccount, AuditAction.PASSWORD_CHANGED, "UserAccount", userId, "Reset by admin");
    }

    @Override
    @Transactional
    public void approveMember(Long userId, UserAccount adminAccount) {
        requireEnabledAdmin(adminAccount);
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));
        if (account.getRole() != Role.MEMBER) {
            throw new IllegalArgumentException("Only member accounts can be approved");
        }
        var member = memberRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        if (member.getStatus() != MemberStatus.PENDING) {
            throw new IllegalArgumentException("This member application is not pending approval");
        }
        member.setStatus(MemberStatus.ACTIVE);
        account.setEnabled(true);
        memberRepository.save(member);
        userAccountRepository.save(account);
        auditLogService.record(adminAccount, AuditAction.MEMBER_APPROVED, "Member", member.getId(),
                "Approved membership " + member.getMembershipNumber());
        try {
            emailService.sendToMembers(List.of(member.getId()),
                    "Your Kimwanyi SACCO account has been approved",
                    "Hello " + account.getFirstName() + ",\n\n"
                            + "Your Kimwanyi SACCO membership has been verified and approved. "
                            + "You can now log in using your registered username.\n\n"
                            + "Membership number: " + member.getMembershipNumber() + "\n\n"
                            + "Welcome to Kimwanyi SACCO.",
                    adminAccount);
        } catch (RuntimeException emailFailure) {
            auditLogService.record(adminAccount, AuditAction.EMAIL_FAILED, "Member", member.getId(),
                    "Approval email could not be sent: " + emailFailure.getMessage());
        }
    }

    private void requireEnabledAdmin(UserAccount account) {
        if (account == null || account.getRole() != Role.ADMIN || !account.isEnabled()) {
            throw new SecurityException("An enabled administrator account is required");
        }
    }
}
