# Kimwanyi SACCO Management System: UI & Converter Architecture for Savings (Deposit & Withdraw)

This guide walks you through how the User Interface (UI), JSF Backing Beans, Converters, and Business Services cooperate to perform **Deposit** and **Withdrawal** operations.

---

## 1. What are Converters doing in this project?

In JSF web development, there are two distinct types of converters. This project utilizes both patterns:

### A. Data Mappers (Entity-to-DTO Converters)
Found in: `org.joel.kimwanyisacco.common.util.converter.*` (e.g., `SavingsAccountConverter`, `MemberConverter`).
* **Function**: They are Spring-managed `@Component` utilities that map rich database **Entities** (which are managed by JPA/Hibernate and have database relationships) into lightweight, serializable **DTOs** (Data Transfer Objects).
* **Why we use them**: 
  1. **Prevents `LazyInitializationException`**: JSF pages rendering outside the active Spring `@Transactional` database connection session can crash if they try to read related lazy entities. Converters eagerly fetch and extract this data into safe, flat DTO fields first.
  2. **Security**: They ensure sensitive details (like BCrypt password hashes from `UserAccount`) never leak up to the web front-end.
  3. **Decoupling**: The presentation layer is decoupled from database schema updates.

**Example from `SavingsAccountConverter.java`:**
```java
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

### B. JSF Component Converters (UI Converters)
Used directly in XHTML files (e.g., `<f:convertNumber>` or `<f:convertDateTime>`).
* **Function**: Format values for display (e.g., prefixing `UGX` and removing decimal points for currency) and convert input strings from input boxes back into Java types.

**Example from `admin/savings.xhtml`:**
```xml
<h:outputText value="#{account.balance}">
    <f:convertNumber type="currency" currencySymbol="UGX " maxFractionDigits="0"/>
</h:outputText>
```

---

## 2. Cash Deposit Flow: Code & UI walkthrough

Deposits are initiated by an administrator on behalf of a member when they receive physical cash at the office.

### A. The Front-End (XHTML)
In `/admin/savings.xhtml`, a data table lists all accounts. Each row contains a command link:
```xml
<p:commandLink action="#{adminSavingsBean.prepareTransaction(account, 'DEPOSIT')}"
               oncomplete="PF('txDialog').show();" update=":txDialogWidget">
    <i class="fa-solid fa-arrow-down"></i> Deposit
</p:commandLink>
```
* **What happens**: Clicking this button invokes the bean's `prepareTransaction` method, setting the type to `'DEPOSIT'`, and opens the PrimeFaces dialog (`PF('txDialog').show()`).

### B. The Backing Bean (Controller)
In `AdminSavingsBean.java`:
```java
public void prepareTransaction(SavingsAccount account, String type) {
    this.selectedAccount = account;
    this.transactionType = type;
    this.amount = null;
    this.description = null;
}

public void processTransaction() {
    // ... validation checks ...
    if ("DEPOSIT".equals(transactionType)) {
        DepositForm form = new DepositForm();
        form.setSavingsAccountId(selectedAccount.getId());
        form.setAmount(amount);
        form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Deposit");
        
        savingsService.deposit(form); // Calls Business Logic
        FacesMessageUtil.addInfoMessage("Successfully deposited UGX " + amount);
    }
    // ...
}
```

### C. The Business Logic & Database Update
In `SavingsServiceImpl.java` (running inside a `@Transactional` context):
```java
@Override
@Transactional
public SavingsTransactionDto deposit(DepositForm form) {
    // 1. Validate deposit amount is positive
    if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Deposit amount must be greater than zero");
    }

    // 2. Fetch target savings account from DB
    SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

    BigDecimal balanceBefore = account.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

    // 3. Update the account's authoritative balance in the DB
    account.setBalance(balanceAfter);
    savingsAccountRepository.save(account);

    // 4. Create and save a transaction ledger entry for the statement
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

    // 5. Audit the transaction and send in-app notification
    auditLogService.record(account.getMember().getUserAccount(), AuditAction.DEPOSIT_PROCESSED, ...);
    notificationService.notify(account.getMember().getUserAccount(), NotificationType.DEPOSIT, ...);

    // 6. Convert the saved database entity to a DTO and return it
    return savingsTransactionConverter.toDto(tx);
}
```

---

## 3. Cash Withdrawal Flow: Code & UI walkthrough

Withdrawals are restricted by a **minimum balance rule** (a member must always leave at least UGX 20,000 in their savings account).

### A. The Front-End (XHTML)
In `/admin/savings.xhtml`, each row contains a withdrawal link:
```xml
<p:commandLink action="#{adminSavingsBean.prepareTransaction(account, 'WITHDRAW')}"
               oncomplete="PF('txDialog').show();" update=":txDialogWidget">
    <i class="fa-solid fa-arrow-up"></i> Withdraw
</p:commandLink>
```
* **What happens**: Clicking this invokes `prepareTransaction(account, 'WITHDRAW')` and displays the modal input box.

### B. The Backing Bean (Controller)
In `AdminSavingsBean.java`:
```java
public void processTransaction() {
    // ... validation checks ...
    if ("WITHDRAW".equals(transactionType)) {
        WithdrawalForm form = new WithdrawalForm();
        form.setSavingsAccountId(selectedAccount.getId());
        form.setAmount(amount);
        form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Withdrawal");
        
        savingsService.withdraw(form); // Calls Business Logic
        FacesMessageUtil.addInfoMessage("Successfully withdrawn UGX " + amount);
    }
    // ...
}
```

### C. The Business Logic & Withdrawal Policy Checking
In `SavingsServiceImpl.java`:
```java
@Override
@Transactional
public SavingsTransactionDto withdraw(WithdrawalForm form) {
    SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

    BigDecimal balanceBefore = account.getBalance();

    // 1. Delegate rules checking to WithdrawalPolicy
    if (!withdrawalPolicy.isWithdrawalAllowed(balanceBefore, form.getAmount())) {
        throw new IllegalArgumentException(
                "Withdrawal not allowed: amount must be positive and leave at least UGX 20,000 in the account.");
    }

    BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

    // 2. Update balance in database
    account.setBalance(balanceAfter);
    savingsAccountRepository.save(account);

    // 3. Save a transaction ledger entry for statements
    SavingsTransaction tx = new SavingsTransaction();
    tx.setSavingsAccount(account);
    tx.setReference("WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    tx.setType(TransactionType.WITHDRAW);
    tx.setAmount(form.getAmount());
    tx.setBalanceBefore(balanceBefore);
    tx.setBalanceAfter(balanceAfter);
    tx.setDescription(form.getDescription());
    tx.setCreatedAt(LocalDateTime.now());
    savingsTransactionRepository.save(tx);

    // 4. Audit logging and notifications
    auditLogService.record(account.getMember().getUserAccount(), AuditAction.WITHDRAWAL_PROCESSED, ...);
    notificationService.notify(account.getMember().getUserAccount(), NotificationType.WITHDRAWAL, ...);

    return savingsTransactionConverter.toDto(tx);
}
```

---

## 4. Withdrawal Policy Rule Checks

The validation is modularized into `WithdrawalPolicy.java` to make it easily testable and maintainable:
```java
@Component
public class WithdrawalPolicy {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    public boolean isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount) {
        // Enforce positive withdrawal amount
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        // Enforce that the account maintains at least UGX 20,000.00 after the transaction
        BigDecimal balanceAfter = currentBalance.subtract(requestedAmount);
        return balanceAfter.compareTo(MINIMUM_BALANCE) >= 0;
    }
}
```
If this check fails, the policy returns `false`, causing the service layer to throw an `IllegalArgumentException` which is caught in the Controller and displayed to the Admin as an error notification.
