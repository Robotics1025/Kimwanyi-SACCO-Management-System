# Database schema

The diagram is derived from the JPA entities in `org.joel.kimwanyisacco.model`.

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
    }
    MEMBERS {
        bigint id PK
        bigint user_account_id FK,UK
        varchar membership_number UK
        varchar national_id UK
        varchar phone_number
        varchar status
    }
    SAVINGS_ACCOUNTS {
        bigint id PK
        bigint member_id FK,UK
        varchar account_number UK
        decimal balance
        decimal minimum_balance
        bigint version
    }
    SAVINGS_TRANSACTIONS {
        bigint id PK
        bigint savings_account_id FK
        varchar reference UK
        varchar type
        decimal amount
        decimal balance_before
        decimal balance_after
    }
    LOANS {
        bigint id PK
        bigint member_id FK
        decimal principal
        decimal interest_amount
        decimal outstanding_balance
        varchar status
        date due_date
        bigint version
    }
    LOAN_REPAYMENTS {
        bigint id PK
        bigint loan_id FK
        varchar reference UK
        decimal amount
        decimal balance_before
        decimal balance_after
    }
```

Hibernate maintains the local development schema when `HIBERNATE_DDL_AUTO=update`.
Production deployments should use reviewed migrations and `validate` mode.
