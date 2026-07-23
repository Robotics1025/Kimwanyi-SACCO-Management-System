# Kimwanyi SACCO Management System - Requirements Checklist

Source: Pahappa Limited requirements document prepared by Rujumba Leonard,
Shaun Kendrone, and Duku Allan on 17 June 2025.

Last audited: 23 July 2026

## Status legend

- [x] Implemented and covered by the current application or automated tests
- [ ] Not implemented or not yet verified end to end
- [~] Partially implemented; remaining work is listed below it

## Current verification baseline

- [x] Project compiles for Java 17 using Maven `release` mode.
- [x] `./mvnw clean verify` succeeds.
- [x] 67 automated tests pass with zero failures and zero errors.
- [x] `target/kimwanyi-sacco.war` is generated successfully.
- [ ] Run the complete application against a clean MySQL database and verify every reachable page in a browser.

## 1. Member registration, authentication, and profiles

- [x] Register a member with username, password, name, email, and National ID.
- [x] Reject duplicate usernames.
- [x] Reject duplicate email addresses.
- [x] Reject duplicate National IDs.
- [x] Generate a unique membership number.
- [x] Create one savings account automatically for a new member.
- [x] Hash passwords with BCrypt.
- [x] Log in and log out.
- [x] Reject disabled accounts during authentication.
- [x] Protect authenticated pages with `SecurityFilter`.
- [x] Protect `/admin/*` pages from member accounts.
- [~] Admin can enable or disable a login account.
  - [x] Synchronize the related `Member.status` with account activation/deactivation.
  - [x] Prevent an administrator from disabling their own account.
  - [x] Enforce admin authorization inside `UserManagementService`, not only in the UI/filter.
- [~] Member profile management.
  - [x] Implement loading a member profile in `MemberProfileBean`.
  - [x] Implement member profile retrieval/update methods in `MemberService`.
  - [x] Implement `members/profile.xhtml`.
  - [x] Implement `members/edit.xhtml`.
- [~] Admin member listing.
  - [x] Implement member loading/searching in `MemberListBean` and `MemberService`.
  - [x] Implement `members/list.xhtml`.
- [x] Admin and member roles are represented separately.
- [~] Prevent an admin account from becoming a savings member.
  - [x] Normal member registration always creates the `MEMBER` role.
  - [ ] Add a database/service invariant test proving an `ADMIN` cannot be linked to a `Member`.

## 2. Savings

- [x] Create a savings account with a unique account number.
- [x] Record deposits and update the authoritative balance transactionally.
- [x] Record withdrawals and update the authoritative balance transactionally.
- [x] Reject zero or negative deposits.
- [x] Reject withdrawals that would leave less than UGX 20,000.
- [x] Therefore reject withdrawals exceeding the available balance.
- [x] View savings balance and transaction history on implemented account/dashboard pages.
- [x] Record balance-before and balance-after values for transactions.
- [x] Calculate monthly savings interest as `balance * 5% / 12`, rounded to two decimal places.
- [~] Apply savings interest monthly.
  - [x] Add a service operation that credits interest as an `INTEREST` transaction.
  - [x] Make posting idempotent so the same account cannot receive interest twice for one month.
  - [x] Add a scheduled or administrator-triggered month-end posting process.
  - [ ] Add integration tests for interest posting and reruns.
- [~] Dedicated savings pages.
  - [x] `savings/account.xhtml` displays transaction history.
  - [ ] Implement or remove the empty `savings/deposit.xhtml` page.
  - [ ] Implement or remove the empty `savings/withdraw.xhtml` page.
  - [ ] Implement or remove the empty `savings/balance.xhtml` page.

## 3. Loans

- [x] Member can apply for a loan.
- [x] Reject missing, zero, or negative loan amounts.
- [x] Only active members can apply.
- [x] Maximum principal is three times the current savings balance.
- [x] Flat loan interest is 10% of principal.
- [x] Prevent a new application while a loan is pending, active, or overdue.
- [x] Store pending, active, rejected, overdue, and fully repaid statuses.
- [x] Admin UI can approve or reject pending applications.
- [x] Reject a second decision on a non-pending loan.
- [x] Approval sets a 12-month due date.
- [x] Loan repayment updates amount repaid and outstanding balance.
- [x] Reject non-positive repayments and overpayments.
- [x] Mark a loan fully repaid when its balance reaches zero.
- [x] Member can view loan application/status information.
- [~] Enforce admin-only decisions at the service layer.
  - [x] Verify that `adminUserId` belongs to an enabled user with the `ADMIN` role before approving/rejecting.
  - [ ] Add authorization tests for member and disabled-admin attempts.
- [~] Loan purpose and term.
  - [ ] Persist the purpose supplied by the member instead of always using `General Loan`.
  - [ ] Either apply the selected term or remove the unused term field; approval currently always uses 12 months.
- [x] Automatically mark active loans as overdue after their due date.
- [ ] Add an administrator-visible overdue-loans list/report.
- [~] Dedicated loan pages.
  - [x] `admin/loans.xhtml` implements approvals and rejections.
  - [x] `loans/applications.xhtml` displays member loans.
  - [x] Implement `LoanRepaymentBean` and `loans/repayments.xhtml`, or deliberately route repayment through another finished page.
  - [ ] Implement or remove empty `loans/approvals.xhtml` and `loans/details.xhtml` pages.

## 4. Account statements

- [x] Define fields for `AccountStatementDto` and `StatementEntryDto`.
- [x] Define the `StatementService` contract.
- [x] Implement `StatementServiceImpl` as a Spring service.
- [ ] Combine deposits, withdrawals, transfers, interest, and loan repayments into a dated member statement.
- [x] Support a date range and opening/closing balances.
- [x] Implement `StatementBean`.
- [x] Implement `statements/account-statement.xhtml`.
- [x] Add printable statement output.
- [ ] Add unit and integration tests.

## 5. Admin dashboard and reporting

- [x] Show total, active, inactive, and suspended member counts.
- [x] Show total savings balance.
- [x] Show pending and active loan counts.
- [x] Show total outstanding loan balance.
- [x] Show recent audit activity.
- [~] Dashboard chart.
  - [ ] Replace generated linear sample values with real monthly transaction and loan aggregates.
- [ ] Show overdue-loan count and total overdue balance.
- [ ] Stop silently swallowing repository errors in `DashboardServiceImpl`; log them and show an operational error state.

## 6. Optional features already present

- [x] In-system notifications for deposits, withdrawals, and loan events.
- [x] Mark one or all notifications as read.
- [x] Audit logs for important member, user, savings, and loan actions.
- [x] Internal account-to-account transfers.
- [x] Internal transfers preserve the UGX 20,000 minimum sender balance.
- [ ] Email delivery is not implemented; only notification/email model scaffolding exists.
- [ ] Advanced financial reports are not implemented.

## 7. Configuration and security

Current file: `src/main/resources/application.properties`

- [x] MySQL URL, driver, Hibernate, and application settings exist.
- [x] Remove the committed database password (`db.password=1025`).
- [x] Read database URL, username, and password from environment variables or container/JNDI configuration.
- [x] Keep a safe local configuration without real credentials.
- [x] Turn `hibernate.show_sql` off by default.
- [ ] Replace `hibernate.hbm2ddl.auto=update` with controlled migrations for production.
- [x] Remove the fixed `admin/admin123` production seed credentials.
- [x] Seed the initial admin from environment-provided credentials.
- [ ] Add CSRF protection or document and implement an equivalent JSF request-protection strategy.
- [x] Add session timeout and HTTP-only cookie configuration.
- [x] Add database backup and restore instructions to address the data-loss requirement.

## 8. Required deliverables

- [x] Full source code exists in the repository.
- [x] Maven can produce a deployable WAR.
- [~] Working demo.
  - [ ] Verify MySQL initialization on a clean machine/database.
  - [ ] Deploy the generated WAR to the documented servlet container.
  - [ ] Complete a browser smoke test for registration, login, savings, loans, admin decisions, and logout.
- [x] Replace the short README with prerequisites, configuration, build, test, and deployment instructions.
- [x] Add a real setup guide under `docs/`.
- [x] Add a real schema/ER diagram under `docs/`.
- [x] Align deployment documentation with the Servlet 6/Tomcat packaging.

## Recommended implementation order

Follow these phases in order so later features build on stable foundations.

### Phase 1 - Security and configuration

- [ ] Externalize database and initial-admin credentials.
- [ ] Enforce role authorization inside services.
- [ ] Synchronize member status and login status.
- [ ] Add authentication/authorization integration tests.

### Phase 2 - Required member functionality

- [ ] Complete member lookup, list, profile, and edit flows.
- [ ] Complete account statement generation and UI.
- [ ] Complete a usable repayment UI.

### Phase 3 - Financial automation

- [ ] Implement idempotent monthly savings-interest posting.
- [ ] Implement automatic overdue-loan status updates.
- [ ] Replace simulated dashboard chart data with real aggregates.

### Phase 4 - Documentation and demo readiness

- [ ] Write the complete README and setup guide.
- [ ] Produce the real database schema diagram.
- [ ] Verify a clean MySQL/Tomcat installation.
- [ ] Run and record the complete browser acceptance checklist.

## Definition of done

The requirements are complete only when every non-optional item above is checked,
`./mvnw clean verify` passes, the WAR deploys against a clean MySQL database,
and the complete member/admin workflow passes browser acceptance testing.
