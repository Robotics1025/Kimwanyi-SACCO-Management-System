# Member Self-Service Pages: Apply for Loan, My Loans, Savings History

**Date:** 2026-07-21
**Status:** Approved for planning

## Background

The member portal (`member-template.xhtml`, `member.css`) and its navigation already link to `/loans/apply`, `/loans/applications`, and `/savings/history`, but all three view files are empty 5-line stubs. Backing controllers are mostly stubs too:

- `LoanApplicationBean` — empty class, no fields or methods.
- `SavingsHistoryBean` — has fields but no loading logic (never populated).
- `SavingsServiceImpl` — every method throws `UnsupportedOperationException`.

Meanwhile the service/policy layer for loans is fully implemented and unused:

- `LoanService.applyLoan()` resolves the member's `SavingsAccount`, runs `LoanEligibilityPolicy` (blocks a second PENDING/ACTIVE/OVERDUE loan; caps principal at 3x savings balance), computes a flat 10% interest via `LoanInterestCalculator`, and persists the `Loan`.
- `LoanService.getLoansByMember()` already exists for listing.

Separately, member self-registration (`MemberServiceImpl.registerMember`) creates a `Member` + `UserAccount` but never a `SavingsAccount`, even though `SavingsAccount.member` is a required (`optional = false`, unique) one-to-one relationship. This is a gap that blocks the whole savings/loan flow for any newly registered member, since `LoanService.applyLoan()` requires a `SavingsAccount` to already exist.

This spec was validated against the official project document ("Kimwanyi SACCO Management System", Pahappa Limited, v01, 2025-06-17): "Savings module: ...view balance and history" and "Loan module: apply... view loan status" are explicit in-scope deliverables. See project memory `project_spec.md` for the full business rules. One gap noted but explicitly out of scope here: the spec's 5% p.a. monthly savings interest rule has no implementation anywhere in the codebase — not addressed by this spec.

## Scope

**In scope:**
1. Auto-provision a `SavingsAccount` when a member registers.
2. Implement the two read paths of `SavingsServiceImpl` needed for the history page: `getAccountById`, `getTransactionHistory`.
3. Wire `LoanApplicationBean` to `LoanService.applyLoan()`.
4. New `MemberLoansBean` for the My Loans list page.
5. Finish `SavingsHistoryBean` to load transactions on page view.
6. Build out the three view files: `loans/apply.xhtml`, `loans/applications.xhtml`, `savings/history.xhtml`.

**Out of scope (explicitly deferred, not addressed by this spec):**
- Deposit / withdraw pages and `SavingsServiceImpl.deposit()` / `withdraw()`.
- A dedicated "Create Savings Account" page (dropped — auto-creation at registration covers it; see decision below).
- Admin tooling to manually reopen/create a savings account for edge cases.
- Loan repayment page.
- Print/export of statements (existing `StatementBean`/`StatementService` stub is untouched).
- 5% p.a. monthly savings interest calculation.

## Decision: savings account creation

Auto-create the `SavingsAccount` inside `MemberServiceImpl.registerMember()`, in the same transaction as the `Member`/`UserAccount` creation. Balance starts at 0, `minimumBalance` uses the entity default (UGX 20,000). No separate "create account" UI is needed — this was evaluated and dropped in favor of auto-creation, which matches how `membershipNumber` is already auto-generated in the same method.

## Backend design

### `SavingsAccountNumberGenerator` (new, `common/util`)

Mirrors `MembershipNumberGenerator`: prefix `SAV-{year}-`, sequence padded to 4 digits (`SAV-2026-0001`), collision-checked via `SavingsAccountRepository.existsByAccountNumber`, sequence seeded from a new `SavingsAccountRepository.countByAccountNumberStartingWith(prefix)`.

### `MemberServiceImpl.registerMember()`

After `memberRepository.save(member)`, construct a `SavingsAccount` (member = savedMember, accountNumber = generated, balance = ZERO) and save it via `SavingsAccountRepository`, before the audit log call. Inject `SavingsAccountRepository` and `SavingsAccountNumberGenerator` as new constructor dependencies.

### `SavingsServiceImpl`

- `getAccountById(Long id)`: load `SavingsAccount` by id (404-style `IllegalArgumentException` if missing), map to `SavingsAccountDto` (id, accountNumber, memberId, balance).
- `getTransactionHistory(Long savingsAccountId)`: `SavingsTransactionRepository` query for transactions by account, ordered newest-first, mapped to `SavingsTransactionDto` (id, transactionType from `TransactionType` enum name, amount, balanceAfter, createdAt).
- `deposit()` / `withdraw()` remain `UnsupportedOperationException` — untouched.

### `LoanApplicationBean` (rewrite)

`@Component @RequestScope`, injects `LoanService`, `MemberRepository`, `UserSessionBean`. Holds a `LoanApplicationForm`-shaped view model (principal amount, term months — memberId is resolved server-side, never client-supplied). On init (`@PostConstruct`), resolves the current member via `memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())` and loads their loans (`loanService.getLoansByMember`) to determine whether an active/pending loan already exists (drives the "form vs. status card" branch in the view). `apply()` method builds the `LoanApplicationForm`, calls `loanService.applyLoan()`, adds a success/error `FacesMessage` (following the `MemberRegistrationBean` convention), and redirects to `/loans/applications` on success.

### `MemberLoansBean` (new)

`@Component @RequestScope`, injects `LoanService`, `MemberRepository`, `UserSessionBean`. On init, resolves the member and loads `loanService.getLoansByMember(memberId)`, newest-first.

### `SavingsHistoryBean` (finish)

Add `@PostConstruct` init that resolves the member's `SavingsAccount` (via a new `SavingsAccountRepository` injection) and populates `transactions` via `savingsService.getTransactionHistory()`, plus expose the account (balance, account number) for the page header.

## Page designs

### `/loans/apply` (Apply for Loan)

- If member already has a PENDING/ACTIVE/OVERDUE loan: show a `mem-card` status message ("You have a loan in progress — status: X") with a link to My Loans, no form.
- Otherwise: `mem-card` with a form — Principal Amount (UGX), Term (months). Helper text shows "Maximum eligible: UGX X (3x your savings balance)" and "Interest: flat 10% of principal". Submit calls `LoanApplicationBean.apply()`.
- Client-side: reject non-positive amounts and amounts over the 3x cap before submit, mirroring (not replacing) the server-side `LoanEligibilityPolicy` check.

### `/loans/applications` (My Loans)

- `mem-table`: Applied Date, Principal, Interest, Total Repayable, Outstanding Balance, Status (`.badge-*`, color mapped per `LoanStatus`: PENDING=yellow, ACTIVE=blue, FULLY_REPAID=green, REJECTED/OVERDUE=red), Due Date.
- Empty state: `.mem-empty` pattern, "No loan applications yet" + Apply for Loan button.

### `/savings/history` (Savings History)

- Header strip: account number + current balance (from `SavingsAccountDto`).
- `mem-table`: Date, Type (badge), Amount, Balance After. Newest first.
- Empty state: `.mem-empty` pattern, "No transactions yet".

## Testing plan

- `SavingsAccountNumberGeneratorTest` — format + uniqueness, mirrors `MembershipNumberGeneratorTest`.
- `MemberServiceTest` — extend to assert a `SavingsAccount` is created alongside the `Member`.
- `SavingsServiceImplTest` (replacing the disabled scaffold) — `getAccountById`/`getTransactionHistory`, happy path + not-found.
- Controller tests for `LoanApplicationBean`, `MemberLoansBean`, `SavingsHistoryBean` following the `LoginBeanTest` pattern (mock services, verify form binding/navigation outcomes).
- Manual in-browser verification of all three pages via agent-browser after implementation (matches how the dashboard work was verified).
