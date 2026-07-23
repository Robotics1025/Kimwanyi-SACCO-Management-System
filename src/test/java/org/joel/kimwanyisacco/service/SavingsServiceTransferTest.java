package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.InternalTransferForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SavingsServiceTransferTest {

    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsTransactionRepository savingsTransactionRepository;
    @Mock private SavingsAccountConverter savingsAccountConverter;
    @Mock private SavingsTransactionConverter savingsTransactionConverter;
    @Mock private org.joel.kimwanyisacco.policy.WithdrawalPolicy withdrawalPolicy;
    @Mock private NotificationService notificationService;
    @Mock private org.joel.kimwanyisacco.policy.SavingsInterestCalculator savingsInterestCalculator;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private SavingsServiceImpl savingsService;

    @Test
    void transferSucceedsWhenValid() {
        SavingsAccount fromAccount = new SavingsAccount();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("SAV-1");
        fromAccount.setBalance(new BigDecimal("50000.00")); // 30k transferrable
        fromAccount.setMember(activeMember());

        SavingsAccount toAccount = new SavingsAccount();
        toAccount.setId(2L);
        toAccount.setAccountNumber("SAV-2");
        toAccount.setBalance(new BigDecimal("10000.00"));
        toAccount.setMember(activeMember());

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(savingsAccountRepository.findByAccountNumber("SAV-2")).thenReturn(Optional.of(toAccount));

        InternalTransferForm form = new InternalTransferForm();
        form.setFromAccountId(1L);
        form.setToAccountNumber("SAV-2");
        form.setAmount(new BigDecimal("25000.00"));

        savingsService.transfer(form);

        verify(savingsAccountRepository, times(2)).save(any(SavingsAccount.class));
        verify(savingsTransactionRepository, times(2)).save(any());

        assertEquals(new BigDecimal("25000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("35000.00"), toAccount.getBalance());
    }

    @Test
    void transferFailsIfMinimumBalanceViolated() {
        SavingsAccount fromAccount = new SavingsAccount();
        fromAccount.setId(1L);
        fromAccount.setAccountNumber("SAV-1");
        fromAccount.setBalance(new BigDecimal("30000.00")); // Only 10k transferrable
        fromAccount.setMember(activeMember());

        SavingsAccount toAccount = new SavingsAccount();
        toAccount.setId(2L);
        toAccount.setAccountNumber("SAV-2");
        toAccount.setBalance(new BigDecimal("10000.00"));
        toAccount.setMember(activeMember());

        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(savingsAccountRepository.findByAccountNumber("SAV-2")).thenReturn(Optional.of(toAccount));

        InternalTransferForm form = new InternalTransferForm();
        form.setFromAccountId(1L);
        form.setToAccountNumber("SAV-2");
        form.setAmount(new BigDecimal("15000.00"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> savingsService.transfer(form));
        assertTrue(ex.getMessage().contains("minimum balance"));
    }

    @Test
    void transferRejectsInactiveRecipient() {
        SavingsAccount from = new SavingsAccount();
        from.setId(1L); from.setBalance(new BigDecimal("50000.00")); from.setMember(activeMember());
        SavingsAccount to = new SavingsAccount();
        to.setId(2L); to.setAccountNumber("SAV-2"); to.setBalance(BigDecimal.ZERO);
        Member inactive = new Member(); inactive.setStatus(MemberStatus.INACTIVE); to.setMember(inactive);
        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(from));
        when(savingsAccountRepository.findByAccountNumber("SAV-2")).thenReturn(Optional.of(to));
        InternalTransferForm form = new InternalTransferForm();
        form.setFromAccountId(1L); form.setToAccountNumber("SAV-2"); form.setAmount(new BigDecimal("1000"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> savingsService.transfer(form));
        assertTrue(ex.getMessage().contains("recipient must be an active"));
    }

    private Member activeMember() {
        Member member = new Member();
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }
}
