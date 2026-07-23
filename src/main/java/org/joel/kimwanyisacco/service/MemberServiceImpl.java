package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.util.MembershipNumberGenerator;
import org.joel.kimwanyisacco.common.util.SavingsAccountNumberGenerator;
import org.joel.kimwanyisacco.common.util.converter.MemberConverter;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.dto.MemberUpdateForm;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import java.util.List;
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
    private final NotificationService notificationService;
    private final EmailService emailService;

    public MemberServiceImpl(
            UserAccountRepository userAccountRepository,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            MemberConverter memberConverter,
            PasswordEncoder passwordEncoder,
            MembershipNumberGenerator membershipNumberGenerator,
            SavingsAccountNumberGenerator savingsAccountNumberGenerator,
            AuditLogService auditLogService,
            NotificationService notificationService,
            EmailService emailService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.memberConverter = memberConverter;
        this.passwordEncoder = passwordEncoder;
        this.membershipNumberGenerator = membershipNumberGenerator;
        this.savingsAccountNumberGenerator = savingsAccountNumberGenerator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public Member registerMember(MemberRegistrationForm form) {

        validateForm(form);

        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        String nationalId = form.getNationalId().trim();

        if (userAccountRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
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
        notificationService.notifyAdmins(org.joel.kimwanyisacco.model.enums.NotificationType.SYSTEM,
                "Member approval required",
                savedAccount.getFirstName() + " " + savedAccount.getLastName()
                        + " registered as " + membershipNumber + ". Review and approve the account.");
        try {
            emailService.sendToMembers(List.of(savedMember.getId()),
                    "Kimwanyi SACCO registration received",
                    "Hello " + savedAccount.getFirstName() + ",\n\n"
                            + "We have received your application to become a member of Kimwanyi SACCO. "
                            + "Your membership number is " + membershipNumber + ".\n\n"
                            + "Your account is currently waiting for verification and approval by an administrator. "
                            + "You will not be able to log in until the approval is complete. "
                            + "We will email you again when your account has been approved.\n\n"
                            + "Kimwanyi SACCO",
                    savedAccount);
        } catch (RuntimeException emailFailure) {
            // Registration remains successful even when email configuration or delivery is unavailable.
            auditLogService.record(savedAccount, AuditAction.EMAIL_FAILED, "Member", savedMember.getId(),
                    "Registration acknowledgement could not be sent: " + emailFailure.getMessage());
        }
        
        return savedMember;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDto getByUserAccountId(Long userAccountId) {
        Member member = memberRepository.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        return memberConverter.toDto(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberDto> search(String keyword) {
        List<Member> members = keyword == null || keyword.isBlank()
                ? memberRepository.findAllWithUserAccount()
                : memberRepository.search(keyword.trim());
        return members.stream().map(memberConverter::toDto).toList();
    }

    @Override
    @Transactional
    public MemberDto updateProfile(Long userAccountId, MemberUpdateForm form) {
        if (form == null || form.getEmail() == null || form.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        Member member = memberRepository.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        UserAccount account = member.getUserAccount();
        String email = form.getEmail().trim().toLowerCase();
        if (!email.equalsIgnoreCase(account.getEmail()) && userAccountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        account.setEmail(email);
        member.setPhoneNumber(form.getPhoneNumber() == null || form.getPhoneNumber().isBlank()
                ? null : form.getPhoneNumber().trim());
        userAccountRepository.save(account);
        memberRepository.save(member);
        auditLogService.record(account, AuditAction.MEMBER_UPDATED, "Member", member.getId(), "Contact profile updated");
        return memberConverter.toDto(member);
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
