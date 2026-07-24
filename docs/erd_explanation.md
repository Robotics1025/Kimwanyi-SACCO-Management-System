# Kimwanyi SACCO Management System: Entity Relationship Diagram (ERD) & Database Schema

This guide provides the Entity Relationship Diagram (ERD) and a detailed explanation of the database tables, keys, fields, and relationships.

---

## 1. Entity Relationship Diagram (Mermaid Visualization)

```mermaid
erDiagram
    USER_ACCOUNTS ||--o| MEMBERS : owns
    MEMBERS ||--|| SAVINGS_ACCOUNTS : has
    MEMBERS ||--o{ LOANS : applies
    SAVINGS_ACCOUNTS ||--o{ SAVINGS_TRANSACTIONS : records
    SAVINGS_ACCOUNTS ||--o{ INTERNAL_TRANSFERS : sends
    SAVINGS_ACCOUNTS ||--o{ INTERNAL_TRANSFERS : receives
    LOANS ||--o{ LOAN_REPAYMENTS : receives
    USER_ACCOUNTS ||--o{ NOTIFICATIONS : receives
    USER_ACCOUNTS ||--o{ AUDIT_LOGS : performs
    USER_ACCOUNTS ||--o{ EMAIL_LOGS : receives
    USER_ACCOUNTS ||--o{ PAYMENTS : owns

    USER_ACCOUNTS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar email UK
        varchar role
        boolean enabled
        datetime created_at
        datetime updated_at
    }
    MEMBERS {
        bigint id PK
        bigint user_account_id FK,UK
        varchar membership_number UK
        varchar national_id UK
        varchar phone_number
        varchar status
        date joined_at
        datetime created_at
        datetime updated_at
    }
    SAVINGS_ACCOUNTS {
        bigint id PK
        bigint member_id FK,UK
        varchar account_number UK
        decimal balance
        bigint version
        datetime created_at
        datetime updated_at
    }
    SAVINGS_TRANSACTIONS {
        bigint id PK
        bigint savings_account_id FK
        varchar reference UK
        varchar type
        decimal amount
        decimal balance_before
        decimal balance_after
        varchar description
        datetime created_at
    }
    LOANS {
        bigint id PK
        bigint member_id FK
        bigint decided_by_id FK
        decimal principal
        decimal interest_rate
        decimal interest_amount
        decimal total_repayable
        decimal amount_repaid
        decimal outstanding_balance
        varchar status
        varchar purpose
        varchar rejection_reason
        date application_date
        date decision_date
        date due_date
        bigint version
        datetime created_at
        datetime updated_at
    }
    LOAN_REPAYMENTS {
        bigint id PK
        bigint loan_id FK
        varchar reference UK
        decimal amount
        decimal balance_before
        decimal balance_after
        varchar payment_method
        date payment_date
        datetime created_at
        datetime updated_at
    }
    AUDIT_LOGS {
        bigint id PK
        bigint user_account_id FK
        varchar action
        varchar entity_name
        bigint entity_id
        varchar details
        datetime created_at
    }
    NOTIFICATIONS {
        bigint id PK
        bigint user_account_id FK
        varchar title
        varchar message
        varchar type
        boolean is_read
        datetime created_at
    }
```

---

## 2. Explanation of Core Database Relationships

### A. One-to-One Relationships (1 : 1)
* **`USER_ACCOUNTS` $\leftrightarrow$ `MEMBERS`**:
  - **Relationship**: A single login credential account (`USER_ACCOUNTS`) owns exactly one cooperative member profile (`MEMBERS`). An admin user, however, has a login account but does not have a member profile record.
  - **Foreign Key**: `members.user_account_id` $\rightarrow$ `user_accounts.id` (Unique).
* **`MEMBERS` $\leftrightarrow$ `SAVINGS_ACCOUNTS`**:
  - **Relationship**: Each cooperative member has exactly one savings account to deposit or withdraw funds.
  - **Foreign Key**: `savings_accounts.member_id` $\rightarrow$ `members.id` (Unique).

### B. One-to-Many Relationships (1 : N)
* **`MEMBERS` $\rightarrow$ `LOANS`**:
  - **Relationship**: A member can apply for multiple loans over time. However, business policies enforce that they can only have one *active* or *pending* loan at a single time.
  - **Foreign Key**: `loans.member_id` $\rightarrow$ `members.id`.
* **`LOANS` $\rightarrow$ `LOAN_REPAYMENTS`**:
  - **Relationship**: A single loan accumulates multiple repayments over its lifecycle until the outstanding balance reaches zero.
  - **Foreign Key**: `loan_repayments.loan_id` $\rightarrow$ `loans.id`.
* **`SAVINGS_ACCOUNTS` $\rightarrow$ `SAVINGS_TRANSACTIONS`**:
  - **Relationship**: A savings account accumulates a ledger log of all deposits, withdrawals, internal transfers, and interest postings.
  - **Foreign Key**: `savings_transactions.savings_account_id` $\rightarrow$ `savings_accounts.id`.
* **`USER_ACCOUNTS` $\rightarrow$ `NOTIFICATIONS` & `AUDIT_LOGS`**:
  - **Relationship**: Users (both members and admins) receive multiple notifications and produce multiple security or financial audit log trails.

---

## 3. Important Table Key Constraints

1. **Unique Indexes (UK)**:
   - `user_accounts.username`: Avoids duplicate login names.
   - `user_accounts.email`: Avoids multiple logins sharing the same email address.
   - `members.national_id`: Enforces that each member registers with a unique National Identification Card.
   - `members.membership_number`: Enforces uniquely sequential membership numbering (e.g. `KIM-001`).
   - `savings_accounts.account_number`: Enforces unique account identification (e.g. `SAV-129847`).
   - `savings_transactions.reference`: Guarantees transaction idempotency (e.g., prevents crediting interest twice).
2. **JPA Version Columns (Optimistic Locking)**:
   - `savings_accounts.version` & `loans.version` use JPA `@Version` counters. This prevents **race conditions** (e.g. if two threads try to withdraw or apply repayments concurrently, the transaction that commits second will fail rather than overwriting balance calculations).
3. **Foreign Keys (FK)**:
   - Enforce database referential integrity. Deleting a record with active dependent relations is blocked.
