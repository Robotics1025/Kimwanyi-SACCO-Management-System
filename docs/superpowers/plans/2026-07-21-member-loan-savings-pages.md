# Member Self-Service Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build three working member-portal pages — Apply for Loan, My Loans, Savings History — backed by real service logic, plus fix the gap where registering a member never creates their savings account.

**Architecture:** JSF/Facelets views under `member-template.xhtml` backed by Spring-managed `@RequestScope` controller beans, calling the existing `LoanService`/`SavingsService` interfaces. `SavingsServiceImpl`'s read paths and two new entity→DTO converters are implemented for the first time; `LoanService` is already fully implemented and just needs wiring.

**Tech Stack:** Jakarta Faces (MyFaces) + Facelets, Spring (constructor injection, `@Component`/`@Service`), JPA/Hibernate, JUnit 5 + Mockito.

## Global Constraints

- Max loan amount = 3x member's current savings balance (`LoanEligibilityPolicy`, already implemented — do not duplicate the enforcement, only mirror it for UI hints).
- Flat 10% loan interest on principal (`LoanInterestCalculator`, already implemented).
- A member may only hold one PENDING/ACTIVE/OVERDUE loan at a time (already enforced server-side).
- Minimum savings balance UGX 20,000 (`SavingsAccount.minimumBalance` default — do not change).
- New "not found" errors in `SavingsServiceImpl` use `ResourceNotFoundException` (`common/exception/ResourceNotFoundException`), matching the pattern in `UserManagementServiceImpl` (not `IllegalArgumentException`, which is what the older `LoanServiceImpl`/`MemberServiceImpl` use — this file is new code, so it uses the more precise exception type).
- Follow the existing controller convention exactly (see `LoanApprovalBean`): `@Component("beanName") @RequestScope`, constructor injection, `@PostConstruct init()`, `FacesMessageUtil.addInfoMessage`/`addErrorMessage` for feedback, catch generic `Exception` around service calls.
- Entity→DTO conversion convention: one `XxxConverter` class per entity in `common/util/converter`, `@Component`, method named `toDto(Entity)` (see `UserAccountConverter`, `AuditLogConverter`).
- Currency formatting in views: `<f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>` (see `admin/dashboard.xhtml`).

---

## File Structure

**New files:**
- `src/main/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGenerator.java`
- `src/test/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGeneratorTest.java`
- `src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsAccountConverter.java`
- `src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsTransactionConverter.java`
- `src/main/java/org/joel/kimwanyisacco/controller/MemberLoansBean.java`
- `src/test/java/org/joel/kimwanyisacco/controller/MemberLoansBeanTest.java`
- `src/test/java/org/joel/kimwanyisacco/controller/LoanApplicationBeanTest.java`
- `src/test/java/org/joel/kimwanyisacco/controller/SavingsHistoryBeanTest.java`

**Modified files:**
- `src/main/java/org/joel/kimwanyisacco/repository/SavingsAccountRepository.java` — add `countByAccountNumberStartingWith`.
- `src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java` — auto-create `SavingsAccount` on registration.
- `src/test/java/org/joel/kimwanyisacco/service/MemberServiceTest.java` — un-disable, add real tests.
- `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java` — implement `getAccountById`, `getTransactionHistory`.
- `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java` — un-disable, add real tests.
- `src/main/java/org/joel/kimwanyisacco/controller/LoanApplicationBean.java` — full rewrite.
- `src/main/java/org/joel/kimwanyisacco/controller/SavingsHistoryBean.java` — add loading logic.
- `src/main/webapp/loans/apply.xhtml` — build out.
- `src/main/webapp/loans/applications.xhtml` — build out.
- `src/main/webapp/savings/history.xhtml` — build out.
- `src/main/webapp/resources/css/member.css` — add form field classes.

---

### Task 1: `SavingsAccountNumberGenerator`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/repository/SavingsAccountRepository.java`
- Create: `src/main/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGenerator.java`
- Test: `src/test/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGeneratorTest.java`

**Interfaces:**
- Produces: `SavingsAccountNumberGenerator.generate(): String`, format `SAV-{year}-{seq:04d}` (e.g. `SAV-2026-0001`). Consumed by Task 2.

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsAccountNumberGeneratorTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Test
    void generatesFirstAccountNumberOfTheYear() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(0L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0001")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0001", generator.generate());
    }

    @Test
    void continuesSequenceFromExistingCountForTheYear() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(41L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0042")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0042", generator.generate());
    }

    @Test
    void incrementsPastCollisionsUntilAnUnusedNumberIsFound() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(0L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0001")).thenReturn(true);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0002")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0002", generator.generate());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=SavingsAccountNumberGeneratorTest test`
Expected: FAIL — compile error, `SavingsAccountNumberGenerator` and `countByAccountNumberStartingWith` don't exist yet.

- [ ] **Step 3: Add the repository method**

In `src/main/java/org/joel/kimwanyisacco/repository/SavingsAccountRepository.java`, add this method inside the interface (alongside the existing ones):

```java
    long countByAccountNumberStartingWith(String prefix);
```

Full resulting file:

```java
package org.joel.kimwanyisacco.repository;

import java.math.BigDecimal;
import java.util.Optional;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    Optional<SavingsAccount> findByMemberId(Long memberId);

    boolean existsByAccountNumber(String accountNumber);

    long countByAccountNumberStartingWith(String prefix);

    @Query("select coalesce(sum(s.balance), 0) from SavingsAccount s")
    BigDecimal sumAllBalances();
}
```

- [ ] **Step 4: Write the generator**

```java
package org.joel.kimwanyisacco.common.util;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.springframework.stereotype.Component;

@Component
public class SavingsAccountNumberGenerator {

    private final SavingsAccountRepository savingsAccountRepository;

    public SavingsAccountNumberGenerator(SavingsAccountRepository savingsAccountRepository) {
        this.savingsAccountRepository = savingsAccountRepository;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "SAV-" + year + "-";

        long sequence = savingsAccountRepository.countByAccountNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (savingsAccountRepository.existsByAccountNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -Dtest=SavingsAccountNumberGeneratorTest test`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/repository/SavingsAccountRepository.java \
        src/main/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGenerator.java \
        src/test/java/org/joel/kimwanyisacco/common/util/SavingsAccountNumberGeneratorTest.java
git commit -m "Add SavingsAccountNumberGenerator for account provisioning"
```

---

### Task 2: Auto-create savings account at member registration

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/MemberServiceTest.java`

**Interfaces:**
- Consumes: `SavingsAccountNumberGenerator.generate(): String` (Task 1), `SavingsAccountRepository.save(SavingsAccount): SavingsAccount` (existing).
- Produces: every successful `MemberServiceImpl.registerMember()` call now also persists a `SavingsAccount` with `balance = BigDecimal.ZERO`, linked to the new `Member`. Consumed by Tasks 4–6 (a member always has an account by the time they can apply for a loan or view savings).

- [ ] **Step 1: Write the failing test**

Replace the entire contents of `src/test/java/org/joel/kimwanyisacco/service/MemberServiceTest.java`:

```java
package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=MemberServiceTest test`
Expected: FAIL — `MemberServiceImpl` constructor doesn't accept `SavingsAccountRepository`/`SavingsAccountNumberGenerator` yet.

- [ ] **Step 3: Update `MemberServiceImpl`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java`:

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=MemberServiceTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Run the full test suite to check for other breakage**

Run: `mvn -q test`
Expected: PASS — no other test constructs `MemberServiceImpl` directly (confirm via `grep -rn "new MemberServiceImpl" src/test`; if any other test does, add the two new mock parameters there too).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java \
        src/test/java/org/joel/kimwanyisacco/service/MemberServiceTest.java
git commit -m "Auto-provision savings account when a member registers"
```

---

### Task 3: Implement `SavingsServiceImpl` read paths

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsAccountConverter.java`
- Create: `src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsTransactionConverter.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java`
- Modify: `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java`

**Interfaces:**
- Produces: `SavingsService.getAccountById(Long): SavingsAccountDto` (throws `ResourceNotFoundException` if missing), `SavingsService.getTransactionHistory(Long): List<SavingsTransactionDto>` (newest-first). Consumed by Task 6 (`SavingsHistoryBean`).

- [ ] **Step 1: Write the failing test**

Replace the entire contents of `src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java`:

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
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.joel.kimwanyisacco.model.enums.TransactionType;
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

    private final SavingsAccountConverter savingsAccountConverter = new SavingsAccountConverter();
    private final SavingsTransactionConverter savingsTransactionConverter = new SavingsTransactionConverter();

    private SavingsServiceImpl service() {
        return new SavingsServiceImpl(
                savingsAccountRepository, savingsTransactionRepository,
                savingsAccountConverter, savingsTransactionConverter);
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

Run: `mvn -q -Dtest=SavingsServiceTest test`
Expected: FAIL — `SavingsServiceImpl` has no such constructor, converters don't exist yet.

- [ ] **Step 3: Write the converters**

```java
package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.springframework.stereotype.Component;

@Component
public class SavingsAccountConverter {

    public SavingsAccountDto toDto(SavingsAccount account) {
        SavingsAccountDto dto = new SavingsAccountDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMemberId(account.getMember() != null ? account.getMember().getId() : null);
        dto.setBalance(account.getBalance());
        return dto;
    }
}
```

```java
package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.springframework.stereotype.Component;

@Component
public class SavingsTransactionConverter {

    public SavingsTransactionDto toDto(SavingsTransaction transaction) {
        SavingsTransactionDto dto = new SavingsTransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionType(transaction.getType() != null ? transaction.getType().name() : null);
        dto.setAmount(transaction.getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setCreatedAt(transaction.getCreatedAt());
        return dto;
    }
}
```

- [ ] **Step 4: Implement `SavingsServiceImpl`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java`:

```java
package org.joel.kimwanyisacco.service;

import java.util.Comparator;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class SavingsServiceImpl implements SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final SavingsAccountConverter savingsAccountConverter;
    private final SavingsTransactionConverter savingsTransactionConverter;

    public SavingsServiceImpl(
            SavingsAccountRepository savingsAccountRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            SavingsAccountConverter savingsAccountConverter,
            SavingsTransactionConverter savingsTransactionConverter
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.savingsAccountConverter = savingsAccountConverter;
        this.savingsTransactionConverter = savingsTransactionConverter;
    }

    @Override
    public SavingsTransactionDto deposit(DepositForm form) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SavingsTransactionDto withdraw(WithdrawalForm form) {
        throw new UnsupportedOperationException("not implemented");
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
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -Dtest=SavingsServiceTest test`
Expected: PASS (3 tests)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsAccountConverter.java \
        src/main/java/org/joel/kimwanyisacco/common/util/converter/SavingsTransactionConverter.java \
        src/main/java/org/joel/kimwanyisacco/service/SavingsServiceImpl.java \
        src/test/java/org/joel/kimwanyisacco/service/SavingsServiceTest.java
git commit -m "Implement SavingsServiceImpl read paths for account and history"
```

---

### Task 4: `LoanApplicationBean` (Apply for Loan controller)

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/controller/LoanApplicationBean.java`
- Create: `src/test/java/org/joel/kimwanyisacco/controller/LoanApplicationBeanTest.java`

**Interfaces:**
- Consumes: `LoanService.applyLoan(LoanApplicationForm): Loan`, `LoanService.getLoansByMember(Long): List<Loan>` (existing), `MemberRepository.findByUserAccountId(Long): Optional<Member>` (existing), `SavingsAccountRepository.findByMemberId(Long): Optional<SavingsAccount>` (existing), `UserSessionBean.getLoggedInUser(): LoggedInUserDto` (existing).
- Produces: `LoanApplicationBean` getters `getPrincipalAmount()/setPrincipalAmount(BigDecimal)`, `getTermMonths()/setTermMonths(Integer)`, `isHasActiveLoan(): boolean`, `getMaxEligibleAmount(): BigDecimal`, `apply(): String`. Consumed by Task 7 (`apply.xhtml`).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.faces.context.FacesContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanApplicationBeanTest {

    @Mock private LoanService loanService;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserSessionBean userSessionBean;

    private LoanApplicationBean bean() {
        return new LoanApplicationBean(loanService, memberRepository, savingsAccountRepository, userSessionBean);
    }

    private void loggedInAs(long userAccountId) {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccountId);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);
    }

    @Test
    void initResolvesMemberAndComputesMaxEligibleAmount() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setBalance(new BigDecimal("50000.00"));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(account));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());

        LoanApplicationBean bean = bean();
        bean.init();

        assertEquals(new BigDecimal("150000.00"), bean.getMaxEligibleAmount());
        assertFalse(bean.isHasActiveLoan());
    }

    @Test
    void initDetectsExistingPendingLoan() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));

        Loan pendingLoan = new Loan();
        pendingLoan.setStatus(LoanStatus.PENDING);
        when(loanService.getLoansByMember(10L)).thenReturn(List.of(pendingLoan));

        LoanApplicationBean bean = bean();
        bean.init();

        assertTrue(bean.isHasActiveLoan());
    }

    @Test
    void applyRedirectsToMyLoansOnSuccess() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());

        LoanApplicationBean bean = bean();
        bean.init();
        bean.setPrincipalAmount(new BigDecimal("10000.00"));
        bean.setTermMonths(6);

        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = bean.apply();

            assertEquals("/loans/applications?faces-redirect=true", outcome);
        }
    }

    @Test
    void applyReturnsNullAndAddsErrorMessageWhenServiceRejects() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());
        when(loanService.applyLoan(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("Requested loan amount exceeds the maximum allowed limit"));

        LoanApplicationBean bean = bean();
        bean.init();
        bean.setPrincipalAmount(new BigDecimal("999999.00"));
        bean.setTermMonths(6);

        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = bean.apply();

            assertNull(outcome);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=LoanApplicationBeanTest test`
Expected: FAIL — `LoanApplicationBean` has none of these members yet.

- [ ] **Step 3: Rewrite `LoanApplicationBean`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/controller/LoanApplicationBean.java`:

```java
package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loanApplicationBean")
@RequestScope
public class LoanApplicationBean {

    private final LoanService loanService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;

    private Long memberId;
    private BigDecimal maxEligibleAmount = BigDecimal.ZERO;
    private boolean hasActiveLoan;

    private BigDecimal principalAmount;
    private Integer termMonths;

    public LoanApplicationBean(
            LoanService loanService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean
    ) {
        this.loanService = loanService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));
        memberId = member.getId();

        SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(memberId).orElse(null);
        BigDecimal balance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        maxEligibleAmount = balance.multiply(new BigDecimal("3"));

        List<Loan> memberLoans = loanService.getLoansByMember(memberId);
        hasActiveLoan = memberLoans.stream().anyMatch(loan ->
                loan.getStatus() == LoanStatus.PENDING
                        || loan.getStatus() == LoanStatus.ACTIVE
                        || loan.getStatus() == LoanStatus.OVERDUE);
    }

    public String apply() {
        try {
            LoanApplicationForm form = new LoanApplicationForm();
            form.setMemberId(memberId);
            form.setPrincipalAmount(principalAmount);
            form.setTermMonths(termMonths);

            loanService.applyLoan(form);

            FacesMessageUtil.addInfoMessage("Loan application submitted successfully");
            return "/loans/applications?faces-redirect=true";
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public BigDecimal getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public boolean isHasActiveLoan() {
        return hasActiveLoan;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=LoanApplicationBeanTest test`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/LoanApplicationBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/LoanApplicationBeanTest.java
git commit -m "Wire LoanApplicationBean to LoanService.applyLoan"
```

---

### Task 5: `MemberLoansBean` (My Loans controller)

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/controller/MemberLoansBean.java`
- Create: `src/test/java/org/joel/kimwanyisacco/controller/MemberLoansBeanTest.java`

**Interfaces:**
- Consumes: `LoanService.getLoansByMember(Long): List<Loan>`, `MemberRepository.findByUserAccountId(Long): Optional<Member>`, `UserSessionBean.getLoggedInUser()`.
- Produces: `MemberLoansBean.getLoans(): List<Loan>` (newest-first), `MemberLoansBean.badgeClass(Loan): String`. Consumed by Task 8 (`applications.xhtml`).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberLoansBeanTest {

    @Mock private LoanService loanService;
    @Mock private MemberRepository memberRepository;
    @Mock private UserSessionBean userSessionBean;

    private Loan loanWith(LoanStatus status, LocalDate applicationDate) {
        Loan loan = new Loan();
        loan.setStatus(status);
        loan.setPrincipal(new BigDecimal("10000.00"));
        loan.setApplicationDate(applicationDate);
        return loan;
    }

    @Test
    void initLoadsMemberLoansNewestFirst() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(1L);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        Loan older = loanWith(LoanStatus.FULLY_REPAID, LocalDate.of(2026, 1, 1));
        Loan newer = loanWith(LoanStatus.PENDING, LocalDate.of(2026, 3, 1));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of(older, newer));

        MemberLoansBean bean = new MemberLoansBean(loanService, memberRepository, userSessionBean);
        bean.init();

        assertEquals(2, bean.getLoans().size());
        assertEquals(LoanStatus.PENDING, bean.getLoans().get(0).getStatus());
        assertEquals(LoanStatus.FULLY_REPAID, bean.getLoans().get(1).getStatus());
    }

    @Test
    void badgeClassMapsStatusToBadgeColor() {
        MemberLoansBean bean = new MemberLoansBean(loanService, memberRepository, userSessionBean);

        assertEquals("badge-yellow", bean.badgeClass(loanWith(LoanStatus.PENDING, LocalDate.now())));
        assertEquals("badge-blue", bean.badgeClass(loanWith(LoanStatus.ACTIVE, LocalDate.now())));
        assertEquals("badge-green", bean.badgeClass(loanWith(LoanStatus.FULLY_REPAID, LocalDate.now())));
        assertEquals("badge-red", bean.badgeClass(loanWith(LoanStatus.REJECTED, LocalDate.now())));
        assertEquals("badge-red", bean.badgeClass(loanWith(LoanStatus.OVERDUE, LocalDate.now())));
        assertEquals("badge-gray", bean.badgeClass(loanWith(LoanStatus.APPROVED, LocalDate.now())));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=MemberLoansBeanTest test`
Expected: FAIL — `MemberLoansBean` doesn't exist yet.

- [ ] **Step 3: Write `MemberLoansBean`**

```java
package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberLoansBean")
@RequestScope
public class MemberLoansBean {

    private final LoanService loanService;
    private final MemberRepository memberRepository;
    private final UserSessionBean userSessionBean;

    private List<Loan> loans = List.of();

    public MemberLoansBean(LoanService loanService, MemberRepository memberRepository, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.memberRepository = memberRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            loans = loanService.getLoansByMember(member.getId()).stream()
                    .sorted(Comparator.comparing(Loan::getApplicationDate).reversed())
                    .toList();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load your loans: " + e.getMessage());
        }
    }

    public String badgeClass(Loan loan) {
        return switch (loan.getStatus()) {
            case PENDING -> "badge-yellow";
            case ACTIVE -> "badge-blue";
            case FULLY_REPAID -> "badge-green";
            case REJECTED, OVERDUE -> "badge-red";
            case APPROVED -> "badge-gray";
        };
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=MemberLoansBeanTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/MemberLoansBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/MemberLoansBeanTest.java
git commit -m "Add MemberLoansBean for the My Loans page"
```

---

### Task 6: Finish `SavingsHistoryBean`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/controller/SavingsHistoryBean.java`
- Create: `src/test/java/org/joel/kimwanyisacco/controller/SavingsHistoryBeanTest.java`

**Interfaces:**
- Consumes: `SavingsService.getAccountById(Long): SavingsAccountDto`, `SavingsService.getTransactionHistory(Long): List<SavingsTransactionDto>` (Task 3), `MemberRepository.findByUserAccountId`, `SavingsAccountRepository.findByMemberId`, `UserSessionBean.getLoggedInUser()`.
- Produces: `SavingsHistoryBean.getTransactions(): List<SavingsTransactionDto>`, `getAccount(): SavingsAccountDto`. Consumed by Task 9 (`history.xhtml`).

- [ ] **Step 1: Write the failing test**

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsHistoryBeanTest {

    @Mock private SavingsService savingsService;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserSessionBean userSessionBean;

    @Test
    void initLoadsAccountAndTransactionHistory() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(1L);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setId(20L);
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(account));

        SavingsAccountDto accountDto = new SavingsAccountDto();
        accountDto.setId(20L);
        accountDto.setBalance(new BigDecimal("30000.00"));
        when(savingsService.getAccountById(20L)).thenReturn(accountDto);

        SavingsTransactionDto txDto = new SavingsTransactionDto();
        txDto.setTransactionType("DEPOSIT");
        when(savingsService.getTransactionHistory(20L)).thenReturn(List.of(txDto));

        SavingsHistoryBean bean = new SavingsHistoryBean(savingsService, memberRepository, savingsAccountRepository, userSessionBean);
        bean.init();

        assertEquals(new BigDecimal("30000.00"), bean.getAccount().getBalance());
        assertEquals(1, bean.getTransactions().size());
        assertEquals("DEPOSIT", bean.getTransactions().get(0).getTransactionType());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=SavingsHistoryBeanTest test`
Expected: FAIL — no matching constructor / no `init()`/`getAccount()` yet.

- [ ] **Step 3: Update `SavingsHistoryBean`**

Replace the entire contents of `src/main/java/org/joel/kimwanyisacco/controller/SavingsHistoryBean.java`:

```java
package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("savingsHistoryBean")
@RequestScope
public class SavingsHistoryBean {

    private final SavingsService savingsService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;

    private SavingsAccountDto account;
    private List<SavingsTransactionDto> transactions = List.of();

    public SavingsHistoryBean(
            SavingsService savingsService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean
    ) {
        this.savingsService = savingsService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(member.getId())
                    .orElseThrow(() -> new IllegalStateException("No savings account found for member"));

            account = savingsService.getAccountById(savingsAccount.getId());
            transactions = savingsService.getTransactionHistory(savingsAccount.getId());
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load savings history: " + e.getMessage());
        }
    }

    public SavingsAccountDto getAccount() {
        return account;
    }

    public List<SavingsTransactionDto> getTransactions() {
        return transactions;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=SavingsHistoryBeanTest test`
Expected: PASS (1 test)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/joel/kimwanyisacco/controller/SavingsHistoryBean.java \
        src/test/java/org/joel/kimwanyisacco/controller/SavingsHistoryBeanTest.java
git commit -m "Load account and transaction history in SavingsHistoryBean"
```

---

### Task 7: Build `/loans/apply.xhtml`

**Files:**
- Modify: `src/main/webapp/loans/apply.xhtml`
- Modify: `src/main/webapp/resources/css/member.css`

**Interfaces:**
- Consumes: `loanApplicationBean` getters/setters and `apply()` from Task 4.

- [ ] **Step 1: Add form field CSS**

Append to the end of `src/main/webapp/resources/css/member.css`:

```css

/* Form fields (Apply for Loan, etc.) */
.mem-field { margin-bottom: 1.1rem; }
.mem-field label {
    display: block; font-size: 0.8rem; font-weight: 600;
    color: #374151; margin-bottom: 0.4rem;
}
.mem-input {
    width: 100%; padding: 0.65rem 0.9rem;
    border: 1px solid var(--mem-border); border-radius: 0.5rem;
    font-size: 0.88rem; font-family: inherit; color: var(--mem-text);
    outline: none; transition: border-color 0.15s;
}
.mem-input:focus { border-color: var(--mem-primary); }
.mem-error { color: #b91c1c; font-size: 0.75rem; margin-top: 0.3rem; display: block; }
.mem-helper-box {
    background: #fef2f2; border: 1px solid #fecaca; border-radius: 0.6rem;
    padding: 0.85rem 1rem; font-size: 0.8rem; color: #7f1d1d;
    margin-bottom: 1.1rem; line-height: 1.5;
}
```

- [ ] **Step 2: Write `apply.xhtml`**

Replace the entire contents of `src/main/webapp/loans/apply.xhtml`:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                template="/WEB-INF/templates/member-template.xhtml">

    <ui:define name="title">Apply for Loan | Kimwanyi SACCO</ui:define>
    <ui:define name="pageTitle">Apply for Loan</ui:define>

    <ui:define name="content">

        <h:panelGroup rendered="#{loanApplicationBean.hasActiveLoan}">
            <div class="mem-card">
                <div class="mem-card-header">
                    <span class="mem-card-title"><i class="fa-solid fa-circle-info" style="color:#dc2626; margin-right:6px;"></i> Loan In Progress</span>
                </div>
                <div class="mem-card-body">
                    <div class="mem-empty" style="padding: 2rem 1rem;">
                        <i class="fa-solid fa-hand-holding-dollar"></i>
                        <strong style="color:#374151;">You already have a loan in progress</strong>
                        <p>You can only apply for a new loan once your current one is fully repaid.</p>
                        <h:link outcome="/loans/applications" styleClass="mem-btn mem-btn-primary" style="margin-top:0.5rem;">
                            <i class="fa-solid fa-list-check"></i> View My Loans
                        </h:link>
                    </div>
                </div>
            </div>
        </h:panelGroup>

        <h:panelGroup rendered="#{not loanApplicationBean.hasActiveLoan}">
            <div class="mem-card">
                <div class="mem-card-header">
                    <span class="mem-card-title"><i class="fa-solid fa-file-signature" style="color:#dc2626; margin-right:6px;"></i> New Loan Application</span>
                </div>
                <div class="mem-card-body">

                    <div class="mem-helper-box">
                        Maximum eligible amount: <strong>
                            <h:outputText value="#{loanApplicationBean.maxEligibleAmount}">
                                <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                            </h:outputText>
                        </strong> (3x your savings balance).
                        Interest is a flat 10% of the principal, added to your total repayable amount.
                    </div>

                    <h:form id="loanForm">
                        <div class="mem-field">
                            <h:outputLabel for="principalAmount" value="Principal Amount (UGX)"/>
                            <h:inputText id="principalAmount"
                                         value="#{loanApplicationBean.principalAmount}"
                                         label="Principal Amount"
                                         required="true"
                                         requiredMessage="Principal amount is required"
                                         styleClass="mem-input">
                                <f:convertNumber type="number" maxFractionDigits="2"/>
                            </h:inputText>
                            <h:message for="principalAmount" styleClass="mem-error"/>
                        </div>

                        <div class="mem-field">
                            <h:outputLabel for="termMonths" value="Term (months)"/>
                            <h:inputText id="termMonths"
                                         value="#{loanApplicationBean.termMonths}"
                                         label="Term"
                                         required="true"
                                         requiredMessage="Loan term is required"
                                         styleClass="mem-input">
                                <f:convertNumber type="number" integerOnly="true"/>
                            </h:inputText>
                            <h:message for="termMonths" styleClass="mem-error"/>
                        </div>

                        <h:commandButton value="Submit Application" action="#{loanApplicationBean.apply}" styleClass="mem-btn mem-btn-primary"/>
                    </h:form>

                </div>
            </div>
        </h:panelGroup>

    </ui:define>
</ui:composition>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/loans/apply.xhtml src/main/webapp/resources/css/member.css
git commit -m "Build out the Apply for Loan page"
```

---

### Task 8: Build `/loans/applications.xhtml` (My Loans)

**Files:**
- Modify: `src/main/webapp/loans/applications.xhtml`

**Interfaces:**
- Consumes: `memberLoansBean.getLoans()` and `memberLoansBean.badgeClass(Loan)` from Task 5.

- [ ] **Step 1: Write `applications.xhtml`**

Replace the entire contents of `src/main/webapp/loans/applications.xhtml`:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                template="/WEB-INF/templates/member-template.xhtml">

    <ui:define name="title">My Loans | Kimwanyi SACCO</ui:define>
    <ui:define name="pageTitle">My Loans</ui:define>

    <ui:define name="content">

        <div class="mem-card">
            <div class="mem-card-header">
                <span class="mem-card-title"><i class="fa-solid fa-list-check" style="color:#dc2626; margin-right:6px;"></i> Loan Applications</span>
                <h:link outcome="/loans/apply" styleClass="mem-btn mem-btn-primary">
                    <i class="fa-solid fa-plus"></i> Apply for Loan
                </h:link>
            </div>
            <div class="mem-card-body">

                <h:panelGroup rendered="#{empty memberLoansBean.loans}">
                    <div class="mem-empty">
                        <i class="fa-solid fa-hand-holding-dollar"></i>
                        <strong style="color:#374151;">No Loan Applications Yet</strong>
                        <p>You haven't applied for a loan yet.</p>
                        <h:link outcome="/loans/apply" styleClass="mem-btn mem-btn-primary" style="margin-top:0.5rem;">
                            <i class="fa-solid fa-plus"></i> Apply for Loan
                        </h:link>
                    </div>
                </h:panelGroup>

                <h:panelGroup rendered="#{not empty memberLoansBean.loans}">
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
                                </tr>
                            </ui:repeat>
                        </tbody>
                    </table>
                </h:panelGroup>

            </div>
        </div>

    </ui:define>
</ui:composition>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/loans/applications.xhtml
git commit -m "Build out the My Loans page"
```

---

### Task 9: Build `/savings/history.xhtml`

**Files:**
- Modify: `src/main/webapp/savings/history.xhtml`

**Interfaces:**
- Consumes: `savingsHistoryBean.getAccount()`, `savingsHistoryBean.getTransactions()` from Task 6.

- [ ] **Step 1: Write `history.xhtml`**

Replace the entire contents of `src/main/webapp/savings/history.xhtml`:

```xml
<ui:composition xmlns="http://www.w3.org/1999/xhtml"
                xmlns:ui="jakarta.faces.facelets"
                xmlns:h="jakarta.faces.html"
                xmlns:f="jakarta.faces.core"
                template="/WEB-INF/templates/member-template.xhtml">

    <ui:define name="title">Savings History | Kimwanyi SACCO</ui:define>
    <ui:define name="pageTitle">Savings History</ui:define>

    <ui:define name="content">

        <div class="mem-card">
            <div class="mem-card-header">
                <span class="mem-card-title"><i class="fa-solid fa-clock-rotate-left" style="color:#7c3aed; margin-right:6px;"></i> Transaction History</span>
                <span style="font-size:0.8rem; color:#9ca3af;">
                    Account <strong>#{savingsHistoryBean.account.accountNumber}</strong> &#8226; Balance:
                    <h:outputText value="#{savingsHistoryBean.account.balance}">
                        <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                    </h:outputText>
                </span>
            </div>
            <div class="mem-card-body">

                <h:panelGroup rendered="#{empty savingsHistoryBean.transactions}">
                    <div class="mem-empty">
                        <i class="fa-solid fa-receipt"></i>
                        <strong style="color:#374151;">No Transactions Yet</strong>
                        <p>Your savings transactions will appear here.</p>
                    </div>
                </h:panelGroup>

                <h:panelGroup rendered="#{not empty savingsHistoryBean.transactions}">
                    <table class="mem-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Type</th>
                                <th>Amount</th>
                                <th>Balance After</th>
                            </tr>
                        </thead>
                        <tbody>
                            <ui:repeat value="#{savingsHistoryBean.transactions}" var="tx">
                                <tr>
                                    <td>
                                        <h:outputText value="#{tx.createdAt}">
                                            <f:convertDateTime type="localDateTime" pattern="dd MMM yyyy HH:mm"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <span class="badge #{tx.transactionType == 'DEPOSIT' or tx.transactionType == 'TRANSFER_IN' or tx.transactionType == 'INTEREST' ? 'badge-green' : 'badge-red'}">
                                            #{tx.transactionType}
                                        </span>
                                    </td>
                                    <td>
                                        <h:outputText value="#{tx.amount}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                    <td>
                                        <h:outputText value="#{tx.balanceAfter}">
                                            <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
                                        </h:outputText>
                                    </td>
                                </tr>
                            </ui:repeat>
                        </tbody>
                    </table>
                </h:panelGroup>

            </div>
        </div>

    </ui:define>
</ui:composition>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/savings/history.xhtml
git commit -m "Build out the Savings History page"
```

---

### Task 10: Manual end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `mvn -q test`
Expected: PASS, all tests green.

- [ ] **Step 2: Rebuild and restart the app** (IntelliJ Run button, per user preference established earlier in this project)

- [ ] **Step 3: Register a brand-new member via the UI** (agent-browser or manual), then confirm in the DB/logs that a `SavingsAccount` row was created for them (e.g. via the admin dashboard's total savings balance, or a quick `SavingsAccountRepository` check).

- [ ] **Step 4: Log in as that member and visit `/loans/apply`**
   - Confirm the form renders (not the "loan in progress" branch, since balance is 0 and there are no loans).
   - Submit a small amount within the 3x-of-zero cap (i.e. try a positive amount — since balance is 0, max eligible is 0, so any positive amount should be rejected with the `LoanNotEligibleException` message surfaced via `FacesMessageUtil`). This confirms error-path wiring.

- [ ] **Step 5: Visit `/loans/applications`**
   - Confirm the empty state renders correctly with a working "Apply for Loan" link.

- [ ] **Step 6: Visit `/savings/history`**
   - Confirm the empty state renders correctly with the account number and UGX 0 balance shown in the header.

- [ ] **Step 7: (Optional, if time allows) Manually seed a savings deposit via SQL/test data, then re-check `/loans/apply` (now shows a nonzero max eligible amount and lets you actually submit) and `/savings/history` (shows the transaction row) to confirm the non-empty-state rendering.**

No commit for this task — it's verification of Tasks 1–9.
