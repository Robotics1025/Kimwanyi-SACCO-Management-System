# Loan Repayment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the already-working `LoanServiceImpl.repayLoan()` backend reachable from real UI — members can repay their own active loan, admins can record a repayment on a member's behalf — closing the "Loan module: ...repay" gap from the project spec.

**Architecture:** Extend the existing `LoanRepaymentForm`/`LoanService` with a payment method and an optional "received by admin" id, replace the empty `LoanRepaymentBean` stub with a real session-scoped bean (mirroring `InternalTransferBean`/`DepositBean`), and add a repayment dialog to the two pages that already show loans (`loans/applications.xhtml` for members, `admin/loans.xhtml` for admins, the latter gaining a second tab for active loans). No new pages, no new routes.

**Tech Stack:** Jakarta Faces (MyFaces) + PrimeFaces, Spring (constructor injection), JPA/Hibernate, JUnit 5 + Mockito.

## Global Constraints

- A loan can only be repaid while `ACTIVE` or `OVERDUE`; repayment amount must be positive and cannot exceed the outstanding balance (existing `LoanServiceImpl.repayLoan` validation — unchanged).
- Payment method is one of exactly three string values: `CASH`, `MOBILE_MONEY`, `BANK_TRANSFER`.
- No payment gateway integration — this is record-only, exactly like the existing Deposit/Withdraw flows. Do not touch `Payment`, `PaymentMethod`, or `MobileMoneyProvider`.
- No new pages/routes — the UI lives on `loans/applications.xhtml` and `admin/loans.xhtml`, which are already routed and linked from navigation.
- `loans/repayments.xhtml` and `loans/details.xhtml` stay untouched, unrouted placeholders.
- Follow existing conventions exactly: `FacesMessageUtil` for user feedback, `@SessionScope` session-scoped beans for member self-service actions (matches `InternalTransferBean`/`DepositBean`), `@RequestScope` for the admin loans page bean (matches existing `LoanApprovalBean`).

---

## File Structure

**New files:**
- `src/test/java/org/joel/kimwanyisacco/controller/LoanRepaymentBeanTest.java`
- `src/test/java/org/joel/kimwanyisacco/controller/LoanApprovalBeanTest.java`

**Modified files:**
- `src/main/java/org/joel/kimwanyisacco/dto/LoanRepaymentForm.java` — add `paymentMethod`.
- `src/main/java/org/joel/kimwanyisacco/service/LoanService.java` — `repayLoan` signature change, add `getActiveLoans()`.
- `src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java` — same.
- `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java` — extend for the new signature/behavior.
- `src/main/java/org/joel/kimwanyisacco/repository/LoanRepository.java` — add `findActiveOrOverdue()`.
- `src/main/java/org/joel/kimwanyisacco/controller/LoanRepaymentBean.java` — full implementation (currently an empty stub).
- `src/main/java/org/joel/kimwanyisacco/controller/LoanApprovalBean.java` — add `activeLoans`.
- `src/main/webapp/loans/applications.xhtml` — add Actions column + repay dialog.
- `src/main/webapp/admin/loans.xhtml` — wrap in `p:tabView`, add Active Loans tab + repay dialog.

---

### Task 1: Backend — `repayLoan` signature, payment method, `getActiveLoans`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/dto/LoanRepaymentForm.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/service/LoanService.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/repository/LoanRepository.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java`

**Interfaces:**
- Produces: `LoanRepaymentForm.getPaymentMethod()/setPaymentMethod(String)`; `LoanService.repayLoan(LoanRepaymentForm form, Long receivedByUserId): void`; `LoanService.getActiveLoans(): List<Loan>`. Consumed by Tasks 2 and 3.

- [ ] **Step 1: Write the failing tests**

Replace the entire contents of `src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java`:

```java
package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.LoanRepayment;
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
import org.mockito.ArgumentCaptor;
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
    void repayLoanPersistsGivenPaymentMethodNotHardcodedCash() {
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
        form.setPaymentMethod("MOBILE_MONEY");

        service().repayLoan(form, null);

        ArgumentCaptor<LoanRepayment> captor = ArgumentCaptor.forClass(LoanRepayment.class);
        verify(loanRepaymentRepository).save(captor.capture());
        assertEquals("MOBILE_MONEY", captor.getValue().getPaymentMethod());
        assertNull(captor.getValue().getReceivedBy());
        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_REPAYMENT), anyString(), anyString());
    }

    @Test
    void repayLoanSetsReceivedByWhenAnAdminRecordsIt() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingBalance(new BigDecimal("20000.00"));
        loan.setAmountRepaid(BigDecimal.ZERO);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        when(userAccountRepository.findById(9L)).thenReturn(Optional.of(admin));

        LoanRepaymentForm form = new LoanRepaymentForm();
        form.setLoanId(5L);
        form.setAmount(new BigDecimal("5000.00"));
        form.setPaymentMethod("CASH");

        service().repayLoan(form, 9L);

        ArgumentCaptor<LoanRepayment> captor = ArgumentCaptor.forClass(LoanRepayment.class);
        verify(loanRepaymentRepository).save(captor.capture());
        assertEquals(admin, captor.getValue().getReceivedBy());
    }

    @Test
    void getActiveLoansDelegatesToRepository() {
        Loan loan = new Loan();
        loan.setId(1L);
        when(loanRepository.findActiveOrOverdue()).thenReturn(List.of(loan));

        List<Loan> result = service().getActiveLoans();

        assertEquals(1, result.size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=LoanServiceTest test`
Expected: FAIL — `LoanRepaymentForm` has no `paymentMethod`, `LoanService.repayLoan` still takes one arg, `getActiveLoans()`/`findActiveOrOverdue()` don't exist.

- [ ] **Step 3: Add `paymentMethod` to `LoanRepaymentForm`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/dto/LoanRepaymentForm.java`:

```java
package org.joel.kimwanyisacco.dto;

import java.math.BigDecimal;

public class LoanRepaymentForm {

    private Long loanId;
    private BigDecimal amount;
    private String paymentMethod;

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
```

- [ ] **Step 4: Add `findActiveOrOverdue()` to `LoanRepository`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/repository/LoanRepository.java`:

```java
package org.joel.kimwanyisacco.repository;

import java.math.BigDecimal;

import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByStatus(LoanStatus status);

    java.util.List<Loan> findByMemberId(Long memberId);

    @Query("select l from Loan l join fetch l.member m join fetch m.userAccount where l.status = :status")
    java.util.List<Loan> findByStatus(@Param("status") LoanStatus status);

    @Query("select l from Loan l join fetch l.member m join fetch m.userAccount where l.status in ('ACTIVE','OVERDUE')")
    java.util.List<Loan> findActiveOrOverdue();

    @Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status in ('ACTIVE','OVERDUE')")
    BigDecimal sumOutstandingBalance();
}
```

(The `findActiveOrOverdue` query mirrors the existing `findByStatus`'s `JOIN FETCH member.userAccount` pattern — needed because `admin/loans.xhtml`'s Active Loans tab displays the member's name, and this codebase has hit `LazyInitializationException` more than once today from missing fetch joins on exactly this kind of query. The `'ACTIVE','OVERDUE'` string-literal-in-`IN`-clause style mirrors the existing `sumOutstandingBalance` query directly above it.)

- [ ] **Step 5: Update `LoanService` interface**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/service/LoanService.java`:

```java
package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;

public interface LoanService {

    Loan applyLoan(LoanApplicationForm form);

    Loan decideLoan(LoanDecisionForm form, Long adminUserId);

    void repayLoan(LoanRepaymentForm form, Long receivedByUserId);

    List<Loan> getPendingLoans();

    List<Loan> getActiveLoans();

    List<Loan> getLoansByMember(Long memberId);

    Loan getLoanById(Long loanId);
}
```

- [ ] **Step 6: Update `LoanServiceImpl`**

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
    public void repayLoan(LoanRepaymentForm form, Long receivedByUserId) {
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
        repayment.setPaymentMethod(form.getPaymentMethod());
        repayment.setReference("REP-" + System.currentTimeMillis());
        repayment.setPaymentDate(LocalDate.now());

        if (receivedByUserId != null) {
            UserAccount admin = userAccountRepository.findById(receivedByUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));
            repayment.setReceivedBy(admin);
        }

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
    public List<Loan> getActiveLoans() {
        return loanRepository.findActiveOrOverdue();
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

- [ ] **Step 7: Run test to verify it passes**

Run: `mvn -o -Dtest=LoanServiceTest test`
Expected: PASS (6 tests)

- [ ] **Step 8: Run the full suite to check for other breakage**

Run: `mvn -o test`
Expected: PASS — confirm no other caller of `repayLoan(form)` (one arg) exists: `grep -rn "\.repayLoan(" src/main src/test`. If one is found outside `LoanServiceImpl`/`LoanServiceTest`, update its call site to pass `null` as the second argument (member self-service — Task 2 will be the real caller going forward).

- [ ] **Step 9: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/dto/LoanRepaymentForm.java \
        src/main/java/org/joel/kimwanyisacco/service/LoanService.java \
        src/main/java/org/joel/kimwanyisacco/service/LoanServiceImpl.java \
        src/main/java/org/joel/kimwanyisacco/repository/LoanRepository.java \
        src/test/java/org/joel/kimwanyisacco/service/LoanServiceTest.java
git commit -m "Add payment method, receivedBy, and getActiveLoans to loan repayment"
```

---

### Task 2: `LoanRepaymentBean`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/controller/LoanRepaymentBean.java` (currently an empty stub class)
- Create: `src/test/java/org/joel/kimwanyisacco/controller/LoanRepaymentBeanTest.java`

**Interfaces:**
- Consumes: `LoanService.repayLoan(LoanRepaymentForm, Long)` (Task 1), `UserSessionBean.getLoggedInUser(): LoggedInUserDto` (existing, `.getId()`, `.getRoles(): List<String>`).
- Produces: `LoanRepaymentBean.prepare(Long loanId, BigDecimal outstandingBalance): void`, `getLoanId(): Long`, `getOutstandingBalance(): BigDecimal`, `getAmount()/setAmount(BigDecimal)`, `getPaymentMethod()/setPaymentMethod(String)`, `repay(): void`. Consumed by Tasks 4 and 5 (both member and admin repayment dialogs share this one bean).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.faces.context.FacesContext;
import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanRepaymentBeanTest {

    @Mock private LoanService loanService;
    @Mock private UserSessionBean userSessionBean;

    private LoanRepaymentBean bean() {
        return new LoanRepaymentBean(loanService, userSessionBean);
    }

    @Test
    void prepareStoresLoanIdAndOutstandingBalanceAndClearsPreviousInput() {
        LoanRepaymentBean bean = bean();
        bean.setAmount(new BigDecimal("999.00"));
        bean.setPaymentMethod("CASH");

        bean.prepare(7L, new BigDecimal("15000.00"));

        assertEquals(7L, bean.getLoanId());
        assertEquals(new BigDecimal("15000.00"), bean.getOutstandingBalance());
        assertNull(bean.getAmount());
        assertNull(bean.getPaymentMethod());
    }

    @Test
    void repayCallsServiceWithNullReceivedByForMemberSelfService() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(3L);
        dto.setRoles(List.of("MEMBER"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        LoanRepaymentBean bean = bean();
        bean.prepare(7L, new BigDecimal("15000.00"));
        bean.setAmount(new BigDecimal("5000.00"));
        bean.setPaymentMethod("CASH");

        bean.repay();

        ArgumentCaptor<LoanRepaymentForm> formCaptor = ArgumentCaptor.forClass(LoanRepaymentForm.class);
        verify(loanService).repayLoan(formCaptor.capture(), org.mockito.ArgumentMatchers.isNull());
        assertEquals(7L, formCaptor.getValue().getLoanId());
        assertEquals(new BigDecimal("5000.00"), formCaptor.getValue().getAmount());
        assertEquals("CASH", formCaptor.getValue().getPaymentMethod());
    }

    @Test
    void repayPassesAdminUserIdWhenLoggedInUserIsAdmin() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(9L);
        dto.setRoles(List.of("ADMIN"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        LoanRepaymentBean bean = bean();
        bean.prepare(7L, new BigDecimal("15000.00"));
        bean.setAmount(new BigDecimal("5000.00"));
        bean.setPaymentMethod("MOBILE_MONEY");

        bean.repay();

        verify(loanService).repayLoan(any(LoanRepaymentForm.class), org.mockito.ArgumentMatchers.eq(9L));
    }

    @Test
    void repayAddsErrorMessageAndDoesNotCallServiceWhenAmountMissing() {
        LoanRepaymentBean bean = bean();
        bean.prepare(7L, new BigDecimal("15000.00"));
        bean.setPaymentMethod("CASH");

        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            bean.repay();

            verify(facesContext).addMessage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());
        }
        verify(loanService, org.mockito.Mockito.never()).repayLoan(any(), any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=LoanRepaymentBeanTest test`
Expected: FAIL — `LoanRepaymentBean` has no constructor/methods yet (it's an empty class).

- [ ] **Step 3: Implement `LoanRepaymentBean`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/controller/LoanRepaymentBean.java`:

```java
package org.joel.kimwanyisacco.controller;

import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("loanRepaymentBean")
@SessionScope
public class LoanRepaymentBean {

    private final LoanService loanService;
    private final UserSessionBean userSessionBean;

    private Long loanId;
    private BigDecimal outstandingBalance;
    private BigDecimal amount;
    private String paymentMethod;

    public LoanRepaymentBean(LoanService loanService, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.userSessionBean = userSessionBean;
    }

    /** Called when a repayment dialog is opened for a specific loan (member or admin page). */
    public void prepare(Long loanId, BigDecimal outstandingBalance) {
        this.loanId = loanId;
        this.outstandingBalance = outstandingBalance;
        this.amount = null;
        this.paymentMethod = null;
    }

    public void repay() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Please enter a valid repayment amount.");
            return;
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            FacesMessageUtil.addErrorMessage("Please select a payment method.");
            return;
        }

        try {
            LoanRepaymentForm form = new LoanRepaymentForm();
            form.setLoanId(loanId);
            form.setAmount(amount);
            form.setPaymentMethod(paymentMethod);

            Long receivedByUserId = isCurrentUserAdmin() ? userSessionBean.getLoggedInUser().getId() : null;

            loanService.repayLoan(form, receivedByUserId);

            FacesMessageUtil.addInfoMessage("Repayment of UGX " + amount.toPlainString() + " recorded successfully.");
            amount = null;
            paymentMethod = null;
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    private boolean isCurrentUserAdmin() {
        return userSessionBean.getLoggedInUser() != null
                && userSessionBean.getLoggedInUser().getRoles() != null
                && userSessionBean.getLoggedInUser().getRoles().contains("ADMIN");
    }

    public Long getLoanId() {
        return loanId;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -o -Dtest=LoanRepaymentBeanTest test`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/LoanRepaymentBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/LoanRepaymentBeanTest.java
git commit -m "Implement LoanRepaymentBean for member and admin repayment recording"
```

---

### Task 3: Extend `LoanApprovalBean` with active loans

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/controller/LoanApprovalBean.java`
- Create: `src/test/java/org/joel/kimwanyisacco/controller/LoanApprovalBeanTest.java`

**Interfaces:**
- Consumes: `LoanService.getActiveLoans(): List<Loan>` (Task 1).
- Produces: `LoanApprovalBean.getActiveLoans(): List<Loan>`, loaded on `@PostConstruct` alongside the existing `pendingLoans`. Consumed by Task 5 (admin page's Active Loans tab).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanApprovalBeanTest {

    @Mock private LoanService loanService;
    @Mock private UserSessionBean userSessionBean;

    @Test
    void initLoadsBothPendingAndActiveLoans() {
        Loan pending = new Loan();
        Loan active = new Loan();
        when(loanService.getPendingLoans()).thenReturn(List.of(pending));
        when(loanService.getActiveLoans()).thenReturn(List.of(active));

        LoanApprovalBean bean = new LoanApprovalBean(loanService, userSessionBean);
        bean.init();

        assertEquals(1, bean.getPendingLoans().size());
        assertEquals(1, bean.getActiveLoans().size());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -o -Dtest=LoanApprovalBeanTest test`
Expected: FAIL — `LoanApprovalBean` has no `init()`/`getActiveLoans()` matching this yet (or `init()` exists but doesn't load active loans).

- [ ] **Step 3: Extend `LoanApprovalBean`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/controller/LoanApprovalBean.java`:

```java
package org.joel.kimwanyisacco.controller;

import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.annotation.PostConstruct;

@Component("loanApprovalBean")
@RequestScope
public class LoanApprovalBean {

    private final LoanService loanService;
    private final UserSessionBean userSessionBean;

    private List<Loan> pendingLoans;
    private List<Loan> activeLoans;
    private Long selectedLoanId;
    private String rejectionReason;

    public LoanApprovalBean(LoanService loanService, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        loadPendingLoans();
        loadActiveLoans();
    }

    public void loadPendingLoans() {
        try {
            pendingLoans = loanService.getPendingLoans();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load pending loans: " + e.getMessage());
        }
    }

    public void loadActiveLoans() {
        try {
            activeLoans = loanService.getActiveLoans();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load active loans: " + e.getMessage());
        }
    }

    public void approve(Long loanId) {
        try {
            if (!userSessionBean.isLoggedIn()) {
                throw new IllegalStateException("You must be logged in to approve loans");
            }
            LoanDecisionForm form = new LoanDecisionForm();
            form.setLoanId(loanId);
            form.setApproved(true);
            
            loanService.decideLoan(form, userSessionBean.getLoggedInUser().getId());
            loadPendingLoans();
            FacesMessageUtil.addInfoMessage("Loan approved successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public void reject() {
        try {
            if (!userSessionBean.isLoggedIn()) {
                throw new IllegalStateException("You must be logged in to reject loans");
            }
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required");
            }
            LoanDecisionForm form = new LoanDecisionForm();
            form.setLoanId(selectedLoanId);
            form.setApproved(false);
            form.setRemarks(rejectionReason);

            loanService.decideLoan(form, userSessionBean.getLoggedInUser().getId());
            
            rejectionReason = null;
            selectedLoanId = null;
            
            loadPendingLoans();
            FacesMessageUtil.addInfoMessage("Loan rejected successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    // Getters and Setters
    public List<Loan> getPendingLoans() {
        return pendingLoans;
    }

    public List<Loan> getActiveLoans() {
        return activeLoans;
    }

    public Long getSelectedLoanId() {
        return selectedLoanId;
    }

    public void setSelectedLoanId(Long selectedLoanId) {
        this.selectedLoanId = selectedLoanId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -o -Dtest=LoanApprovalBeanTest test`
Expected: PASS (1 test)

- [ ] **Step 5: Run the full suite to check for other breakage**

Run: `mvn -o test`
Expected: PASS — this change is additive to `LoanApprovalBean`'s public interface, existing `approve()`/`reject()` behavior is untouched.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/LoanApprovalBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/LoanApprovalBeanTest.java
git commit -m "Load active loans in LoanApprovalBean for the admin Active Loans tab"
```

---

### Task 4: Member-facing repayment UI (`loans/applications.xhtml`)

**Files:**
- Modify: `src/main/webapp/loans/applications.xhtml`

**Interfaces:**
- Consumes: `loanRepaymentBean.prepare(Long, BigDecimal)`, `getLoanId()`, `getOutstandingBalance()`, `getAmount()/setAmount`, `getPaymentMethod()/setPaymentMethod`, `repay()` (Task 2).

- [ ] **Step 1: Add an Actions column and the repayment dialog**

Replace the entire contents of `src/main/webapp/loans/applications.xhtml`:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                xmlns:p="http://primefaces.org/ui"
                template="/WEB-INF/templates/member-template.xhtml">

    <ui:define name="title">My Loans | Kimwanyi SACCO</ui:define>
    <ui:define name="pageTitle">My Loans</ui:define>

    <ui:define name="content">

        <div class="mem-card">
            <div class="mem-card-header">
                <span class="mem-card-title"><i class="fa-solid fa-list-check" style="color:#dc2626; margin-right:6px;"></i> Loan Applications</span>
                <h:link outcome="/members/dashboard" styleClass="mem-btn mem-btn-primary">
                    <i class="fa-solid fa-plus"></i> Apply for Loan
                </h:link>
            </div>
            <div class="mem-card-body">

                <h:panelGroup rendered="#{empty memberLoansBean.loans}">
                    <div class="mem-empty">
                        <i class="fa-solid fa-hand-holding-dollar"></i>
                        <strong style="color:#374151;">No Loan Applications Yet</strong>
                        <p>You haven't applied for a loan yet.</p>
                        <h:link outcome="/members/dashboard" styleClass="mem-btn mem-btn-primary" style="margin-top:0.5rem;">
                            <i class="fa-solid fa-plus"></i> Apply for Loan
                        </h:link>
                    </div>
                </h:panelGroup>

                <h:panelGroup rendered="#{not empty memberLoansBean.loans}">
                    <h:form id="loansForm">
                    <table class="mem-table">
                        <thead>
                            <tr>
                                <th>Applied</th>
                                <th>Principal</th>
                                <th>Interest</th>
                                <th>Total Repayable</th>
                                <th>Outstanding</th>
                                <th>Status</th>
                                <th>Due Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <ui:repeat value="#{memberLoansBean.loans}" var="loan">
                                <tr>
                                    <td>
                                        <h:outputText value="#{loan.applicationDate}">
                                            <f:convertDateTime type="localDate" pattern="dd MMM yyyy"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <h:outputText value="#{loan.principal}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <h:outputText value="#{loan.interestAmount}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <h:outputText value="#{loan.totalRepayable}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <h:outputText value="#{loan.outstandingBalance}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <span class="badge #{memberLoansBean.badgeClass(loan)}">#{loan.status}</span>
                                    </td>
                                    <td>
                                        <h:outputText value="#{loan.dueDate}" rendered="#{loan.dueDate != null}">
                                            <f:convertDateTime type="localDate" pattern="dd MMM yyyy"/>
                                        </h:outputText>
                                        <h:outputText value="—" rendered="#{loan.dueDate == null}"/>
                                    </td>
                                    <td>
                                        <p:commandLink rendered="#{loan.status == 'ACTIVE' or loan.status == 'OVERDUE'}"
                                                       action="#{loanRepaymentBean.prepare(loan.id, loan.outstandingBalance)}"
                                                       oncomplete="PF('repayDialogWidget').show();" update=":repayForm"
                                                       style="text-decoration:none; color:#dc2626; font-weight:600; font-size:0.82rem;">
                                            <i class="fa-solid fa-money-bill-wave"></i> Repay
                                        </p:commandLink>
                                    </td>
                                </tr>
                            </ui:repeat>
                        </tbody>
                    </table>
                    </h:form>
                </h:panelGroup>

            </div>
        </div>

        <p:dialog header="Record Repayment" widgetVar="repayDialogWidget" modal="true" responsive="true" width="420" resizable="false">
            <h:form id="repayForm">
                <div style="display:flex; flex-direction:column; gap:1rem; padding:0.5rem 0;">

                    <div class="mem-helper-box">
                        Outstanding balance: <strong>
                            <h:outputText value="#{loanRepaymentBean.outstandingBalance}">
                                <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                            </h:outputText>
                        </strong>
                    </div>

                    <div class="mem-field">
                        <h:outputLabel for="repayAmount" value="Amount (UGX)"/>
                        <h:inputText id="repayAmount" value="#{loanRepaymentBean.amount}" styleClass="mem-input">
                            <f:convertNumber type="number" maxFractionDigits="2"/>
                        </h:inputText>
                    </div>

                    <div class="mem-field">
                        <h:outputLabel for="repayMethod" value="Payment Method"/>
                        <h:selectOneMenu id="repayMethod" value="#{loanRepaymentBean.paymentMethod}" styleClass="mem-input">
                            <f:selectItem itemLabel="Select a method" itemValue="#{null}" noSelectionOption="true"/>
                            <f:selectItem itemLabel="Cash" itemValue="CASH"/>
                            <f:selectItem itemLabel="Mobile Money" itemValue="MOBILE_MONEY"/>
                            <f:selectItem itemLabel="Bank Transfer" itemValue="BANK_TRANSFER"/>
                        </h:selectOneMenu>
                    </div>

                    <div style="display:flex; justify-content:flex-end; gap:0.5rem;">
                        <button type="button" class="mem-btn-cancel" onclick="PF('repayDialogWidget').hide();">Cancel</button>
                        <p:commandButton value="Submit Repayment" action="#{loanRepaymentBean.repay}"
                                         update=":loansForm :notifBell :notifPanel :growl"
                                         oncomplete="PF('repayDialogWidget').show();"
                                         styleClass="mem-btn mem-btn-primary"/>
                    </div>
                </div>
            </h:form>
        </p:dialog>

    </ui:define>
</ui:composition>
```

(The `oncomplete="PF('repayDialogWidget').show();"` on the submit button follows the exact same fix already established for the notification bell earlier today — PrimeFaces detaches an open dialog from its template position, so re-showing it after an ajax update keeps it visibly in sync with the fresh data rather than looking stuck. `update` also includes `:notifBell :notifPanel` so the bell badge reflects the new `LOAN_REPAYMENT` notification without a page reload, and `:growl` so the success/error `FacesMessageUtil` message shows.)

- [ ] **Step 2: Verify the build packages**

Run: `mvn -o -q war:exploded` (no test changes in this task — verification is via Task 6's manual pass)
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/loans/applications.xhtml
git commit -m "Add self-service loan repayment to the My Loans page"
```

---

### Task 5: Admin-facing repayment UI (`admin/loans.xhtml`)

**Files:**
- Modify: `src/main/webapp/admin/loans.xhtml`

**Interfaces:**
- Consumes: `loanApprovalBean.activeLoans` (Task 3), `loanRepaymentBean.prepare/repay` (Task 2).

- [ ] **Step 1: Wrap existing content in a tab view and add the Active Loans tab**

Replace the entire contents of `src/main/webapp/admin/loans.xhtml`:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                xmlns:p="http://primefaces.org/ui"
                template="/WEB-INF/templates/admin-template.xhtml">

    <ui:define name="title">Loan Approvals | Admin</ui:define>

    <ui:define name="pageHeader">Loan Management</ui:define>

    <ui:define name="content">
        <div class="section-card">
            <p:tabView>
                <p:tab title="Pending Applications">
                    <div class="section-body" style="padding: 0;">
                        <h:form id="loansForm">
                            <h:dataTable id="loansTable" value="#{loanApprovalBean.pendingLoans}" var="loan"
                                         styleClass="activity-table" style="width: 100%; border-collapse: collapse; text-align: left;">

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Member</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem;">
                                        <div style="font-weight: 600; color: #1f2937;">#{loan.member.userAccount.firstName} #{loan.member.userAccount.lastName}</div>
                                        <div style="font-size: 0.8rem; color: #6b7280;">No: #{loan.member.membershipNumber}</div>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Principal</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem; font-weight: 600; color: #111827;">
                                        <h:outputText value="#{loan.principal}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Interest (10% Flat)</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem; color: #dc2626;">
                                        <h:outputText value="#{loan.interestAmount}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Total Repayable</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem; font-weight: 600; color: #059669;">
                                        <h:outputText value="#{loan.totalRepayable}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Purpose</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.85rem; color: #4b5563; max-width: 200px;">
                                        #{loan.purpose}
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Applied On</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.85rem; color: #6b7280;">
                                        <h:outputText value="#{loan.applicationDate}">
                                            <f:convertDateTime type="localDate" pattern="dd MMM yyyy"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500; text-align: right;">Actions</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; text-align: right; display: flex; gap: 0.5rem; justify-content: flex-end;">
                                        <h:commandLink action="#{loanApprovalBean.approve(loan.id)}" styleClass="action-btn-approve" style="text-decoration:none; background:#dcfce7; color:#15803d; padding:6px 12px; border-radius:4px; font-weight:600; font-size:0.8rem; display:inline-flex; align-items:center; gap:4px;">
                                            <i class="fa-solid fa-check"></i> Approve
                                            <f:ajax render="@form :growl"/>
                                        </h:commandLink>

                                        <p:commandLink action="#{loanApprovalBean.setSelectedLoanId(loan.id)}"
                                                       oncomplete="PF('rejectDialog').show();"
                                                       update=":rejectForm"
                                                       styleClass="action-btn-reject"
                                                       style="text-decoration:none; background:#fee2e2; color:#b91c1c; padding:6px 12px; border-radius:4px; font-weight:600; font-size:0.8rem; display:inline-flex; align-items:center; gap:4px;">
                                            <i class="fa-solid fa-xmark"></i> Reject
                                        </p:commandLink>
                                    </div>
                                </h:column>

                            </h:dataTable>

                            <h:panelGroup rendered="#{empty loanApprovalBean.pendingLoans}">
                                <div style="padding: 4rem; text-align: center; color: #9ca3af;">
                                    <i class="fa-solid fa-hand-holding-dollar fa-3x" style="margin-bottom: 1rem; color: #d1d5db;"></i><br/>
                                    No pending loan applications found.
                                </div>
                            </h:panelGroup>
                        </h:form>
                    </div>
                </p:tab>

                <p:tab title="Active Loans">
                    <div class="section-body" style="padding: 0;">
                        <h:form id="activeLoansForm">
                            <h:dataTable id="activeLoansTable" value="#{loanApprovalBean.activeLoans}" var="loan"
                                         styleClass="activity-table" style="width: 100%; border-collapse: collapse; text-align: left;">

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Member</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem;">
                                        <div style="font-weight: 600; color: #1f2937;">#{loan.member.userAccount.firstName} #{loan.member.userAccount.lastName}</div>
                                        <div style="font-size: 0.8rem; color: #6b7280;">No: #{loan.member.membershipNumber}</div>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Principal</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem; font-weight: 600; color: #111827;">
                                        <h:outputText value="#{loan.principal}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Outstanding</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.9rem; font-weight: 600; color: #dc2626;">
                                        <h:outputText value="#{loan.outstandingBalance}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Due Date</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; font-size: 0.85rem; color: #6b7280;">
                                        <h:outputText value="#{loan.dueDate}" rendered="#{loan.dueDate != null}">
                                            <f:convertDateTime type="localDate" pattern="dd MMM yyyy"/>
                                        </h:outputText>
                                        <h:outputText value="—" rendered="#{loan.dueDate == null}"/>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500;">Status</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6;">
                                        <span class="badge #{loan.status == 'OVERDUE' ? 'badge-red' : 'badge-blue'}">#{loan.status}</span>
                                    </div>
                                </h:column>

                                <h:column>
                                    <f:facet name="header"><div style="padding:15px; border-bottom:1px solid #e5e7eb; color:#9ca3af; font-size:0.8rem; font-weight: 500; text-align: right;">Actions</div></f:facet>
                                    <div style="padding:15px; border-bottom:1px dashed #f3f4f6; text-align: right;">
                                        <p:commandLink action="#{loanRepaymentBean.prepare(loan.id, loan.outstandingBalance)}"
                                                       oncomplete="PF('repayDialogWidget').show();" update=":repayForm"
                                                       style="text-decoration:none; background:#eef2ff; color:#4f46e5; padding:6px 12px; border-radius:4px; font-weight:600; font-size:0.8rem; display:inline-flex; align-items:center; gap:4px;">
                                            <i class="fa-solid fa-money-bill-wave"></i> Record Repayment
                                        </p:commandLink>
                                    </div>
                                </h:column>

                            </h:dataTable>

                            <h:panelGroup rendered="#{empty loanApprovalBean.activeLoans}">
                                <div style="padding: 4rem; text-align: center; color: #9ca3af;">
                                    <i class="fa-solid fa-circle-check fa-3x" style="margin-bottom: 1rem; color: #d1d5db;"></i><br/>
                                    No active loans found.
                                </div>
                            </h:panelGroup>
                        </h:form>
                    </div>
                </p:tab>
            </p:tabView>
        </div>

        <!-- Reject Reason Dialog -->
        <p:dialog header="Reject Loan Application" widgetVar="rejectDialog" modal="true" responsive="true" width="400" resizable="false">
            <h:form id="rejectForm">
                <div style="display: flex; flex-direction: column; gap: 1rem; padding: 0.5rem 0;">
                    <div style="display: flex; flex-direction: column; gap: 0.25rem;">
                        <h:outputLabel for="rejectReason" value="Rejection Reason" style="font-weight: 600; font-size: 0.85rem; color: #4b5563;"/>
                        <p:inputTextarea id="rejectReason" value="#{loanApprovalBean.rejectionReason}" rows="4" autoResize="false" style="padding: 0.5rem; border-radius: 0.375rem; border: 1px solid var(--border-color); width: 100%; box-sizing: border-box;" placeholder="Enter details on why this application is rejected..."/>
                    </div>

                    <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem;">
                        <p:commandButton value="Cancel" onclick="PF('rejectDialog').hide();" type="button" styleClass="ui-button-flat ui-button-secondary"/>
                        <p:commandButton value="Submit Rejection" action="#{loanApprovalBean.reject}" update=":loansForm :growl" oncomplete="if (!args.validationFailed) PF('rejectDialog').hide();" styleClass="ui-button-danger"/>
                    </div>
                </div>
            </h:form>
        </p:dialog>

        <!-- Record Repayment Dialog (shared markup/bean with the member-facing page) -->
        <p:dialog header="Record Repayment" widgetVar="repayDialogWidget" modal="true" responsive="true" width="420" resizable="false">
            <h:form id="repayForm">
                <div style="display: flex; flex-direction: column; gap: 1rem; padding: 0.5rem 0;">

                    <div style="background:#f9fafb; padding:1rem; border-radius:0.5rem; border:1px solid #e5e7eb; font-size:0.85rem;">
                        Outstanding balance: <strong style="color:#dc2626;">
                            <h:outputText value="#{loanRepaymentBean.outstandingBalance}">
                                <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                            </h:outputText>
                        </strong>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 0.25rem;">
                        <h:outputLabel for="adminRepayAmount" value="Amount (UGX)" style="font-weight: 600; font-size: 0.85rem; color: #4b5563;"/>
                        <h:inputText id="adminRepayAmount" value="#{loanRepaymentBean.amount}" style="padding: 0.5rem; border-radius: 0.375rem; border: 1px solid var(--border-color); width: 100%; box-sizing: border-box;">
                            <f:convertNumber type="number" maxFractionDigits="2"/>
                        </h:inputText>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 0.25rem;">
                        <h:outputLabel for="adminRepayMethod" value="Payment Method" style="font-weight: 600; font-size: 0.85rem; color: #4b5563;"/>
                        <h:selectOneMenu id="adminRepayMethod" value="#{loanRepaymentBean.paymentMethod}" style="padding: 0.5rem; border-radius: 0.375rem; border: 1px solid var(--border-color); background: white;">
                            <f:selectItem itemLabel="Select a method" itemValue="#{null}" noSelectionOption="true"/>
                            <f:selectItem itemLabel="Cash" itemValue="CASH"/>
                            <f:selectItem itemLabel="Mobile Money" itemValue="MOBILE_MONEY"/>
                            <f:selectItem itemLabel="Bank Transfer" itemValue="BANK_TRANSFER"/>
                        </h:selectOneMenu>
                    </div>

                    <div style="display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.5rem;">
                        <p:commandButton value="Cancel" onclick="PF('repayDialogWidget').hide();" type="button" styleClass="ui-button-flat ui-button-secondary"/>
                        <p:commandButton value="Submit Repayment" action="#{loanRepaymentBean.repay}"
                                         update=":activeLoansForm :notifBell :notifPanel :growl"
                                         oncomplete="if (!args.validationFailed) PF('repayDialogWidget').hide();"
                                         styleClass="ui-button-success"/>
                    </div>
                </div>
            </h:form>
        </p:dialog>
    </ui:define>
</ui:composition>
```

(Note: the member page's repay dialog re-shows itself via `oncomplete="PF('repayDialogWidget').show();"` unconditionally — matching the notification bell's established fix, since the outstanding-balance line inside needs to refresh after a successful repayment. The admin dialog instead hides on success and only re-shows implicitly never — it follows the exact same idiom as the existing reject dialog above it (`if (!args.validationFailed) PF('...').hide();`), since after an admin records a repayment the natural flow is to close the dialog and glance at the refreshed Active Loans table, not keep the dialog open. Both are reasonable; this mirrors the closest existing precedent on each respective page.)

- [ ] **Step 2: Verify the build packages**

Run: `mvn -o -q war:exploded`
Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/admin/loans.xhtml
git commit -m "Add Active Loans tab with repayment recording to admin loan management"
```

---

### Task 6: Manual end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `mvn -o test`
Expected: PASS, all tests green.

- [ ] **Step 2: Rebuild and restart the app**

```bash
mvn -o -q war:exploded
```
Restart the Tomcat process serving `kimwanyi_sacco_war_exploded` (a Java-level change in this plan needs a JVM restart, not just a rebuild).

- [ ] **Step 3: Member self-service repayment**
   - Log in as a member with an `ACTIVE` loan (or create one: apply for a loan as a member with savings balance, then approve it as admin).
   - Go to My Loans, confirm a "Repay" link appears only on the `ACTIVE` row (not on `PENDING`/`FULLY_REPAID` rows if any exist).
   - Click Repay, confirm the dialog shows the correct outstanding balance, submit a partial repayment with a payment method selected.
   - Confirm: outstanding balance decreases correctly, a success message appears, the notification bell badge increments, and the bell shows "Repayment Recorded" with the correct amount/remaining balance.
   - Repay the remaining balance in a second repayment; confirm the loan's status badge changes to `FULLY_REPAID` and the Repay link disappears for that row.

- [ ] **Step 4: Admin-recorded repayment**
   - As admin, open Loan Approvals → Active Loans tab, confirm it lists loans with `ACTIVE`/`OVERDUE` status and the correct member names (verifying the `JOIN FETCH` in `findActiveOrOverdue` avoids `LazyInitializationException` — check the Tomcat log for any 500 during this step, this codebase has hit exactly this failure mode multiple times today).
   - Click "Record Repayment" on a loan, submit with a payment method, confirm the outstanding balance updates in the table and the member later sees the correct `LOAN_REPAYMENT` notification.
   - Directly query the database to confirm `loan_repayments.received_by` is set to the admin's user id for this repayment, and `payment_method` matches what was selected (not hardcoded `"CASH"`).

- [ ] **Step 5: Validation edge cases**
   - Attempt a repayment with an amount exceeding the outstanding balance — confirm a clear error message and no state change.
   - Attempt a repayment with no payment method selected — confirm the bean's own validation message appears (not a raw exception).

No commit for this task — it's verification of Tasks 1–5.
