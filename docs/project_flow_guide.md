# Kimwanyi SACCO Management System: Architecture and Flow Guide

This document is designed to help developers understand the architecture, database models, system flows, and individual functions of the **Kimwanyi SACCO Management System**. 

---

## 1. High-Level Architectural Layers

The application is structured as a classic **3-Tier Layered Web Application** integrated with Spring for dependency injection, transactions, and repository management, and Jakarta Faces (JSF) + PrimeFaces for the web UI.

```
+-------------------------------------------------------------+
| Presentation Layer (.xhtml Pages - PrimeFaces / MyFaces)    |
+-------------------------------------------------------------+
                              | (Bind inputs / Invoke actions)
                              v
+-------------------------------------------------------------+
| Backing Beans (JSF Controllers - Spring @Component)        |
+-------------------------------------------------------------+
                              | (Call service methods)
                              v
+-------------------------------------------------------------+
| Business Logic Layer (Spring @Services & @Components)       |
+-------------------------------------------------------------+
                              | (CRUD & Custom Queries)
                              v
+-------------------------------------------------------------+
| Data Access Layer (Spring Data JPA Repositories)             |
+-------------------------------------------------------------+
                              | (SQL queries)
                              v
+-------------------------------------------------------------+
| Database Layer (MySQL)                                      |
+-------------------------------------------------------------+
```

1. **Presentation Layer (`src/main/webapp/` & `controller/`)**:
   - **XHTML Pages (`.xhtml`)**: Built with Jakarta Faces (JSF) and PrimeFaces components. These files define the structural layout, user inputs, and visual elements.
   - **Backing Beans (`*Bean.java`)**: Bind UI inputs directly to Java variables (DTOs) and handle user action events (e.g. clicking a button triggers a bean method). They are Spring components annotated with scopes like `@RequestScope`, `@ViewScope`, or `@SessionScope`.
2. **Business Logic Layer (`service/` & `policy/`)**:
   - **Services (`*Service.java` / `*ServiceImpl.java`)**: Define application operations. They handle transactions using Spring's `@Transactional`, orchestrate repository calls, write audit logs, and trigger user notifications.
   - **Policies (`policy/`)**: Independent rule components that validate business constraints (e.g. checking if a member has enough savings to apply for a loan, or checking if a withdrawal preserves the minimum required balance).
3. **Data Access Layer (`repository/`)**:
   - **Repositories (`*Repository.java`)**: Interface declarations extending Spring Data JPA's `JpaRepository`. Spring automatically implements these at runtime, providing CRUD operations and custom query execution.
4. **Domain Model (`model/`)**:
   - **Entities**: Standard JPA annotations (`@Entity`, `@Table`, `@Id`, etc.) mapped to MySQL database tables.

---

## 2. Core Entities and Database Schema

Here is a map of the core entities in the `org.joel.kimwanyisacco.model` package:

| Entity Class | DB Table | Purpose | Key Relationships |
| :--- | :--- | :--- | :--- |
| **`UserAccount`** | `user_accounts` | Holds login credentials, encryption hashes, email, and user role (`ADMIN` or `MEMBER`). | None |
| **`Member`** | `members` | Holds SACCO member profiles, national ID, membership numbers, and member status (`ACTIVE`, `INACTIVE`, etc.). | `OneToOne` with `UserAccount` |
| **`SavingsAccount`** | `savings_accounts` | The monetary account associated with a Member. Stores the authoritative current balance. | `OneToOne` with `Member` |
| **`SavingsTransaction`** | `savings_transactions` | A ledger recording all deposits, withdrawals, transfers, and interest postings. | `ManyToOne` with `SavingsAccount` |
| **`Loan`** | `loans` | Tracks loan applications, approval status (`PENDING`, `ACTIVE`, `REJECTED`, `FULLY_REPAID`, `OVERDUE`), principal, interest, and due dates. | `ManyToOne` with `Member`, `ManyToOne` with `UserAccount` (deciding admin) |
| **`LoanRepayment`** | `loan_repayments` | Logs repayments made against an active or overdue loan. | `ManyToOne` with `Loan` |
| **`AuditLog`** | `audit_logs` | Logs administrative and financial actions (e.g., login success, interest application, loan decisions). | `ManyToOne` with `UserAccount` |
| **`Notification`** | `notifications` | System alerts displayed via an in-app bell (e.g. "Your loan was approved", "Deposit received"). | `ManyToOne` with `UserAccount` |

---

## 3. Step-by-Step System Flows

### A. Member Registration Flow

1. **Guest Member** fills the registration form in `login.xhtml` or registration page and clicks "Register".
2. Input fields bind to fields in `MemberRegistrationBean`.
3. `MemberRegistrationBean.register()` calls `MemberService.registerMember(form)`.
4. `MemberServiceImpl` coordinates the registration process:
   - Validates that the username, email, and National ID are not already taken by querying `UserAccountRepository` and `MemberRepository`.
   - Hashes the password using Spring Security's `PasswordEncoder` (BCrypt).
   - Saves a new `UserAccount` with the role `MEMBER`.
   - Generates a unique membership number.
   - Saves the new `Member` profile linked to the `UserAccount`.
   - **Important**: Automatically instantiates and saves a new `SavingsAccount` for this member with a starting balance of zero.
   - Logs an audit log record and triggers a registration notification.
5. Control returns to the registration bean, which redirects the member to the login page.

---

### B. User Login & Session Flow

1. User enters username and password in `login.xhtml`.
2. Action triggers `LoginBean.login()`, passing input data via a `LoginForm` DTO.
3. `LoginBean` invokes `AuthenticationService.authenticate(loginForm)`.
4. `AuthenticationServiceImpl` performs checks:
   - Fetches the `UserAccount` by username.
   - Verifies if the account is `enabled` (returns an error if disabled).
   - Uses `passwordEncoder.matches(...)` to compare the raw input password against the BCrypt password hash.
   - Writes an `AuditLog` of either `LOGIN_SUCCESS` or `LOGIN_FAILED`.
   - Returns a `LoggedInUserDto` containing user details (ID, username, roles).
5. `LoginBean` populates the `@SessionScope` bean `UserSessionBean` with the logged-in user's details to maintain the login state throughout the HTTP session.
6. The user is redirected to:
   - `/admin/dashboard.xhtml` if their role is `ADMIN`.
   - `/members/dashboard.xhtml` if their role is `MEMBER`.

---

### C. Internal Transfer Flow (Account-to-Account)

Members can transfer money to other members within the SACCO. The flow enforces crucial checks transactionally:

1. Member inputs recipient's account number and transfer amount in the UI.
2. `InternalTransferBean.transfer()` calls `SavingsService.transfer(form)`.
3. `SavingsServiceImpl` performs these steps inside a transactional block (`@Transactional`):
   - Fetches both sender and recipient savings accounts.
   - Validates both members are `ACTIVE`.
   - Checks if sender has enough balance to transfer the amount **while keeping at least the minimum balance of UGX 20,000** in their account.
   - Debits the sender's account balance and writes a `TRANSFER_OUT` transaction.
   - Credits the recipient's account balance and writes a `TRANSFER_IN` transaction.
   - Records an audit log for the action.

---

### D. Loan Application, Approval, and Repayment Flow

The Loan lifecycle is heavily governed by cooperative financial policies:

1. **Application**:
   - The member submits a loan request (`LoanApplicationBean` -> `LoanService.applyLoan`).
   - `LoanEligibilityPolicy` checks:
     - Is the member `ACTIVE`?
     - Does the member already have a `PENDING`, `ACTIVE`, or `OVERDUE` loan? (Only 1 active loan is allowed).
     - Is the requested principal amount $\le 3 \times$ the member's current savings balance?
   - Interest is calculated at a flat **10%** (`LoanInterestCalculator`).
   - The loan is saved as `PENDING`.
2. **Approval / Rejection (Admin Decision)**:
   - An administrator views pending applications at `/admin/loans.xhtml` (`LoanApprovalBean`).
   - The admin approves or rejects the application (`LoanService.decideLoan`).
   - **If Approved**: Status changes to `ACTIVE` and a **12-month due date** is set. An in-app notification is sent to the member.
   - **If Rejected**: Status changes to `REJECTED`, persisting the admin's remarks.
3. **Repayment**:
   - The member makes payments against their active/overdue loans.
   - The application checks:
     - Is the payment positive?
     - Does the payment exceed the remaining outstanding balance?
   - The payment updates the loan's outstanding balance. If the balance reaches `0`, the status is updated to `FULLY_REPAID`.
   - A `LoanRepayment` record is written to the ledger.

---

## 4. Key Services and Their Functions

Here is the exact developer reference explaining what each service implementation does:

### 1. `SavingsServiceImpl`
* **`deposit(DepositForm form)`**: Increases a savings account balance and creates a `DEPOSIT` transaction. Sends a deposit notification to the member.
* **`withdraw(WithdrawalForm form)`**: Decreases account balance. Invokes `WithdrawalPolicy` to ensure the withdrawal does not violate the minimum balance limit of UGX 20,000. Creates a `WITHDRAW` transaction.
* **`transfer(InternalTransferForm form)`**: Debits the sender, credits the recipient, creates double-entry ledger transactions (`TRANSFER_OUT` and `TRANSFER_IN`), and posts a security audit log.
* **`applyMonthlyInterest(YearMonth month)`**: Credits **5% annual interest** compounded monthly (`balance * 0.05 / 12`) to all savings accounts. It guarantees **idempotency** by checking if a transaction with the reference `INT-[month]-[accountNumber]` already exists.
* **`postPreviousMonthInterest()`**: Automatically triggered by a cron scheduler on the 1st of every month to calculate and apply interest.

### 2. `LoanServiceImpl`
* **`applyLoan(LoanApplicationForm form)`**: Registers a loan application. Asserts member eligibility, calculates interest, generates total repayable amount, and notifies administrators of a new pending application.
* **`decideLoan(LoanDecisionForm form, Long adminUserId)`**: Validates that the deciding user is an enabled administrator, enforces that the loan is in `PENDING` state, transitions the loan status to `ACTIVE` or `REJECTED`, and stores the due date.
* **`repayLoan(LoanRepaymentForm form)`**: Records a cash payment, checks for overpayment, decrements outstanding balance, and updates status to `FULLY_REPAID` when fully paid.
* **`markOverdueLoans()`**: A scheduled nightly background job (`@Scheduled(cron = "0 15 0 * * *")`) that transitions active loans whose due dates are in the past to the `OVERDUE` state.

### 3. `StatementServiceImpl`
* **`generateForMember(Long userAccountId, LocalDate fromDate, LocalDate toDate)`**: Collects all transactions (deposits, withdrawals, transfers, interest credits) in the specified date range. Computes the opening balance (the balance after the last transaction prior to the start date) and closing balance, returning a structured list of entries suitable for a printable table.

### 4. `UserManagementServiceImpl`
* **`setEnabled(Long userId, boolean enabled, UserAccount adminAccount)`**: Enables or disables a user login. Automatically synchronizes the corresponding member's status (deactivating user logins blocks them from logging in immediately).
* **`approveMember(Long userId, UserAccount adminAccount)`**: Approves onboarding members, transitioning their status from pending states to active states.

---

## 5. Developer Cheat Sheet: How to Navigate Code

When you need to make changes or debug, trace files using these standard paths:

* **To modify a web page layout, styles, or inputs**:
  - Look in `src/main/webapp/` (e.g., `src/main/webapp/login.xhtml`, `src/main/webapp/admin/dashboard.xhtml`).
* **To trace what happens when a button is clicked**:
  1. Open the page's `.xhtml` file.
  2. Find the command button (e.g., `<p:commandButton action="#{loginBean.login}" ...>`).
  3. Locate the backing bean in `src/main/java/org/joel/kimwanyisacco/controller/` (e.g., `LoginBean.java` -> `login()`).
  4. Trace the service method called inside the bean (e.g., `AuthenticationService.authenticate()`).
* **To adjust database properties or Hibernate logs**:
  - Open `src/main/resources/application.properties`.
* **To run tests**:
  - Run `mvn clean verify` in the project root.
