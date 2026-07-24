# Kimwanyi SACCO Management System: Complete Codebase Walkthrough

This document provides a class-by-class and package-by-package walkthrough of the entire **Kimwanyi SACCO Management System** codebase. It acts as an developer's map to understand where files are located, what each class does, and how they relate.

---

## 1. Directory Structure Overview

The project is structured under the root package `org.joel.kimwanyisacco`:
* **`common`**: Shared utility code (membership/account number generators, message utilities, mapping converters, and custom exception classes).
* **`config`**: Security configurations, database persistence settings, and application bootstrapper (AdminAccountSeeder).
* **`controller`**: JSF Backing Beans. These receive visual interactions from `.xhtml` files and pass them to the backend services.
* **`dto`**: Data Transfer Objects. Lightweight Java objects used for form submissions, summaries, and API payloads.
* **`model`**: JPA Database Entities and their associated state `enums`.
* **`policy`**: Domain policies (interest calculators, loan eligibility verification, withdrawal limits).
* **`repository`**: Spring Data JPA Interfaces mapping to MySQL tables.
* **`service`**: Core business services defining transactional application operations.

---

## 2. Package-by-Package File Index

### 📂 `model` & `enums` (The Domain Model)
These represent the persistent database entities mapped via Hibernate.

#### JPA Entities (`org.joel.kimwanyisacco.model.*`)
1. **`UserAccount`**: Stores basic credentials. Represents anyone who logs in. Fields: `username`, `passwordHash`, `firstName`, `lastName`, `email`, `role` (ADMIN or MEMBER), `enabled`.
2. **`Member`**: Linked 1-to-1 with `UserAccount`. Contains cooperative-specific profile fields: `membershipNumber` (e.g. `KIM-001`), `nationalId`, `phoneNumber`, `status` (PENDING, ACTIVE, INACTIVE, SUSPENDED), `joinedAt`.
3. **`SavingsAccount`**: A member's savings account. Automatically generated when a member is registered. Stores the authoritative `balance` and `accountNumber`.
4. **`SavingsTransaction`**: Records individual financial actions on a savings account (deposits, withdrawals, transfers, interest credits). Stores `balanceBefore` and `balanceAfter` to guarantee auditability.
5. **`Loan`**: Represents loan requests. Fields: `principal`, `interestRate` (fixed at 10%), `interestAmount`, `totalRepayable`, `amountRepaid`, `outstandingBalance`, `status` (PENDING, APPROVED, ACTIVE, FULLY_REPAID, REJECTED, OVERDUE), `dueDate`.
6. **`LoanRepayment`**: Stores individual payments against active/overdue loans. Maps to `Loan`.
7. **`AuditLog`**: Stores security and financial event history (logins, transfers, approval decisions) for admin reviews.
8. **`Notification`**: Stores system-generated messages displayed in the member's notification panel.
9. **`EmailLog`**: Logs outgoing system emails (emails are simulated/scaffolded).
10. **`Payment`**: Represents deposits/payments made via MTN Mobile Money, Airtel Money, or card.
11. **`InternalTransfer`**: Logs transfer history metadata between two member accounts.

#### Enums (`org.joel.kimwanyisacco.model.enums.*`)
* **`Role`**: `ADMIN`, `MEMBER`.
* **`MemberStatus`**: `PENDING`, `ACTIVE`, `INACTIVE`, `SUSPENDED`.
* **`LoanStatus`**: `PENDING`, `APPROVED`, `ACTIVE`, `FULLY_REPAID`, `REJECTED`, `OVERDUE`.
* **`TransactionType`**: `DEPOSIT`, `WITHDRAW`, `INTEREST`, `TRANSFER_IN`, `TRANSFER_OUT`.
* **`AuditAction`**: Actions like `LOGIN_SUCCESS`, `DEPOSIT_PROCESSED`, `LOAN_APPROVED`, etc.
* **`NotificationType`**: `DEPOSIT`, `WITHDRAWAL`, `LOAN_APPLICATION`, `LOAN_APPROVED`, `LOAN_REPAYMENT`, etc.
* **`PaymentMethod`**, `PaymentStatus`, `MobileMoneyProvider`, `TransferStatus`.

---

### 📂 `repository` (Database Access)
Spring Data JPA interfaces extending `JpaRepository<Entity, Long>`. Spring auto-implements these at startup.

1. **`UserAccountRepository`**: Find accounts by username, email, or role.
2. **`MemberRepository`**: Retrieve members by National ID, account ID, or search strings.
3. **`SavingsAccountRepository`**: Locate savings accounts by account numbers or Member ID.
4. **`SavingsTransactionRepository`**: Fetch ledger statements for a specific account.
5. **`LoanRepository`**: Search loans by status, member, or overdue due-dates.
6. **`LoanRepaymentRepository`**: Retrieve payment history for a specific loan.
7. **`AuditLogRepository`**: Query logs with support for admin keyword filtering.
8. **`NotificationRepository`**: Load unread/read notifications for a user.
9. **`EmailLogRepository`**: Core log persistence for emails.

---

### 📂 `policy` (Cooperative Business Rules)
These classes isolate mathematical and validation requirements from general database or page-routing logic.

1. **`WithdrawalPolicy`**: Ensures withdrawals are greater than zero and maintain a minimum account balance of **UGX 20,000**.
2. **`LoanEligibilityPolicy`**: Verifies that a member is ACTIVE, does not have other active loans, and is applying for $\le 3 \times$ their current savings balance.
3. **`LoanInterestCalculator`**: Flat **10%** loan interest rate calculator.
4. **`SavingsInterestCalculator`**: Computes monthly savings interest (annual **5% / 12 months** compounded).

---

### 📂 `service` (Core Logic Implementation)
Handles transactional logic (`@Transactional`), exceptions, and connects controllers to database repositories.

1. **`AuthenticationServiceImpl`**: Checks user credentials, verifies if accounts are disabled, evaluates passwords via BCrypt, and stores successful/failed logins in the audit log.
2. **`MemberServiceImpl`**: Registers members, hashes passwords, provisions new savings accounts automatically, updates profile details, and searches members.
3. **`SavingsServiceImpl`**: Processes deposits, withdrawals, and internal transfers. Implements the monthly savings interest scheduler (`postPreviousMonthInterest`).
4. **`LoanServiceImpl`**: Validates eligibility, saves pending applications, handles admin approval/rejection decisions, processes repayments, and hosts the cron job that marks active loans as `OVERDUE` after their due date.
5. **`StatementServiceImpl`**: Consolidates transaction history into print-ready PDF/XHTML statements over custom date ranges, computing opening and closing balances.
6. **`UserManagementServiceImpl`**: Admin service for enabling/disabling login accounts, resetting passwords, and approving onboarding applications.
7. **`AuditLogServiceImpl`**: Saves system activity logs.
8. **`NotificationServiceImpl`**: Publishes messages to user accounts and broadcasts application alerts to administrators.
9. **`EmailServiceImpl`**: Scaffolded interface for email dispatches.

---

### 📂 `controller` (JSF Backing Beans)
Backing beans bind user inputs (using DTOs) to JSF templates (`.xhtml`), execute UI requests, and handle redirects.

1. **`LoginBean`**: Logs users in, updates `UserSessionBean`, logs out, and redirects roles (Admins go to `/admin/*`, Members to `/members/*`).
2. **`UserSessionBean`**: A `@SessionScope` bean that preserves the current logged-in user's profile and credentials throughout their browser session.
3. **`MemberRegistrationBean`**: Captures member signup forms from the web front-end.
4. **`MemberProfileBean`**: Loads and updates member profile details.
5. **`MemberDashboardBean`**: Loads dashboards for logged-in members, listing their savings balance, loan balances, recent notifications, and transaction history.
6. **`AdminSavingsBean`**: Used by admins to record physical cash deposits and process cash withdrawals on behalf of members.
7. **`DepositBean`**: Member-facing deposit simulator.
8. **`WithdrawalBean`**: Placeholder request bean for member withdrawals.
9. **`InternalTransferBean`**: Validates internal peer-to-peer transfers from one member's account to another.
10. **`LoanApplicationBean`**: Submits loan requests.
11. **`LoanApprovalBean`**: Used by admins to review, approve, and reject loans.
12. **`LoanRepaymentBean`**: Captures loan repayment inputs.
13. **`StatementBean`**: Generates account statements over custom date ranges.
14. **`UserManagementBean`**: Allows admins to list users, change password hashes, and enable/disable accounts.
15. **`AuditLogBean`**: Displays system activity logs to administrators.
16. **`NotificationBean`**: Fetches in-app alerts and handles "Mark as Read" click actions.

---

### 📂 `common` (Utilities and Exception Mapping)
Shared helper code to avoid repeating logic.

#### Generators (`org.joel.kimwanyisacco.common.util.*`)
* **`MembershipNumberGenerator`**: Auto-generates unique sequential membership identifiers (e.g. `KIM-001`, `KIM-002`).
* **`SavingsAccountNumberGenerator`**: Auto-generates unique savings account numbers (e.g. `SAV-873612`).
* **`FacesMessageUtil`**: Simplifies pushing success/error notifications to PrimeFaces `<p:growl>` components.
* **`MoneyUtil`**: Standardizes currency formatting.

#### DTO Converters (`org.joel.kimwanyisacco.common.util.converter.*`)
* **`MemberConverter`**, `SavingsAccountConverter`, `SavingsTransactionConverter`, `AuditLogConverter`, `NotificationConverter`, `UserAccountConverter`: Translate rich database entities into lightweight Data Transfer Objects to avoid lazy-loading crashes.

#### Custom Exceptions (`org.joel.kimwanyisacco.common.exception.*`)
* **`AuthenticationException`**: Invalid credentials or disabled logins.
* **`BusinessException`**: Base class for cooperative rule violations.
* **`InsufficientBalanceException`**: Withdrawal violations.
* **`LoanNotEligibleException`**: Fails loan policies.
* **`ResourceNotFoundException`**: Requested entity does not exist in DB.

---

### 📂 `config` (Configurations)
Configures Spring, Servlet parameters, Security filters, and DB connections.

1. **`WebAppInitializer`**: Bootstraps the application context and binds JSF's `FacesServlet` to serve `.xhtml` requests.
2. **`AppConfig`**: Enables Spring scheduling (`@EnableScheduling`) for background processes (e.g. interest calculation, overdue loan updates) and registers BCrypt `PasswordEncoder`.
3. **`PersistenceConfig`**: Configures Hibernate ORM, transaction managers, and the MySQL DataSource (reading credentials securely from environment variables).
4. **`SecurityFilter`**: A standard HTTP filter (`jakarta.servlet.Filter`) that inspects the browser session:
   - Blocks unauthenticated users from reaching `/members/*` and `/admin/*`.
   - Restricts `/admin/*` directories exclusively to users with the `ADMIN` role.
5. **`AdminAccountSeeder`**: An event listener running at startup that seeds an initial administrative user account using details supplied via environment variables (`INITIAL_ADMIN_USERNAME`, etc.).

---

## 3. How to Trace Request Lifecycles

Whenever you read or write code, trace execution using this **downward and upward** loop:

```
[Browser Action (Click/Submit)]
            │
            ▼
[JSF Page (.xhtml)] ── (Binds values to) ──► [Backing Bean (*Bean.java)]
                                                    │
                                                    ▼
                                            [Service (*ServiceImpl.java)]
                                                    │
                                                    ▼
                                            [Policy Checks (*Policy.java)]
                                                    │
                                                    ▼
[JPA Entity] ◄────── (Updates & Saves) ───── [Repository (*Repository.java)]
```
