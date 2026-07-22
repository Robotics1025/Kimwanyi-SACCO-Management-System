# Notification Bell + Withdrawal Implementation

**Date:** 2026-07-22
**Status:** Approved for planning

## Background

The `Notification` entity and `NotificationType` enum already exist (table already created via `hibernate.hbm2ddl.auto=update`), but nothing reads or writes them — no repository, service, or controller exists. The bell icon in both `admin-template.xhtml` and `member-template.xhtml` is pure decoration: a static `<i class="fa-regular fa-bell">` with a hardcoded red dot, no click handler, no data.

Separately, `SavingsServiceImpl.withdraw()` and `WithdrawalPolicy.isWithdrawalAllowed()` are both stubs (`throw new UnsupportedOperationException`), even though the admin Savings Accounts page's "Withdraw" button already calls `savingsService.withdraw(form)` — today that call fails every time with a caught exception shown as an error toast. Since "a deposit/withdrawal is processed" is one of the notification trigger events the user wants, withdrawals need to actually work first.

## Scope

**In scope:**
1. Implement `WithdrawalPolicy.isWithdrawalAllowed()` and `SavingsServiceImpl.withdraw()`.
2. Notification backend: repository, converter, DTO, service.
3. Wire real notification-creation calls into: loan applied (→ all admins), loan approved/rejected (→ applicant), loan repayment recorded (→ applicant), deposit processed (→ account owner), withdrawal processed (→ account owner).
4. Replace the static bell icon in both templates with a working PrimeFaces overlay-panel dropdown backed by a shared `NotificationBean`: unread count badge, recent notifications list, mark-as-read / mark-all-as-read.

**Explicitly out of scope:**
- `SYSTEM` notification type (no generic system event to hook it to; not requested).
- Clicking a notification navigating to a target page (nice-to-have, not requested; click just marks it read).
- Real-time push/polling — notifications load fresh on each page render, matching how every other bean in this app already works (`@RequestScope`, reload on `@PostConstruct`).
- Email notifications (spec's separate optional item, not part of this request).

## Design

### Withdrawal implementation

`WithdrawalPolicy.isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount)`:
- Returns `false` if `requestedAmount` is null or `<= 0`.
- Returns `false` if `currentBalance.subtract(requestedAmount)` would drop below `UGX 20,000` (the same `MINIMUM_BALANCE` constant `SavingsServiceImpl.transfer()` already uses).
- Returns `true` otherwise.

`SavingsServiceImpl.withdraw(WithdrawalForm form)` mirrors `deposit()`'s structure exactly: load the `SavingsAccount`, check `withdrawalPolicy.isWithdrawalAllowed(...)` (throw `IllegalArgumentException` with a clear message if not), subtract the balance, save, record a `SavingsTransaction` of type `WITHDRAW` with a `WTH-` prefixed reference (mirroring `deposit()`'s `DEP-` prefix), return the mapped DTO.

### Notification backend

- **`NotificationRepository`** (new): `List<Notification> findTop15ByUserAccountIdOrderByCreatedAtDesc(Long userAccountId)`; `long countByUserAccountIdAndReadStatusFalse(Long userAccountId)`; a `@Modifying @Query` bulk-update for `markAllAsRead(Long userAccountId)` (sets `readStatus = true`, `readAt = now()` for all unread rows of that user, in one statement rather than loading + saving each entity).
- **`NotificationConverter`** (new): `toDto(Notification)` → `NotificationDto` (id, title, message, `type.name()`, readStatus, createdAt), following the exact pattern of `SavingsAccountConverter`/`AuditLogConverter`.
- **`NotificationDto`** (new): plain fields matching the converter output.
- **`UserAccountRepository.findByRole(Role role)`** (new method on the existing repository): needed to resolve "all admins" for loan-application notifications.
- **`NotificationService`/`NotificationServiceImpl`** (new):
  - `notify(UserAccount recipient, NotificationType type, String title, String message)` — builds and saves one `Notification`.
  - `notifyAdmins(NotificationType type, String title, String message)` — `userAccountRepository.findByRole(Role.ADMIN)`, calls `notify()` for each.
  - `getRecent(Long userAccountId)` → `List<NotificationDto>` (top 15, newest first).
  - `getUnreadCount(Long userAccountId)` → `long`.
  - `markAsRead(Long notificationId)` — loads, sets `readStatus = true`, saves.
  - `markAllAsRead(Long userAccountId)` — delegates to the repository bulk update.

### Event wiring (all in existing, already-transactional service methods — one line added per event)

- `LoanServiceImpl.applyLoan()`, after `loanRepository.save(loan)`: `notificationService.notifyAdmins(LOAN_APPLICATION, "New Loan Application", member.getUserAccount().getUsername() + " applied for UGX " + principal)`.
- `LoanServiceImpl.decideLoan()`, in the approved branch: `notificationService.notify(loan.getMember().getUserAccount(), LOAN_APPROVED, "Loan Approved", "Your loan of UGX " + loan.getPrincipal() + " was approved.")`; in the rejected branch: `LOAN_REJECTED` with the rejection reason in the message.
- `LoanServiceImpl.repayLoan()`, after saving the repayment: `notificationService.notify(loan.getMember().getUserAccount(), LOAN_REPAYMENT, "Repayment Recorded", "UGX " + amount + " repaid. Outstanding balance: UGX " + newOutstanding + ".")`.
- `SavingsServiceImpl.deposit()`, after saving the transaction: `notificationService.notify(account.getMember().getUserAccount(), DEPOSIT, "Deposit Received", "UGX " + form.getAmount() + " deposited. New balance: UGX " + balanceAfter + ".")`.
- `SavingsServiceImpl.withdraw()`, after saving the transaction: `notificationService.notify(account.getMember().getUserAccount(), WITHDRAWAL, "Withdrawal Processed", "UGX " + form.getAmount() + " withdrawn. New balance: UGX " + balanceAfter + ".")`.

### UI: the bell icon

One shared `@Component("notificationBean") @RequestScope` bean, used identically from `admin-template.xhtml` and `member-template.xhtml`. On `@PostConstruct`, resolves the current `UserAccount` id from `userSessionBean.getLoggedInUser().getId()` and loads `recent` (`List<NotificationDto>`) + `unreadCount` (`long`).

Both templates replace their static bell `<i>` with:
- The bell icon wrapped so it toggles a `<p:overlayPanel>` (`for` targeting the bell's id, `dismissable="true"`).
- A badge `<span>` showing `notificationBean.unreadCount` when `> 0`, hidden otherwise (replacing the hardcoded red dot).
- Panel content: header "Notifications" + a "Mark all read" `p:commandLink` (`rendered="#{notificationBean.unreadCount > 0}"`, ajax-updates the panel + badge) calling `markAllAsRead()`; an `<ui:repeat>` over `recent` rendering each as icon (per `type`) + title + message + absolute timestamp via `<f:convertDateTime type="localDateTime" pattern="dd MMM yyyy HH:mm"/>` (matching the existing convention used on the Savings History and dashboard pages), with an unread item getting a highlighted background; a `p:commandLink` per item calling `markAsRead(notification.id)` (ajax-updates itself + the badge); empty state "No notifications yet" when `recent` is empty.

## Testing plan

- `WithdrawalPolicyTest` (un-disable the existing scaffold): allowed/blocked cases (exact minimum boundary, negative/zero amount, amount exceeding balance).
- `SavingsServiceTest`: extend with `withdraw()` happy path + policy-rejection path, mirroring existing `deposit()` test structure.
- `NotificationServiceImplTest` (new): `notify()`, `notifyAdmins()` (multiple admins), `getRecent()`, `getUnreadCount()`, `markAsRead()`, `markAllAsRead()`.
- `LoanServiceTest`/existing loan tests: extend to verify a notification call happens on apply/decide/repay (mock `NotificationService`, verify interaction — not re-testing notification internals here).
- Manual in-browser verification (agent-browser): trigger each of the 5 events for real, confirm the bell badge count updates and the dropdown shows the right entries; confirm mark-as-read and mark-all-read work.
