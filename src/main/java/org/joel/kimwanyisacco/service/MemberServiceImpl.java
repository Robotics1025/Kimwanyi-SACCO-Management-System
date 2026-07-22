package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.util.MembershipNumberGenerator;
import org.joel.kimwanyisacco.common.util.SavingsAccountNumberGenerator;
import org.joel.kimwanyisacco.common.util.converter.MemberConverter;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService {

    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final MemberConverter memberConverter;
    private final PasswordEncoder passwordEncoder;
    private final MembershipNumberGenerator membershipNumberGenerator;
    private final SavingsAccountNumberGenerator savingsAccountNumberGenerator;
    private final AuditLogService auditLogService;

    public MemberServiceImpl(
            UserAccountRepository userAccountRepository,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            MemberConverter memberConverter,
            PasswordEncoder passwordEncoder,
            MembershipNumberGenerator membershipNumberGenerator,
            SavingsAccountNumberGenerator savingsAccountNumberGenerator,
            AuditLogService auditLogService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.memberConverter = memberConverter;
        this.passwordEncoder = passwordEncoder;
        this.membershipNumberGenerator = membershipNumberGenerator;
        this.savingsAccountNumberGenerator = savingsAccountNumberGenerator;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public Member registerMember(MemberRegistrationForm form) {

        validateForm(form);

        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        String nationalId = form.getNationalId().trim();

        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        if (memberRepository.existsByNationalId(nationalId)) {
            throw new IllegalArgumentException(
                    "National ID already exists"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(form.getPassword());

        UserAccount userAccount =
                memberConverter.toUserAccount(
                        form,
                        hashedPassword
                );

        UserAccount savedAccount =
                userAccountRepository.save(userAccount);

        String membershipNumber =
                membershipNumberGenerator.generate();

        Member member =
                memberConverter.toMember(
                        form,
                        savedAccount,
                        membershipNumber
                );

        Member savedMember = memberRepository.save(member);

        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setMember(savedMember);
        savingsAccount.setAccountNumber(savingsAccountNumberGenerator.generate());
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccountRepository.save(savingsAccount);

        auditLogService.record(savedAccount, AuditAction.MEMBER_REGISTERED, "Member", savedMember.getId(), "Membership " + membershipNumber);
        
        return savedMember;
    }

    private void validateForm(MemberRegistrationForm form) {

        if (form == null) {
            throw new IllegalArgumentException(
                    "Registration form is required"
            );
        }

        if (form.getUsername() == null ||
                form.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (form.getPassword() == null ||
                form.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        if (form.getFirstName() == null ||
                form.getFirstName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name is required"
            );
        }

        if (form.getLastName() == null ||
                form.getLastName().isBlank()) {
            throw new IllegalArgumentException(
                    "Last name is required"
            );
        }

        if (form.getEmail() == null ||
                form.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (form.getNationalId() == null ||
                form.getNationalId().isBlank()) {
            throw new IllegalArgumentException(
                    "National ID is required"
            );
        }
    }
}