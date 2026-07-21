package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.util.MembershipNumberGenerator;
import org.joel.kimwanyisacco.common.util.SavingsAccountNumberGenerator;
import org.joel.kimwanyisacco.common.util.converter.MemberConverter;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private MemberConverter memberConverter;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MembershipNumberGenerator membershipNumberGenerator;
    @Mock private SavingsAccountNumberGenerator savingsAccountNumberGenerator;
    @Mock private AuditLogService auditLogService;

    private MemberServiceImpl memberService;
    private MemberRegistrationForm form;

    @BeforeEach
    void setUp() {
        memberService = new MemberServiceImpl(
                userAccountRepository, memberRepository, savingsAccountRepository,
                memberConverter, passwordEncoder, membershipNumberGenerator,
                savingsAccountNumberGenerator, auditLogService);

        form = new MemberRegistrationForm();
        form.setUsername("jkamau");
        form.setPassword("secret123");
        form.setFirstName("John");
        form.setLastName("Kamau");
        form.setEmail("john@example.com");
        form.setNationalId("CM12345");
    }

    @Test
    void registerMemberCreatesZeroBalanceSavingsAccountForNewMember() {
        UserAccount userAccount = new UserAccount();
        UserAccount savedUserAccount = new UserAccount();
        savedUserAccount.setUsername("jkamau");
        Member member = new Member();
        Member savedMember = new Member();
        savedMember.setId(7L);

        when(userAccountRepository.existsByUsername("jkamau")).thenReturn(false);
        when(userAccountRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(memberRepository.existsByNationalId("CM12345")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(memberConverter.toUserAccount(form, "hashed")).thenReturn(userAccount);
        when(userAccountRepository.save(userAccount)).thenReturn(savedUserAccount);
        when(membershipNumberGenerator.generate()).thenReturn("KIM-2026-0001");
        when(memberConverter.toMember(form, savedUserAccount, "KIM-2026-0001")).thenReturn(member);
        when(memberRepository.save(member)).thenReturn(savedMember);
        when(savingsAccountNumberGenerator.generate()).thenReturn("SAV-2026-0001");
        when(savingsAccountRepository.save(any(SavingsAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Member result = memberService.registerMember(form);

        assertEquals(savedMember, result);

        ArgumentCaptor<SavingsAccount> captor = ArgumentCaptor.forClass(SavingsAccount.class);
        verify(savingsAccountRepository).save(captor.capture());
        SavingsAccount createdAccount = captor.getValue();
        assertEquals(savedMember, createdAccount.getMember());
        assertEquals("SAV-2026-0001", createdAccount.getAccountNumber());
        assertEquals(BigDecimal.ZERO, createdAccount.getBalance());
    }

    @Test
    void registerMemberThrowsWhenUsernameAlreadyExists() {
        when(userAccountRepository.existsByUsername("jkamau")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> memberService.registerMember(form));
    }
}
