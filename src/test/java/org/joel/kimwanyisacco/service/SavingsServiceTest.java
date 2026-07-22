package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.joel.kimwanyisacco.model.enums.TransactionType;
import org.joel.kimwanyisacco.policy.WithdrawalPolicy;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsServiceTest {

    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private SavingsTransactionRepository savingsTransactionRepository;
    @Mock private NotificationService notificationService;

    private final SavingsAccountConverter savingsAccountConverter = new SavingsAccountConverter();
    private final SavingsTransactionConverter savingsTransactionConverter = new SavingsTransactionConverter();
    private final WithdrawalPolicy withdrawalPolicy = new WithdrawalPolicy();

    private SavingsServiceImpl service() {
        return new SavingsServiceImpl(
                savingsAccountRepository, savingsTransactionRepository,
                savingsAccountConverter, savingsTransactionConverter,
                withdrawalPolicy, notificationService);
    }

    @Test
    void getAccountByIdReturnsMappedDto() {
        SavingsAccount account = new SavingsAccount();
        account.setAccountNumber("SAV-2026-0001");
        account.setBalance(new BigDecimal("50000.00"));
        when(savingsAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        SavingsAccountDto dto = service().getAccountById(1L);

        assertEquals("SAV-2026-0001", dto.getAccountNumber());
        assertEquals(new BigDecimal("50000.00"), dto.getBalance());
    }

    @Test
    void getAccountByIdThrowsWhenMissing() {
        when(savingsAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().getAccountById(99L));
    }

    @Test
    void getTransactionHistoryReturnsNewestFirst() {
        SavingsTransaction older = new SavingsTransaction();
        older.setType(TransactionType.DEPOSIT);
        older.setAmount(new BigDecimal("10000.00"));
        older.setBalanceAfter(new BigDecimal("10000.00"));
        setCreatedAt(older, LocalDateTime.of(2026, 1, 1, 9, 0));

        SavingsTransaction newer = new SavingsTransaction();
        newer.setType(TransactionType.WITHDRAW);
        newer.setAmount(new BigDecimal("2000.00"));
        newer.setBalanceAfter(new BigDecimal("8000.00"));
        setCreatedAt(newer, LocalDateTime.of(2026, 2, 1, 9, 0));

        when(savingsTransactionRepository.findBySavingsAccountId(5L)).thenReturn(List.of(older, newer));

        List<SavingsTransactionDto> history = service().getTransactionHistory(5L);

        assertEquals(2, history.size());
        assertEquals("WITHDRAW", history.get(0).getTransactionType());
        assertEquals("DEPOSIT", history.get(1).getTransactionType());
    }

    @Test
    void withdrawSucceedsAndReturnsDebitedTransaction() {
        org.joel.kimwanyisacco.model.UserAccount userAccount = new org.joel.kimwanyisacco.model.UserAccount();
        userAccount.setUsername("jkamau");
        org.joel.kimwanyisacco.model.Member member = new org.joel.kimwanyisacco.model.Member();
        member.setUserAccount(userAccount);

        SavingsAccount account = new SavingsAccount();
        account.setId(7L);
        account.setBalance(new BigDecimal("50000.00"));
        account.setMember(member);
        when(savingsAccountRepository.findById(7L)).thenReturn(Optional.of(account));

        WithdrawalForm form = new WithdrawalForm();
        form.setSavingsAccountId(7L);
        form.setAmount(new BigDecimal("20000.00"));

        SavingsTransactionDto result = service().withdraw(form);

        assertEquals("WITHDRAW", result.getTransactionType());
        assertEquals(new BigDecimal("20000.00"), result.getAmount());
        assertEquals(new BigDecimal("30000.00"), account.getBalance());
    }

    @Test
    void withdrawThrowsWhenItWouldDropBelowMinimumBalance() {
        SavingsAccount account = new SavingsAccount();
        account.setId(7L);
        account.setBalance(new BigDecimal("30000.00"));
        when(savingsAccountRepository.findById(7L)).thenReturn(Optional.of(account));

        WithdrawalForm form = new WithdrawalForm();
        form.setSavingsAccountId(7L);
        form.setAmount(new BigDecimal("15000.00"));

        assertThrows(IllegalArgumentException.class, () -> service().withdraw(form));
    }

    private void setCreatedAt(SavingsTransaction transaction, LocalDateTime createdAt) {
        try {
            var field = SavingsTransaction.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(transaction, createdAt);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
