# Loan Repayment — Design

## Problem

The Kimwanyi SACCO spec requires members to be able to repay loans (Section 4, "Loan module: apply, approve/reject, repay, view loan status"). The backend logic for this already exists — `LoanServiceImpl.repayLoan()` validates the amount, updates the loan balance, records a `LoanRepayment`, writes an audit log entry, and sends a notification — but it is not reachable from any UI:

- `LoanRepaymentBean` is an empty stub class.
- `loans/repayments.xhtml` is a blank placeholder page, not linked from any navigation menu.
- Nothing calls `repayLoan()` except the unit test.
- `paymentMethod` on the recorded repayment is hardcoded to `"CASH"`.
- `LoanRepayment.receivedBy` exists but is never set.

This gap was found while investigating a garbled user question about how members pay, how internal transfers work, and whether email sending works. Internal transfers (`InternalTransferBean` + `SavingsService.transfer()`) turned out to already be fully wired. Email sending is a separate, larger gap (no SMTP config, `EmailLog` is an unused scaffold) and is intentionally out of scope here — it's the next piece of work after this one.

## Decisions

- **Who can repay:** both. A member can self-record a repayment against their own loan (mirrors the existing `DepositBean` self-service pattern), and an admin can record a repayment on a member's behalf (e.g. cash collected in person).
- **Payment scope:** record-only. No payment gateway integration. The form lets the user pick a method label (Cash, Mobile Money, Bank Transfer) for money that already changed hands offline — same non-integration as the existing Deposit/Withdraw flows. The unrelated `Payment`/`PaymentMethod`/`MobileMoneyProvider` entities (scaffolded for a future real gateway) are untouched.
- **Where the UI lives:** on existing, already-routed pages (`loans/applications.xhtml` for members, `admin/loans.xhtml` for admins) rather than the orphaned `loans/repayments.xhtml` / `loans/details.xhtml` placeholders, which stay untouched and unrouted.

## Backend changes

- `LoanRepaymentForm`: add a `paymentMethod` field (`String`; expected values `CASH`, `MOBILE_MONEY`, `BANK_TRANSFER`), alongside the existing `loanId` and `amount`.
- `LoanService.repayLoan(LoanRepaymentForm form)` → `repayLoan(LoanRepaymentForm form, Long receivedByUserId)`. Mirrors the existing `decideLoan(form, adminUserId)` signature pattern. `receivedByUserId` is `null` for member self-service; when non-null (an admin recorded it), `LoanRepayment.receivedBy` is set to that `UserAccount`.
- `LoanServiceImpl.repayLoan`: keep all existing validation (loan must be `ACTIVE`/`OVERDUE`, amount must be positive and ≤ outstanding balance), balance math, `FULLY_REPAID` transition, audit log entry, and `NotificationType.LOAN_REPAYMENT` notification unchanged. Only change: use `form.getPaymentMethod()` instead of the hardcoded `"CASH"` literal, and set `receivedBy` when `receivedByUserId` is provided.
- `LoanService`: add `getActiveLoans()` returning loans with status `ACTIVE` or `OVERDUE`, alongside the existing `getPendingLoans()`. Used by the admin "Active Loans" tab.
- Update `LoanServiceTest` to cover: payment method is persisted as given (not hardcoded), `receivedBy` is set when an admin id is passed and stays `null` for member self-service, and existing over-repayment / inactive-loan validation still holds against the new two-arg signature.

## Member-facing UI (`loans/applications.xhtml`)

- Add an **Actions** column to the existing loans table. A "Repay" link renders only when `loan.status` is `ACTIVE` or `OVERDUE` (the "one active loan at a time" business rule means a member never has more than one repayable row at a time, so no loan picker is needed beyond the row clicked).
- Clicking "Repay" opens a `p:dialog` (same idiom as the admin reject dialog on `admin/loans.xhtml`): shows the loan's outstanding balance, an amount input, and a payment-method dropdown (Cash / Mobile Money / Bank Transfer).
- Submitting calls the real `LoanRepaymentBean` (replacing the empty stub, `@Component("loanRepaymentBean")`, session-scoped like `InternalTransferBean`/`DepositBean`) → `loanService.repayLoan(form, null)`.
- On success: close the dialog, refresh the loans table (updated outstanding balance/status badge) and the notification bell via `f:ajax`/`update`, consistent with existing patterns.
- Validation errors (e.g. amount exceeds outstanding, non-positive amount) surface through `FacesMessageUtil`, same as other beans.

## Admin-facing UI (`admin/loans.xhtml`)

- Wrap existing content in a `p:tabView` with two tabs, keeping the same route/nav item (no new page, no new nav link):
  - **Tab 1 — "Pending Applications":** the existing approve/reject `dataTable`, unchanged.
  - **Tab 2 — "Active Loans":** new `dataTable` over `loanService.getActiveLoans()` (columns: member, principal, outstanding, due date, status), with a "Record Repayment" action per row opening the same style of dialog as the member page.
- The admin dialog submits through the same `LoanRepaymentBean`, but passes the logged-in admin's user id as `receivedByUserId`. The bean determines this itself by checking `userSessionBean.getLoggedInUser().getRoles()` — there's no manual "who received this" field for the admin to fill in.

## Explicitly out of scope

- Real payment gateway / mobile money API integration.
- Any change to `Payment`, `PaymentMethod`, or `MobileMoneyProvider` — those remain an unrelated, unwired scaffold for possible future work.
- `loans/repayments.xhtml` and `loans/details.xhtml` — left as unrouted placeholders, not touched.
- Real email sending (SMTP/Gmail + `EmailService`) — a separate, larger gap identified in the same investigation; to be brainstormed and built as its own follow-up piece of work.
