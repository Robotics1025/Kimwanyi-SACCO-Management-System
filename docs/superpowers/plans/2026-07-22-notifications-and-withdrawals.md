# Notification Bell + Withdrawal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real savings withdrawals (currently a stub) and build a working notification bell (currently pure decoration) wired to 5 real events across both the admin and member portals.

**Architecture:** Standard Spring service/repository layer additions following this codebase's existing conventions exactly (converter classes, `@RequestScope` controller beans, `FacesMessageUtil`), plus a PrimeFaces `<p:overlayPanel>` dropdown reused in both `admin-template.xhtml` and `member-template.xhtml`.

**Tech Stack:** Jakarta Faces (MyFaces) + PrimeFaces, Spring (constructor injection), JPA/Hibernate (`hibernate.hbm2ddl.auto=update` — the `notifications` table already exists, no migration needed), JUnit 5 + Mockito.

## Global Constraints

- Minimum savings balance: UGX 20,000 must always remain after a withdrawal (same constant already used in `SavingsServiceImpl.transfer()`).
- Notification types used: `LOAN_APPLICATION`, `LOAN_APPROVED`, `LOAN_REJECTED`, `LOAN_REPAYMENT`, `DEPOSIT`, `WITHDRAWAL` (all pre-existing `NotificationType` enum values). `SYSTEM` stays unused — no task creates it.
- Follow existing conventions exactly: one `XxxConverter` class per entity in `common/util/converter` with a `toDto(Entity)` method; `@Component("beanName") @RequestScope` for controller beans with constructor injection and `@PostConstruct init()`; `FacesMessageUtil.addInfoMessage`/`addErrorMessage` for user feedback; `ResourceNotFoundException` for "not found" in new service code (see `SavingsServiceImpl`).
- Timestamps in views: `<f:convertDateTime type="localDateTime" pattern="dd MMM yyyy HH:mm"/>` (matches Savings History / dashboard).
- Currency in views: `<f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>`.

---

## File Structure

**New files:**
- `src/main/java/org/joel/kimwanyisacco/dto/NotificationDto.java`
- `src/main/java/org/joel/kimwanyisacco/common/util/converter/NotificationConverter.java`
- `src/main/java/org/joel/kimwanyisacco/repository/NotificationRepository.java`
- `src/main/java/org/joel/kimwanyisacco/service/NotificationService.java`
- `src/main/java/org/joel/kimwanyisacco/service/NotificationServiceImpl.java`
- `src/main/java/org/joel/kimwanyisacco/controller/NotificationBean.java`
- `src/test/java/org/joel/kimwanyisacco/service/NotificationServiceImplTest.java`
- `src/test/java/org/joel/kimwanyisacco/controller/NotificationBeanTest.java`

**Modified files:**
- `src/main/java/org/joel/kimwanyisacco/policy/WithdrawalPolicy.java` — implement `isWithdrawalAllowed`.
- `src/test/java/org/joel/kimwanyisacco/policy/WithdrawalPolicyTest.java` — un-disable, real tests.
- `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java` — implement `withdraw()`, add notification calls to `deposit()`/`withdraw()`.
- `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java` — extend for `withdraw()` + notification interaction.
- `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTransferTest.java` — add the new `WithdrawalPolicy`/`NotificationService` mocks so `@InjectMocks` resolves cleanly.
- `src/main/java/org/joel/kimwanyisacco/repository/UserAccountRepository.java` — add `findByRole`.
- `src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java` — add notification calls to `applyLoan()`/`decideLoan()`/`repayLoan()`.
- `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java` — un-disable, targeted notification-interaction tests.
- `src/main/webapp/WEB-INF/templates/admin-template.xhtml` — replace static bell with working dropdown.
- `src/main/webapp/WEB-INF/templates/member-template.xhtml` — replace static bell with working dropdown.
- `src/main/webapp/resources/css/admin.css` — add `.notif-badge`.
- `src/main/webapp/resources/css/member.css` — add `.notif-badge`.

---

### Task 1: `WithdrawalPolicy`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/policy/WithdrawalPolicy.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/policy/WithdrawalPolicyTest.java`

**Interfaces:**
- Produces: `WithdrawalPolicy.isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount): boolean`. Consumed by Task 2.

- [ ] **Step 1: Write the failing test**

Replace the entire contents of `src/test/java/org/joel/kimwanyisacco/policy/WithdrawalPolicyTest.java`:

```java
package org.joel.kimwanyisacco.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WithdrawalPolicyTest {

    private final WithdrawalPolicy policy = new WithdrawalPolicy();

    @Test
    void allowsWithdrawalThatLeavesExactlyMinimumBalance() {
        assertTrue(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), new BigDecimal("30000.00")));
    }

    @Test
    void blocksWithdrawalThatWouldDropBelowMinimumBalance() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("30000.00"), new BigDecimal("15000.00")));
    }

    @Test
    void blocksZeroAmount() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), BigDecimal.ZERO));
    }

    @Test
    void blocksNegativeAmount() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), new BigDecimal("-100.00")));
    }

    @Test
    void blocksAmountExceedingBalance() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("10000.00"), new BigDecimal("20000.00")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=WithdrawalPolicyTest test`
Expected: FAIL — `isWithdrawalAllowed` still throws `UnsupportedOperationException`.

- [ ] **Step 3: Implement the policy**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/policy/WithdrawalPolicy.java`:

```java
package org.joel.kimwanyisacco.policy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalPolicy {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    public boolean isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal balanceAfter = currentBalance.subtract(requestedAmount);
        return balanceAfter.compareTo(MINIMUM_BALANCE) >= 0;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -o -Dtest=WithdrawalPolicyTest test`
Expected: PASS (5 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/policy/WithdrawalPolicy.java \
        src/test/java/org/joel/kimwanyisacco/policy/WithdrawalPolicyTest.java
git commit -m "Implement WithdrawalPolicy minimum-balance check"
```

---

### Task 2: `SavingsServiceImpl.withdraw()`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTransferTest.java`

**Interfaces:**
- Consumes: `WithdrawalPolicy.isWithdrawalAllowed(BigDecimal, BigDecimal): boolean` (Task 1).
- Produces: `SavingsServiceImpl` constructor now takes a 5th parameter `WithdrawalPolicy withdrawalPolicy`. `withdraw(WithdrawalForm)` is fully implemented, returning a `SavingsTransactionDto` of type `WITHDRAW` on success, throwing `IllegalArgumentException` when the policy rejects it.

- [ ] **Step 1: Write the failing test**

In `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java`, add the `WithdrawalPolicy` import and mock, update the `service()` factory, and add two new test methods. Replace the entire file:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=SavingsServiceTest test`
Expected: FAIL — `SavingsServiceImpl` constructor doesn't accept 6 args yet, `withdraw()` still throws.

- [ ] **Step 3: Implement `withdraw()` and update the constructor**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java`:

```java
package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.dto.InternalTransferForm;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.TransactionType;
import org.joel.kimwanyisacco.policy.WithdrawalPolicy;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsServiceImpl implements SavingsService {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final SavingsAccountConverter savingsAccountConverter;
    private final SavingsTransactionConverter savingsTransactionConverter;
    private final WithdrawalPolicy withdrawalPolicy;
    private final NotificationService notificationService;

    public SavingsServiceImpl(
            SavingsAccountRepository savingsAccountRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            SavingsAccountConverter savingsAccountConverter,
            SavingsTransactionConverter savingsTransactionConverter,
            WithdrawalPolicy withdrawalPolicy,
            NotificationService notificationService
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.savingsAccountConverter = savingsAccountConverter;
        this.savingsTransactionConverter = savingsTransactionConverter;
        this.withdrawalPolicy = withdrawalPolicy;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SavingsTransactionDto deposit(DepositForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountRepository.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription("Member deposit");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionRepository.save(tx);

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.DEPOSIT,
                "Deposit Received", "UGX " + form.getAmount() + " deposited. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
    }

    @Override
    @Transactional
    public SavingsTransactionDto withdraw(WithdrawalForm form) {
        SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();

        if (!withdrawalPolicy.isWithdrawalAllowed(balanceBefore, form.getAmount())) {
            throw new IllegalArgumentException(
                    "Withdrawal not allowed: amount must be positive and leave at least UGX 20,000 in the account.");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountRepository.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.WITHDRAW);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim() : "Member withdrawal");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionRepository.save(tx);

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.WITHDRAWAL,
                "Withdrawal Processed", "UGX " + form.getAmount() + " withdrawn. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
    }

    @Override
    public SavingsAccountDto getAccountById(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with ID: " + id));
        return savingsAccountConverter.toDto(account);
    }

    @Override
    public List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId) {
        return savingsTransactionRepository.findBySavingsAccountId(savingsAccountId).stream()
                .sorted(Comparator.comparing(org.joel.kimwanyisacco.model.SavingsTransaction::getCreatedAt).reversed())
                .map(savingsTransactionConverter::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void transfer(InternalTransferForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        SavingsAccount fromAccount = savingsAccountRepository.findById(form.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Your savings account was not found"));

        SavingsAccount toAccount = savingsAccountRepository.findByAccountNumber(form.getToAccountNumber().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account number not found: " + form.getToAccountNumber()));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("You cannot transfer to your own account");
        }

        BigDecimal transferAmount = form.getAmount();
        BigDecimal fromBalanceAfter = fromAccount.getBalance().subtract(transferAmount);

        if (fromBalanceAfter.compareTo(MINIMUM_BALANCE) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Transfer would leave your account below the minimum balance of UGX 20,000. " +
                    "Available for transfer: UGX " + fromAccount.getBalance().subtract(MINIMUM_BALANCE).toPlainString()
            );
        }

        BigDecimal toBalanceAfter = toAccount.getBalance().add(transferAmount);
        String refPair = "TXF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String description = form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim()
                : "Internal transfer";
        LocalDateTime now = LocalDateTime.now();

        // Debit sender
        fromAccount.setBalance(fromBalanceAfter);
        savingsAccountRepository.save(fromAccount);

        SavingsTransaction outTx = new SavingsTransaction();
        outTx.setSavingsAccount(fromAccount);
        outTx.setReference(refPair + "-OUT");
        outTx.setType(TransactionType.TRANSFER_OUT);
        outTx.setAmount(transferAmount);
        outTx.setBalanceBefore(fromAccount.getBalance().add(transferAmount));
        outTx.setBalanceAfter(fromBalanceAfter);
        outTx.setDescription(description + " → " + toAccount.getAccountNumber());
        savingsTransactionRepository.save(outTx);

        // Credit receiver
        toAccount.setBalance(toBalanceAfter);
        savingsAccountRepository.save(toAccount);

        SavingsTransaction inTx = new SavingsTransaction();
        inTx.setSavingsAccount(toAccount);
        inTx.setReference(refPair + "-IN");
        inTx.setType(TransactionType.TRANSFER_IN);
        inTx.setAmount(transferAmount);
        inTx.setBalanceBefore(toAccount.getBalance().subtract(transferAmount));
        inTx.setBalanceAfter(toBalanceAfter);
        inTx.setDescription(description + " ← " + fromAccount.getAccountNumber());
        savingsTransactionRepository.save(inTx);
    }
}
```

- [ ] **Step 4: Update `SavingsServiceTransferTest` so `@InjectMocks` resolves cleanly**

In `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTransferTest.java`, add two mock fields right after the existing ones:

```java
    @Mock private SavingsAccountConverter savingsAccountConverter;
    @Mock private SavingsTransactionConverter savingsTransactionConverter;
    @Mock private org.joel.kimwanyisacco.policy.WithdrawalPolicy withdrawalPolicy;
    @Mock private NotificationService notificationService;
```

(This adds `withdrawalPolicy` and `notificationService` right after the existing `savingsTransactionConverter` mock field — leave everything else in the file unchanged.)

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -o -Dtest=SavingsServiceTest,SavingsServiceTransferTest test`
Expected: PASS (7 + existing transfer tests)

- [ ] **Step 6: Run the full suite to check for other breakage**

Run: `mvn -o test`
Expected: PASS — confirm no other test directly constructs `SavingsServiceImpl` with the old 4-arg constructor (`grep -rn "new SavingsServiceImpl" src/test`); if one is found, add the two new mocks there too the same way as Step 4.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java \
        src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java \
        src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTransferTest.java
git commit -m "Implement SavingsServiceImpl.withdraw() and wire deposit/withdraw notifications"
```

---

### Task 3: Notification backend (repository, converter, DTO, service)

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/dto/NotificationDto.java`
- Create: `src/main/java/org/joel/kimwanyisacco/common/util/converter/NotificationConverter.java`
- Create: `src/main/java/org/joel/kimwanyisacco/repository/NotificationRepository.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/repository/UserAccountRepository.java`
- Create: `src/main/java/org/joel/kimwanyisacco/service/NotificationService.java`
- Create: `src/main/java/org/joel/kimwanyisacco/service/NotificationServiceImpl.java`
- Create: `src/test/java/org/joel/kimwanyisacco/service/NotificationServiceImplTest.java`

**Interfaces:**
- Produces: `NotificationService.notify(UserAccount, NotificationType, String title, String message): void`; `notifyAdmins(NotificationType, String title, String message): void`; `getRecent(Long userAccountId): List<NotificationDto>`; `getUnreadCount(Long userAccountId): long`; `markAsRead(Long notificationId): void`; `markAllAsRead(Long userAccountId): void`. `NotificationDto` has `getId(): Long`, `getTitle(): String`, `getMessage(): String`, `getType(): String`, `isReadStatus(): boolean`, `getCreatedAt(): LocalDateTime`. Consumed by Task 2 (already used above), Task 4, Task 5 (bean).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.common.util.converter.NotificationConverter;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.NotificationRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserAccountRepository userAccountRepository;
    private final NotificationConverter notificationConverter = new NotificationConverter();

    private NotificationServiceImpl service() {
        return new NotificationServiceImpl(notificationRepository, userAccountRepository, notificationConverter);
    }

    @Test
    void notifySavesANotificationForTheGivenRecipient() {
        UserAccount recipient = new UserAccount();
        recipient.setUsername("jkamau");

        service().notify(recipient, NotificationType.DEPOSIT, "Deposit Received", "UGX 5000 deposited.");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals(recipient, saved.getUserAccount());
        assertEquals(NotificationType.DEPOSIT, saved.getType());
        assertEquals("Deposit Received", saved.getTitle());
        assertEquals("UGX 5000 deposited.", saved.getMessage());
    }

    @Test
    void notifyAdminsSendsToEveryAdminAccount() {
        UserAccount admin1 = new UserAccount();
        admin1.setUsername("admin1");
        UserAccount admin2 = new UserAccount();
        admin2.setUsername("admin2");
        when(userAccountRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

        service().notifyAdmins(NotificationType.LOAN_APPLICATION, "New Loan Application", "jkamau applied for UGX 50000");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void getRecentMapsEntitiesToDtos() {
        Notification n = new Notification();
        n.setTitle("Deposit Received");
        n.setMessage("UGX 5000 deposited.");
        n.setType(NotificationType.DEPOSIT);
        when(notificationRepository.findTop15ByUserAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        List<NotificationDto> result = service().getRecent(1L);

        assertEquals(1, result.size());
        assertEquals("Deposit Received", result.get(0).getTitle());
        assertEquals("DEPOSIT", result.get(0).getType());
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByUserAccountIdAndReadStatusFalse(1L)).thenReturn(3L);

        assertEquals(3L, service().getUnreadCount(1L));
    }

    @Test
    void markAsReadSetsReadStatusAndSaves() {
        Notification n = new Notification();
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(n));

        service().markAsRead(9L);

        assertEquals(true, n.isReadStatus());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsReadDelegatesToRepositoryBulkUpdate() {
        service().markAllAsRead(1L);

        verify(notificationRepository).markAllAsRead(1L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=NotificationServiceImplTest test`
Expected: FAIL — none of these classes exist yet.

- [ ] **Step 3: Write `NotificationDto`**

```java
package org.joel.kimwanyisacco.dto;

import java.time.LocalDateTime;

public class NotificationDto {

    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean readStatus;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isReadStatus() { return readStatus; }
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: Write `NotificationConverter`**

```java
package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

    public NotificationDto toDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType() != null ? notification.getType().name() : null);
        dto.setReadStatus(notification.isReadStatus());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
```

- [ ] **Step 5: Write `NotificationRepository`**

```java
package org.joel.kimwanyisacco.repository;

import java.util.List;
import org.joel.kimwanyisacco.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop15ByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);

    long countByUserAccountIdAndReadStatusFalse(Long userAccountId);

    @Modifying
    @Query("update Notification n set n.readStatus = true, n.readAt = CURRENT_TIMESTAMP "
            + "where n.userAccount.id = :userAccountId and n.readStatus = false")
    void markAllAsRead(@Param("userAccountId") Long userAccountId);
}
```

- [ ] **Step 6: Add `findByRole` to `UserAccountRepository`**

In `src/main/java/org/joel/kimwanyisacco/repository/UserAccountRepository.java`, add this method inside the interface (alongside the existing ones), and add the `Role` import:

```java
    List<UserAccount> findByRole(org.joel.kimwanyisacco.model.enums.Role role);
```

Full resulting file:

```java
package org.joel.kimwanyisacco.repository;

import java.util.List;
import java.util.Optional;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserAccount> findByRole(Role role);

    @Query("select u from UserAccount u where lower(u.username) like lower(concat('%', :kw, '%')) or lower(u.email) like lower(concat('%', :kw, '%')) or lower(u.firstName) like lower(concat('%', :kw, '%')) or lower(u.lastName) like lower(concat('%', :kw, '%'))")
    List<UserAccount> search(@Param("kw") String keyword);
}
```

- [ ] **Step 7: Write `NotificationService` and `NotificationServiceImpl`**

```java
package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;

public interface NotificationService {

    void notify(UserAccount recipient, NotificationType type, String title, String message);

    void notifyAdmins(NotificationType type, String title, String message);

    List<NotificationDto> getRecent(Long userAccountId);

    long getUnreadCount(Long userAccountId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userAccountId);
}
```

```java
package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.NotificationConverter;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.model.Notification;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.NotificationRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationConverter notificationConverter;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserAccountRepository userAccountRepository,
            NotificationConverter notificationConverter
    ) {
        this.notificationRepository = notificationRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationConverter = notificationConverter;
    }

    @Override
    @Transactional
    public void notify(UserAccount recipient, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserAccount(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void notifyAdmins(NotificationType type, String title, String message) {
        List<UserAccount> admins = userAccountRepository.findByRole(Role.ADMIN);
        for (UserAccount admin : admins) {
            notify(admin, type, title, message);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getRecent(Long userAccountId) {
        return notificationRepository.findTop15ByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(notificationConverter::toDto)
                .toList();
    }

    @Override
    public long getUnreadCount(Long userAccountId) {
        return notificationRepository.countByUserAccountIdAndReadStatusFalse(userAccountId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setReadStatus(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userAccountId) {
        notificationRepository.markAllAsRead(userAccountId);
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `mvn -o -Dtest=NotificationServiceImplTest test`
Expected: PASS (6 tests)

- [ ] **Step 9: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/dto/NotificationDto.java \
        src/main/java/org/joel/kimwanyisacco/common/util/converter/NotificationConverter.java \
        src/main/java/org/joel/kimwanyisacco/repository/NotificationRepository.java \
        src/main/java/org/joel/kimwanyisacco/repository/UserAccountRepository.java \
        src/main/java/org/joel/kimwanyisacco/service/NotificationService.java \
        src/main/java/org/joel/kimwanyisacco/service/NotificationServiceImpl.java \
        src/test/java/org/joel/kimwanyisacco/service/NotificationServiceImplTest.java
git commit -m "Add notification backend: repository, converter, service"
```

---

### Task 4: Wire notifications into `LoanServiceImpl`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java`

**Interfaces:**
- Consumes: `NotificationService.notifyAdmins(NotificationType, String, String)`, `NotificationService.notify(UserAccount, NotificationType, String, String)` (Task 3).
- Produces: `LoanServiceImpl` constructor now takes a 9th parameter `NotificationService notificationService`.

- [ ] **Step 1: Write the failing test**

Replace the entire contents of `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java`:

```java
package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.policy.LoanEligibilityPolicy;
import org.joel.kimwanyisacco.policy.LoanInterestCalculator;
import org.joel.kimwanyisacco.repository.LoanRepaymentRepository;
import org.joel.kimwanyisacco.repository.LoanRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanRepaymentRepository loanRepaymentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private final LoanEligibilityPolicy loanEligibilityPolicy = new LoanEligibilityPolicy();
    private final LoanInterestCalculator loanInterestCalculator = new LoanInterestCalculator();

    private LoanServiceImpl service() {
        return new LoanServiceImpl(loanRepository, loanRepaymentRepository, memberRepository,
                savingsAccountRepository, userAccountRepository, loanEligibilityPolicy,
                loanInterestCalculator, auditLogService, notificationService);
    }

    private Member memberWithUserAccount(Long memberId, String username) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        Member member = new Member();
        member.setId(memberId);
        member.setUserAccount(userAccount);
        return member;
    }

    @Test
    void applyLoanNotifiesAllAdmins() {
        Member member = memberWithUserAccount(1L, "jkamau");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setBalance(new BigDecimal("100000.00"));
        when(savingsAccountRepository.findByMemberId(1L)).thenReturn(Optional.of(account));
        when(loanRepository.findByMemberId(1L)).thenReturn(List.of());
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationForm form = new LoanApplicationForm();
        form.setMemberId(1L);
        form.setPrincipalAmount(new BigDecimal("50000.00"));
        form.setTermMonths(6);

        service().applyLoan(form);

        verify(notificationService).notifyAdmins(eq(NotificationType.LOAN_APPLICATION), anyString(), anyString());
    }

    @Test
    void decideLoanApprovedNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setPrincipal(new BigDecimal("50000.00"));
        loan.setStatus(LoanStatus.PENDING);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanDecisionForm form = new LoanDecisionForm();
        form.setLoanId(5L);
        form.setApproved(true);

        service().decideLoan(form, 2L);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_APPROVED), anyString(), anyString());
    }

    @Test
    void decideLoanRejectedNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setPrincipal(new BigDecimal("50000.00"));
        loan.setStatus(LoanStatus.PENDING);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanDecisionForm form = new LoanDecisionForm();
        form.setLoanId(5L);
        form.setApproved(false);
        form.setRemarks("Insufficient collateral");

        service().decideLoan(form, 2L);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_REJECTED), anyString(), anyString());
    }

    @Test
    void repayLoanNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingBalance(new BigDecimal("20000.00"));
        loan.setAmountRepaid(BigDecimal.ZERO);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        LoanRepaymentForm form = new LoanRepaymentForm();
        form.setLoanId(5L);
        form.setAmount(new BigDecimal("5000.00"));

        service().repayLoan(form);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_REPAYMENT), anyString(), anyString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=LoanServiceTest test`
Expected: FAIL — `LoanServiceImpl` constructor doesn't accept `NotificationService` yet.

- [ ] **Step 3: Wire notification calls into `LoanServiceImpl`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java`:

```java
package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.LoanRepayment;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.policy.LoanEligibilityPolicy;
import org.joel.kimwanyisacco.policy.LoanInterestCalculator;
import org.joel.kimwanyisacco.repository.LoanRepository;
import org.joel.kimwanyisacco.repository.LoanRepaymentRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoanEligibilityPolicy loanEligibilityPolicy;
    private final LoanInterestCalculator loanInterestCalculator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserAccountRepository userAccountRepository,
            LoanEligibilityPolicy loanEligibilityPolicy,
            LoanInterestCalculator loanInterestCalculator,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.loanEligibilityPolicy = loanEligibilityPolicy;
        this.loanInterestCalculator = loanInterestCalculator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Loan applyLoan(LoanApplicationForm form) {
        Member member = memberRepository.findById(form.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("Savings account not found for member"));
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());

        BigDecimal principal = form.getPrincipalAmount();
        loanEligibilityPolicy.verifyEligibility(member, savingsAccount, principal, memberLoans);

        BigDecimal interest = loanInterestCalculator.calculateInterest(principal);
        BigDecimal totalRepayable = principal.add(interest);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setPrincipal(principal);
        loan.setInterestRate(new BigDecimal("10.00"));
        loan.setInterestAmount(interest);
        loan.setTotalRepayable(totalRepayable);
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(totalRepayable);
        loan.setStatus(LoanStatus.PENDING);
        loan.setPurpose("General Loan");
        loan.setApplicationDate(LocalDate.now());

        Loan saved = loanRepository.save(loan);

        auditLogService.record(member.getUserAccount(), AuditAction.LOAN_APPLIED, "Loan", saved.getId(),
                "Applied for loan: UGX " + principal);

        notificationService.notifyAdmins(NotificationType.LOAN_APPLICATION, "New Loan Application",
                member.getUserAccount().getUsername() + " applied for UGX " + principal);

        return saved;
    }

    @Override
    @Transactional
    public Loan decideLoan(LoanDecisionForm form, Long adminUserId) {
        Loan loan = loanRepository.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setDecisionDate(LocalDate.now());
        loan.setDecidedBy(admin);

        if (form.isApproved()) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setDueDate(LocalDate.now().plusMonths(12));
            loanRepository.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_APPROVED, "Loan", loan.getId(),
                    "Approved loan for member: " + loan.getMember().getUserAccount().getUsername());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_APPROVED,
                    "Loan Approved", "Your loan of UGX " + loan.getPrincipal() + " was approved.");
        } else {
            loan.setStatus(LoanStatus.REJECTED);
            loan.setRejectionReason(form.getRemarks());
            loanRepository.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_REJECTED, "Loan", loan.getId(),
                    "Rejected loan. Reason: " + form.getRemarks());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REJECTED,
                    "Loan Rejected", "Your loan application was rejected. Reason: " + form.getRemarks());
        }

        return loan;
    }

    @Override
    @Transactional
    public void repayLoan(LoanRepaymentForm form) {
        Loan loan = loanRepository.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Loan is not active or overdue");
        }

        BigDecimal amount = form.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive");
        }

        BigDecimal currentOutstanding = loan.getOutstandingBalance();
        if (amount.compareTo(currentOutstanding) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds outstanding balance");
        }

        BigDecimal newOutstanding = currentOutstanding.subtract(amount);
        BigDecimal newAmountRepaid = loan.getAmountRepaid().add(amount);

        loan.setOutstandingBalance(newOutstanding);
        loan.setAmountRepaid(newAmountRepaid);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.FULLY_REPAID);
        }

        loanRepository.save(loan);

        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(amount);
        repayment.setBalanceBefore(currentOutstanding);
        repayment.setBalanceAfter(newOutstanding);
        repayment.setPaymentMethod("CASH");
        repayment.setReference("REP-" + System.currentTimeMillis());
        repayment.setPaymentDate(LocalDate.now());

        loanRepaymentRepository.save(repayment);

        auditLogService.record(loan.getMember().getUserAccount(), AuditAction.LOAN_REPAYMENT_RECORDED, "LoanRepayment", repayment.getId(),
                "Repaid UGX " + amount + " for Loan #" + loan.getId());

        notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REPAYMENT,
                "Repayment Recorded", "UGX " + amount + " repaid. Outstanding balance: UGX " + newOutstanding + ".");
    }

    @Override
    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus(LoanStatus.PENDING);
    }

    @Override
    public List<Loan> getLoansByMember(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    @Override
    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -o -Dtest=LoanServiceTest test`
Expected: PASS (4 tests)

- [ ] **Step 5: Run the full suite to check for other breakage**

Run: `mvn -o test`
Expected: PASS — confirm no other test directly constructs `LoanServiceImpl` (`grep -rn "new LoanServiceImpl" src/test`); update it the same way if one exists.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java \
        src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java
git commit -m "Notify admins on loan application and applicant on decision/repayment"
```

---

### Task 5: `NotificationBean` controller

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/controller/NotificationBean.java`
- Create: `src/test/java/org/joel/kimwanyisacco/controller/NotificationBeanTest.java`

**Interfaces:**
- Consumes: `NotificationService.getRecent/getUnreadCount/markAsRead/markAllAsRead` (Task 3), `UserSessionBean.getLoggedInUser()` (existing).
- Produces: `NotificationBean.getRecent(): List<NotificationDto>`, `getUnreadCount(): long`, `markAsRead(Long id): void`, `markAllAsRead(): void`, `iconClass(NotificationDto): String`. Consumed by Tasks 6 and 7 (templates).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationBeanTest {

    @Mock private NotificationService notificationService;
    @Mock private UserSessionBean userSessionBean;

    private NotificationDto notificationOfType(String type) {
        NotificationDto dto = new NotificationDto();
        dto.setType(type);
        return dto;
    }

    private void loggedInAs(long userAccountId) {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccountId);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);
    }

    @Test
    void initLoadsRecentNotificationsAndUnreadCount() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of(notificationOfType("DEPOSIT")));
        when(notificationService.getUnreadCount(1L)).thenReturn(2L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();

        assertEquals(1, bean.getRecent().size());
        assertEquals(2L, bean.getUnreadCount());
    }

    @Test
    void markAsReadDelegatesAndReloads() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of());
        when(notificationService.getUnreadCount(1L)).thenReturn(0L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();
        bean.markAsRead(9L);

        verify(notificationService).markAsRead(9L);
    }

    @Test
    void markAllAsReadDelegatesAndReloads() {
        loggedInAs(1L);
        when(notificationService.getRecent(1L)).thenReturn(List.of());
        when(notificationService.getUnreadCount(1L)).thenReturn(0L);

        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);
        bean.init();
        bean.markAllAsRead();

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    void iconClassMapsEachNotificationType() {
        NotificationBean bean = new NotificationBean(notificationService, userSessionBean);

        assertEquals("fa-file-signature", bean.iconClass(notificationOfType("LOAN_APPLICATION")));
        assertEquals("fa-circle-check", bean.iconClass(notificationOfType("LOAN_APPROVED")));
        assertEquals("fa-circle-xmark", bean.iconClass(notificationOfType("LOAN_REJECTED")));
        assertEquals("fa-hand-holding-dollar", bean.iconClass(notificationOfType("LOAN_REPAYMENT")));
        assertEquals("fa-arrow-down", bean.iconClass(notificationOfType("DEPOSIT")));
        assertEquals("fa-arrow-up", bean.iconClass(notificationOfType("WITHDRAWAL")));
        assertEquals("fa-circle-info", bean.iconClass(notificationOfType("SYSTEM")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=NotificationBeanTest test`
Expected: FAIL — `NotificationBean` doesn't exist yet.

- [ ] **Step 3: Write `NotificationBean`**

```java
package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.joel.kimwanyisacco.dto.NotificationDto;
import org.joel.kimwanyisacco.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("notificationBean")
@RequestScope
public class NotificationBean {

    private final NotificationService notificationService;
    private final UserSessionBean userSessionBean;

    private List<NotificationDto> recent = List.of();
    private long unreadCount;

    public NotificationBean(NotificationService notificationService, UserSessionBean userSessionBean) {
        this.notificationService = notificationService;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        Long userAccountId = userSessionBean.getLoggedInUser().getId();
        recent = notificationService.getRecent(userAccountId);
        unreadCount = notificationService.getUnreadCount(userAccountId);
    }

    public void markAsRead(Long notificationId) {
        notificationService.markAsRead(notificationId);
        init();
    }

    public void markAllAsRead() {
        notificationService.markAllAsRead(userSessionBean.getLoggedInUser().getId());
        init();
    }

    public String iconClass(NotificationDto notification) {
        if (notification.getType() == null) {
            return "fa-circle-info";
        }
        return switch (notification.getType()) {
            case "LOAN_APPLICATION" -> "fa-file-signature";
            case "LOAN_APPROVED" -> "fa-circle-check";
            case "LOAN_REJECTED" -> "fa-circle-xmark";
            case "LOAN_REPAYMENT" -> "fa-hand-holding-dollar";
            case "DEPOSIT" -> "fa-arrow-down";
            case "WITHDRAWAL" -> "fa-arrow-up";
            default -> "fa-circle-info";
        };
    }

    public List<NotificationDto> getRecent() {
        return recent;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -o -Dtest=NotificationBeanTest test`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/NotificationBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/NotificationBeanTest.java
git commit -m "Add NotificationBean for the bell dropdown"
```

---

### Task 6: Wire the bell into `admin-template.xhtml`

**Files:**
- Modify: `src/main/webapp/WEB-INF/templates/admin-template.xhtml`
- Modify: `src/main/webapp/resources/css/admin.css`

**Interfaces:**
- Consumes: `notificationBean.recent`, `notificationBean.unreadCount`, `notificationBean.markAsRead(Long)`, `notificationBean.markAllAsRead()`, `notificationBean.iconClass(NotificationDto)` (Task 5).

- [ ] **Step 1: Add `.notif-badge` CSS**

Append to the end of `src/main/webapp/resources/css/admin.css`:

```css

/* Notification bell badge */
.notif-badge {
    position: absolute;
    top: -6px; right: -8px;
    min-width: 16px; height: 16px;
    padding: 0 4px;
    background: #dc2626;
    color: #fff;
    border-radius: 999px;
    font-size: 0.65rem;
    font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    line-height: 1;
}
```

- [ ] **Step 2: Replace the static bell markup**

In `src/main/webapp/WEB-INF/templates/admin-template.xhtml`, replace:

```xml
                <div class="topbar-icons" style="display: flex; gap: 1.25rem; color: #6b7280; font-size: 1.1rem;">
                    <i class="fa-regular fa-message" style="cursor: pointer;"></i>
                    <i class="fa-regular fa-bell" style="cursor: pointer; position: relative;">
                        <span style="position: absolute; top: -4px; right: -4px; width: 8px; height: 8px; background: var(--primary); border-radius: 50%;"></span>
                    </i>
                </div>
```

with:

```xml
                <div class="topbar-icons" style="display: flex; gap: 1.25rem; color: #6b7280; font-size: 1.1rem;">
                    <i class="fa-regular fa-message" style="cursor: pointer;"></i>
                    <h:panelGroup id="notifBell" style="cursor: pointer; position: relative; display: inline-block;">
                        <i class="fa-regular fa-bell"></i>
                        <h:panelGroup rendered="#{notificationBean.unreadCount > 0}">
                            <span class="notif-badge">#{notificationBean.unreadCount}</span>
                        </h:panelGroup>
                    </h:panelGroup>
                </div>

                <p:overlayPanel for="notifBell" id="notifPanel" dismissable="true" style="width:340px; max-height:420px; overflow-y:auto; padding:0;">
                    <h:form id="notifForm">
                        <div style="display:flex; justify-content:space-between; align-items:center; padding:0.85rem 1rem; border-bottom:1px solid #e5e7eb;">
                            <strong style="font-size:0.9rem;">Notifications</strong>
                            <p:commandLink action="#{notificationBean.markAllAsRead}" rendered="#{notificationBean.unreadCount > 0}"
                                           update=":notifBell :notifPanel"
                                           style="font-size:0.78rem; color:#4f46e5; text-decoration:none; font-weight:600;">
                                Mark all read
                            </p:commandLink>
                        </div>

                        <h:panelGroup rendered="#{empty notificationBean.recent}">
                            <div style="padding:2rem 1rem; text-align:center; color:#9ca3af; font-size:0.85rem;">No notifications yet</div>
                        </h:panelGroup>

                        <ui:repeat value="#{notificationBean.recent}" var="n">
                            <p:commandLink action="#{notificationBean.markAsRead(n.id)}" update=":notifBell :notifPanel"
                                           style="display:block; text-decoration:none; color:inherit;">
                                <div style="display:flex; gap:0.65rem; padding:0.75rem 1rem; border-bottom:1px solid #f3f4f6; background:#{n.readStatus ? 'transparent' : '#eef2ff'};">
                                    <i class="fa-solid #{notificationBean.iconClass(n)}" style="color:#4f46e5; margin-top:2px;"></i>
                                    <div>
                                        <div style="font-weight:#{n.readStatus ? '500' : '700'}; font-size:0.83rem; color:#111827;">#{n.title}</div>
                                        <div style="font-size:0.78rem; color:#6b7280; margin-top:2px;">#{n.message}</div>
                                        <div style="font-size:0.7rem; color:#9ca3af; margin-top:4px;">
                                            <h:outputText value="#{n.createdAt}">
                                                <f:convertDateTime type="localDateTime" pattern="dd MMM yyyy HH:mm"/>
                                            </h:outputText>
                                        </div>
                                    </div>
                                </div>
                            </p:commandLink>
                        </ui:repeat>
                    </h:form>
                </p:overlayPanel>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/WEB-INF/templates/admin-template.xhtml src/main/webapp/resources/css/admin.css
git commit -m "Wire working notification bell into admin template"
```

---

### Task 7: Wire the bell into `member-template.xhtml`

**Files:**
- Modify: `src/main/webapp/WEB-INF/templates/member-template.xhtml`
- Modify: `src/main/webapp/resources/css/member.css`

**Interfaces:**
- Consumes: same `notificationBean` interface as Task 6 (already built, shared across both portals).

- [ ] **Step 1: Add `.notif-badge` CSS**

Append to the end of `src/main/webapp/resources/css/member.css`:

```css

/* Notification bell badge */
.notif-badge {
    position: absolute;
    top: -6px; right: -8px;
    min-width: 16px; height: 16px;
    padding: 0 4px;
    background: #dc2626;
    color: #fff;
    border-radius: 999px;
    font-size: 0.65rem;
    font-weight: 700;
    display: flex; align-items: center; justify-content: center;
    line-height: 1;
}
```

- [ ] **Step 2: Replace the static bell markup**

In `src/main/webapp/WEB-INF/templates/member-template.xhtml`, replace:

```xml
                <div class="mem-topbar-notif">
                    <i class="fa-regular fa-bell"></i>
                    <span class="notif-dot"></span>
                </div>
```

with:

```xml
                <h:panelGroup id="notifBell" styleClass="mem-topbar-notif">
                    <i class="fa-regular fa-bell"></i>
                    <h:panelGroup rendered="#{notificationBean.unreadCount > 0}">
                        <span class="notif-badge">#{notificationBean.unreadCount}</span>
                    </h:panelGroup>
                </h:panelGroup>

                <p:overlayPanel for="notifBell" id="notifPanel" dismissable="true" style="width:320px; max-height:420px; overflow-y:auto; padding:0;">
                    <h:form id="notifForm">
                        <div style="display:flex; justify-content:space-between; align-items:center; padding:0.85rem 1rem; border-bottom:1px solid #e5e7eb;">
                            <strong style="font-size:0.9rem;">Notifications</strong>
                            <p:commandLink action="#{notificationBean.markAllAsRead}" rendered="#{notificationBean.unreadCount > 0}"
                                           update=":notifBell :notifPanel"
                                           style="font-size:0.78rem; color:#dc2626; text-decoration:none; font-weight:600;">
                                Mark all read
                            </p:commandLink>
                        </div>

                        <h:panelGroup rendered="#{empty notificationBean.recent}">
                            <div style="padding:2rem 1rem; text-align:center; color:#9ca3af; font-size:0.85rem;">No notifications yet</div>
                        </h:panelGroup>

                        <ui:repeat value="#{notificationBean.recent}" var="n">
                            <p:commandLink action="#{notificationBean.markAsRead(n.id)}" update=":notifBell :notifPanel"
                                           style="display:block; text-decoration:none; color:inherit;">
                                <div style="display:flex; gap:0.65rem; padding:0.75rem 1rem; border-bottom:1px solid #f3f4f6; background:#{n.readStatus ? 'transparent' : '#fef2f2'};">
                                    <i class="fa-solid #{notificationBean.iconClass(n)}" style="color:#dc2626; margin-top:2px;"></i>
                                    <div>
                                        <div style="font-weight:#{n.readStatus ? '500' : '700'}; font-size:0.83rem; color:#111827;">#{n.title}</div>
                                        <div style="font-size:0.78rem; color:#6b7280; margin-top:2px;">#{n.message}</div>
                                        <div style="font-size:0.7rem; color:#9ca3af; margin-top:4px;">
                                            <h:outputText value="#{n.createdAt}">
                                                <f:convertDateTime type="localDateTime" pattern="dd MMM yyyy HH:mm"/>
                                            </h:outputText>
                                        </div>
                                    </div>
                                </div>
                            </p:commandLink>
                        </ui:repeat>
                    </h:form>
                </p:overlayPanel>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/WEB-INF/templates/member-template.xhtml src/main/webapp/resources/css/member.css
git commit -m "Wire working notification bell into member template"
```

---

### Task 8: Manual end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `mvn -o test`
Expected: PASS, all tests green.

- [ ] **Step 2: Rebuild the exploded WAR and restart the app server**

```bash
mvn -o -q war:exploded
```
Then restart the Tomcat process serving `kimwanyi_sacco_war_exploded` (kill the current one on port 8080; if it doesn't auto-restart via IntelliJ, start it directly with `catalina.sh run` using the same `CATALINA_BASE` as the IntelliJ-managed instance).

- [ ] **Step 3: Trigger each event and confirm a notification appears**
   - Log in as a member with a positive savings balance, apply for a loan → log in as admin → bell badge shows 1, dropdown shows "New Loan Application".
   - As admin, approve that loan → log back in as the member → bell badge shows 1, dropdown shows "Loan Approved".
   - As admin, process a deposit for a member via the Savings Accounts page → log in as that member → bell shows "Deposit Received".
   - As admin, process a withdrawal for a member via the Savings Accounts page (confirm it actually succeeds now, not an error toast) → log in as that member → bell shows "Withdrawal Processed".
   - Record a loan repayment (if a repayment UI exists) → confirm "Repayment Recorded" notification appears for the member.

- [ ] **Step 4: Verify mark-as-read behavior**
   - Click a single notification → confirm the badge count decrements by 1 and that item's background/weight changes to "read" styling, without needing a full page reload (ajax `update` should handle this).
   - Click "Mark all read" → confirm the badge disappears and all items switch to the read style.

- [ ] **Step 5: Verify the notification bell renders correctly with zero notifications** (e.g. a brand-new member who hasn't triggered any event yet) — badge hidden, dropdown shows "No notifications yet".

No commit for this task — it's verification of Tasks 1–7.
