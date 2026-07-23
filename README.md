# Kimwanyi SACCO Management System

A server-rendered SACCO application for membership, savings, loans, statements,
notifications, audit records, and administration. It uses Java 17, Jakarta Faces
(MyFaces), PrimeFaces, Spring Framework, Spring Data JPA, Hibernate, and MySQL.

## Prerequisites

- JDK 17 or newer (the produced bytecode targets Java 17)
- MySQL 8+
- A Servlet 6 container such as Tomcat 10.1+

## Configure

Create the database and a restricted application user. Supply secrets through
environment variables; do not add them to `application.properties`.

```bash
export DB_URL='jdbc:mysql://localhost:3306/kimwanyi_sacco'
export DB_USERNAME='kimwanyi_app'
export DB_PASSWORD='replace-with-your-database-password'
export INITIAL_ADMIN_USERNAME='admin'
export INITIAL_ADMIN_EMAIL='admin@example.com'
export INITIAL_ADMIN_PASSWORD='replace-with-a-strong-initial-password'
```

The initial administrator is created only when `INITIAL_ADMIN_PASSWORD` is set
and the configured username does not already exist.

On the configured development workstation, omitting `DB_USERNAME` and
`DB_PASSWORD` uses the localhost-only `kimwanyi_app` account. Production must
always supply a password-protected database account through environment variables.

## Build and test

```bash
./mvnw clean verify
```

The deployable file is `target/kimwanyi-sacco.war`.

## Deploy

Copy the WAR into Tomcat's `webapps` directory, start Tomcat with the same
environment variables, then open:

```text
http://localhost:8080/kimwanyi-sacco/
```

For local schema creation, the default Hibernate mode is `update`. Production
operators should set `HIBERNATE_DDL_AUTO=validate` and manage schema changes with
reviewed migrations.

See [docs/setup-guide.md](docs/setup-guide.md),
[docs/database-schema.md](docs/database-schema.md), and
[docs/requirements-implementation-checklist.md](docs/requirements-implementation-checklist.md).
