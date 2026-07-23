# Kimwanyi SACCO Management System

A server-rendered web application for running a SACCO (Savings and Credit Cooperative):
member management, savings, loans, internal transfers, statements, notifications, and a
full audit trail — with separate portals for administrators and members.

Built with **Java 17**, **Jakarta Faces (MyFaces) + PrimeFaces**, the **Spring Framework**,
**Spring Data JPA / Hibernate**, and **MySQL**, packaged as a WAR for a Servlet 6 container.

---

## Features

### Member portal
- **Savings** — view balance and full transaction history (deposits, withdrawals, interest, transfers)
- **Loans** — apply for a loan, track its status, and make repayments
- **Internal transfers** — move funds between member savings accounts
- **Statements** — generate account statements over a date range
- **Notifications** — in-app bell for loan decisions, repayments, and savings activity

### Admin portal
- **Members** — onboard, view, and manage member accounts and statuses
- **Savings** — record cash deposits and withdrawals on a member's behalf
- **Loans** — review, approve, or reject applications and track active/overdue loans
- **Audit logs** — searchable record of security- and money-relevant actions
- **Dashboard** — at-a-glance activity and recent events

### Domain model highlights
| Area | Concepts |
|------|----------|
| Roles | `ADMIN`, `MEMBER` |
| Loan lifecycle | `PENDING → APPROVED → ACTIVE → FULLY_REPAID` (or `REJECTED` / `OVERDUE`) |
| Savings transactions | `DEPOSIT`, `WITHDRAW`, `INTEREST`, `TRANSFER_IN`, `TRANSFER_OUT` |
| Payments | `CARD`, `MOBILE_MONEY` (Airtel Money, MTN Mobile Money) |

---

## Tech stack

- **Language / runtime:** Java 17
- **Web / UI:** Jakarta Faces 4 (Apache MyFaces), PrimeFaces
- **Application:** Spring Framework, Jakarta CDI
- **Persistence:** Spring Data JPA, Hibernate ORM 6
- **Database:** MySQL 8+
- **Security:** Spring Security Crypto (password hashing)
- **Build:** Maven (via the bundled `mvnw` wrapper) → WAR
- **Container:** Servlet 6 / Tomcat 10.1+

---

## Prerequisites

- JDK 17 or newer
- MySQL 8+
- A Servlet 6 container such as Tomcat 10.1+

---

## Configuration

Create the database and a restricted application user, then supply secrets through
**environment variables** — never commit them to `application.properties`.

```bash
export DB_URL='jdbc:mysql://localhost:3306/kimwanyi_sacco'
export DB_USERNAME='kimwanyi_app'
export DB_PASSWORD='replace-with-your-database-password'

# Initial administrator (created only if the username does not already exist)
export INITIAL_ADMIN_USERNAME='admin'
export INITIAL_ADMIN_EMAIL='admin@example.com'
export INITIAL_ADMIN_PASSWORD='replace-with-a-strong-initial-password'
```

The initial administrator is provisioned only when `INITIAL_ADMIN_PASSWORD` is set and the
configured username does not already exist.

On the configured development workstation, omitting `DB_USERNAME` and `DB_PASSWORD` falls
back to the localhost-only `kimwanyi_app` account. **Production must always supply a
password-protected database account through environment variables.**

---

## Build and test

```bash
./mvnw clean verify
```

The deployable artifact is produced at `target/kimwanyi-sacco.war`.

---

## Deploy

Copy the WAR into Tomcat's `webapps` directory, start Tomcat with the same environment
variables, then open:

```text
http://localhost:8080/kimwanyi-sacco/
```

For local schema creation the default Hibernate mode is `update`. Production operators
should set `HIBERNATE_DDL_AUTO=validate` and manage schema changes through reviewed
migrations (see `docs/migrations/`).

---

## Project structure

```
src/main/java/org/joel/kimwanyisacco/
├── common/      shared utilities
├── config/      Spring / Faces / security configuration
├── controller/  JSF backing beans (portal pages)
├── dto/         data-transfer objects
├── model/       JPA entities and enums
├── policy/      business rules (e.g. withdrawal / interest policy)
├── repository/  Spring Data JPA repositories
└── service/     application services (interfaces + Impl)
```

---

## Documentation

- [Setup guide](docs/setup-guide.md)
- [Database schema](docs/database-schema.md)
- [Requirements implementation checklist](docs/requirements-implementation-checklist.md)
</content>
