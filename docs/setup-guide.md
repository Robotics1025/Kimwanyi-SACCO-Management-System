# Setup guide

## 1. Database

Create an empty MySQL database named `kimwanyi_sacco` and a dedicated user with
permissions only on that database. Back up the database with `mysqldump` before
upgrades and test restoration regularly.

For this workstation, the local development account is `kimwanyi_app@localhost`.
It has no global MySQL privileges and can access only `kimwanyi_sacco`. It uses
an empty password so the IntelliJ development server can start without storing
the root password. Do not use this passwordless arrangement outside localhost.

## 2. Environment

Required for normal operation:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | Restricted database user |
| `DB_PASSWORD` | Database password |
| `INITIAL_ADMIN_PASSWORD` | One-time initial administrator password |

Optional variables are documented in `src/main/resources/application.properties`.
After the administrator exists, the initial-admin password can be removed from
the service environment.

## 3. Verify and package

Run `./mvnw clean verify`. This compiles the application, runs the tests, and
produces `target/kimwanyi-sacco.war`.

## 4. Deploy

Deploy the WAR to a Jakarta Servlet 6 container. For Tomcat, copy it to
`$CATALINA_BASE/webapps/`, start the container with the environment variables,
and visit `/kimwanyi-sacco/`.

## 5. Acceptance smoke test

1. Sign in as the configured administrator.
2. Register a new member and sign in as that member.
3. Record a deposit and verify the balance/history.
4. Attempt an invalid withdrawal and confirm it is rejected.
5. Apply for a loan, approve it as admin, and record a repayment.
6. Generate the member statement.
7. Verify notifications and audit activity.
8. Disable the member and confirm login is rejected.

## Production notes

- Use `HIBERNATE_DDL_AUTO=validate` and controlled migrations.
- Use TLS at the reverse proxy and secure session cookies.
- Set a container session timeout appropriate to the SACCO's risk policy.
- Set `HIBERNATE_SHOW_SQL=false`.
- Store backups outside the application host and document restoration ownership.
