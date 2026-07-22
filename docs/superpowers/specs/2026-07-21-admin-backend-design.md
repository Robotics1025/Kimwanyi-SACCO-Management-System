# Admin Backend — Dashboard, User Management, Audit Log — Design

Date: 2026-07-21

## Purpose

The three admin-facing pages (`admin/dashboard.xhtml`, `admin/users.xhtml`,
`admin/audit-logs.xhtml`) are empty Facelets stubs with no backing beans.
`DashboardBean`/`DashboardService`/`DashboardServiceImpl`/`DashboardSummaryDto`
are empty scaffold shells; there is no user-management or audit-log backend
at all. This project implements real backend logic for all three admin
pages, following the conventions already established by the auth and
member-registration subsystems (Spring `@Service`/`@Component` beans,
constructor injection, `SpringBeanFacesELResolver`, flat non-paginated
lists).

**Out of scope:** loan approvals (`LoanApprovalBean`/`LoanService` stay
empty shells — a separate future project covering the whole loan domain),
role changes for user accounts, email/token-based password reset.

## Architecture

No architectural changes — this fills in the existing layered structure
(`model` / `dto` / `repository` / `service` / `controller`) with three new
feature areas. All new beans follow the existing patterns:

- Services: `@Service` interface + `*Impl`, constructor-injected
  repositories, `@Transactional` on mutating methods.
- Backing beans: `@Component("beanName")` + `@RequestScope`, constructor
  injection of services, action methods return navigation strings or
  `null` on error (via `FacesMessageUtil`), matching `LoginBean`.
- DTOs: plain fields + getters/setters, converted from entities via a
  `common/util/converter/*Converter` `@Component` (matching
  `MemberConverter`).
- Lists are flat (`List<Dto>`), unpaginated — matches `MemberListBean`.
  Log/user volume doesn't warrant pagination yet.

## A) Audit log subsystem

This is foundational: the dashboard's activity feed and the audit-log page
both read through it, and it needs write points wired into existing
services before there's anything to show.

### Data layer

- **New** `repository/AuditLogRepository.java extends JpaRepository<AuditLog, Long>`:
  - `List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action)`
  - `List<AuditLog> findByUserAccountIdOrderByCreatedAtDesc(Long userAccountId)`
  - `List<AuditLog> findTop10ByOrderByCreatedAtDesc()` — dashboard feed
  - `List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to)`
- **New** `AuditAction` constants: `USER_ACCOUNT_ENABLED`, `USER_ACCOUNT_DISABLED`
  (appended to the existing enum; `PASSWORD_CHANGED` is reused for
  admin-initiated resets, `MEMBER_REGISTERED` and `LOGIN_SUCCESS` /
  `LOGIN_FAILED` / `LOGOUT` already exist).

### Service

- **New** `service/AuditLogService.java` + `AuditLogServiceImpl`:
  - `void record(UserAccount actor, AuditAction action, String entityType, Long entityId, String description)`
    — `actor` may be `null` (failed login before the user is resolved).
    `@Transactional`.
  - `List<AuditLogDto> search(AuditLogFilterForm filter)` — filters by
    action, username (LIKE, joined through `UserAccount`), and
    `dateFrom`/`dateTo`. All fields optional; a blank filter returns every
    entry, newest first.
  - `List<AuditLogDto> findRecent(int limit)` — used by the dashboard.

### DTOs / conversion

- **New** `dto/AuditLogDto.java`: `id`, `actorUsername` (nullable — shown
  as "system"/"unknown" in the view when null), `action`, `entityType`,
  `entityId`, `description`, `createdAt`.
- **New** `dto/AuditLogFilterForm.java`: `action` (nullable enum),
  `username` (nullable string), `dateFrom`/`dateTo` (nullable
  `LocalDate`).
- **New** `common/util/converter/AuditLogConverter.java`: `AuditLog` →
  `AuditLogDto`.

### Controller

- **New** `controller/AuditLogBean.java` (`@Component("auditLogBean")`,
  `@RequestScope`): holds `AuditLogFilterForm filter` and
  `List<AuditLogDto> results`, `search()` action method that delegates to
  `AuditLogService.search(filter)`.

### Write points (the only two instrumented in this project)

- `AuthenticationServiceImpl.authenticate()`:
  - success → `record(userAccount, LOGIN_SUCCESS, "UserAccount", userAccount.getId(), null)`
  - user not found / disabled / bad password → `record(null, LOGIN_FAILED, "UserAccount", null, "Attempted username: " + loginForm.getUsername())`
- `LoginBean.logout()`: read `userSessionBean.getLoggedInUser()` **before**
  invalidating the session, resolve the `UserAccount` via
  `UserAccountRepository.findById(loggedInUser.getId())` (injected into
  `LoginBean` — this is the one place the bean needs a repository
  directly, since `AuditLogService.record(...)` takes the entity, not the
  session DTO) and `record(userAccount, LOGOUT, "UserAccount",
  userAccount.getId(), null)` before returning the redirect outcome. The
  same `findById`-from-session-id pattern is reused in
  `UserManagementBean` to resolve the acting admin for `setEnabled()`/
  `resetPassword()` audit entries.
- `MemberServiceImpl.registerMember()`: after both saves succeed →
  `record(savedAccount, MEMBER_REGISTERED, "Member", member.getId(), "Membership " + membershipNumber)`.

`AuditLogService` is injected into `AuthenticationServiceImpl`,
`LoginBean`, and `MemberServiceImpl` via constructor injection (adds one
parameter to each existing constructor).

## B) Admin dashboard

Fills in the existing empty shells — no new class names.

### Data layer additions (read-only)

- `SavingsAccountRepository`: add
  `@Query("select coalesce(sum(s.balance), 0) from SavingsAccount s") BigDecimal sumAllBalances();`
- **New** `repository/LoanRepository.java extends JpaRepository<Loan, Long>`
  (the `Loan` entity currently has no repository at all):
  - `long countByStatus(LoanStatus status)`
  - `@Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status in ('ACTIVE','OVERDUE')") BigDecimal sumOutstandingBalance();`
  - Read-only usage here; no approval/write logic — that stays with the
    out-of-scope loan-approval project.

### DTO

- `DashboardSummaryDto` fields: `totalMembers`, `activeMembers`,
  `inactiveMembers`, `suspendedMembers` (all `long`), `totalSavingsBalance`
  (`BigDecimal`), `totalOutstandingLoans` (`BigDecimal`),
  `pendingLoanCount`, `activeLoanCount` (`long`),
  `recentActivity: List<AuditLogDto>`.

### Service / bean

- `DashboardService.getSummary(): DashboardSummaryDto`.
- `DashboardServiceImpl`: constructor-injects `MemberRepository`,
  `SavingsAccountRepository`, `LoanRepository`, `AuditLogService`;
  composes one `DashboardSummaryDto` per call (5 count/sum queries +
  `auditLogService.findRecent(10)`). No caching — dashboard traffic is
  low and this keeps it simple.
- `DashboardBean` (`@Component("dashboardBean")`, `@RequestScope`): calls
  `getSummary()` once, exposes the DTO.

## C) User management

### DTO / conversion

- **New** `dto/UserAccountDto.java`: `id`, `username`, `firstName`,
  `lastName`, `email`, `role` (string), `enabled`, `createdAt`.
- **New** `common/util/converter/UserAccountConverter.java`:
  `UserAccount` → `UserAccountDto`.

### Data layer addition

- `UserAccountRepository`: add
  `@Query("select u from UserAccount u where lower(u.username) like lower(concat('%', :kw, '%')) or lower(u.email) like lower(concat('%', :kw, '%')) or lower(u.firstName) like lower(concat('%', :kw, '%')) or lower(u.lastName) like lower(concat('%', :kw, '%'))") List<UserAccount> search(@Param("kw") String keyword);`

### Service

- **New** `service/UserManagementService.java` + Impl:
  - `List<UserAccountDto> search(String keyword)` — blank/null keyword
    returns `findAll()`.
  - `void setEnabled(Long userId, boolean enabled)` — no-op guard if the
    account is already in that state (skip the save + audit write);
    otherwise saves and calls
    `auditLogService.record(actingAdmin, USER_ACCOUNT_ENABLED|USER_ACCOUNT_DISABLED, "UserAccount", userId, null)`.
    Throws `ResourceNotFoundException` if the id doesn't exist.
  - `void resetPassword(Long userId, String newPassword)` — validates
    non-blank/minimum length (reuse the same 8-char minimum as
    registration), re-hashes via the existing `PasswordEncoder` bean,
    saves, calls `auditLogService.record(actingAdmin, PASSWORD_CHANGED, "UserAccount", userId, "Reset by admin")`.
    Throws `ResourceNotFoundException` if the id doesn't exist.
  - Both mutating methods take the acting admin's `UserAccount` (resolved
    by the bean from `UserSessionBean` + `UserAccountRepository`) so the
    audit entry records *who* performed the action, not just the target.

### Controller

- **New** `controller/UserManagementBean.java`
  (`@Component("userManagementBean")`, `@RequestScope`): search box +
  `List<UserAccountDto> results`, `toggleEnabled(Long userId, boolean
  currentlyEnabled)` action, `newPassword` form field +
  `resetPassword(Long userId)` action. Errors surfaced via
  `FacesMessageUtil.addErrorMessage(...)`, matching `LoginBean`.

No role-change action.

## Error handling

Consistent with `MemberServiceImpl`/`LoginBean`:

- Services validate input and throw `IllegalArgumentException` (bad
  input, e.g. blank password) or `ResourceNotFoundException` (unknown
  user id) — both already exist in `common/exception`.
- Backing beans catch at the point of the action method, call
  `FacesMessageUtil.addErrorMessage(...)`, return `null` (redisplay the
  same page) — the existing `LoginBean.login()` pattern.
- `AuditLogService.record(...)` itself must not throw on the caller's
  behalf in a way that aborts the primary action (e.g. a login or
  registration should still succeed even if, hypothetically, the audit
  write had a problem) — but since it's a normal `@Transactional` JPA
  save within the same transaction as the primary write in
  `MemberServiceImpl`/`UserManagementServiceImpl`, and a separate
  best-effort call in `AuthenticationServiceImpl`/`LoginBean` (which have
  no ambient transaction), no special try/catch wrapping is added beyond
  what Spring already does — consistent with not over-engineering error
  handling for a scenario (DB write failure) that would fail every other
  write in the same request anyway.

## Testing

Unit tests per new service class, following `AuthenticationServiceImplTest`
(mocked repositories via Mockito):

- `AuditLogServiceImplTest`: `record()` persists correctly with a null
  actor; `search()` applies each filter field independently and in
  combination; `findRecent()` respects the limit.
- `DashboardServiceImplTest`: `getSummary()` composes all repository
  calls into the DTO correctly (mocked repository return values).
- `UserManagementServiceImplTest`: `search()` blank vs keyword;
  `setEnabled()` no-op when already in target state, writes + audits
  otherwise, throws on unknown id; `resetPassword()` validates length,
  re-hashes, writes + audits, throws on unknown id.

Bean-level tests only where there's real logic beyond delegation
(error-message branches), matching the existing `LoginBeanTest` style —
not exhaustive delegation tests for every getter/setter.

`.xhtml` pages themselves are out of scope for this backend-focused
project (the pages already exist as valid empty stubs); wiring the actual
PrimeFaces markup to these beans is a follow-up.
