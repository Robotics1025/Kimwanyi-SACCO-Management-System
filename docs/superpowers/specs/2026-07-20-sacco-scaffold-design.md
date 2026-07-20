# Kimwanyi SACCO Management System — Project Scaffold Design

Date: 2026-07-20
Revised: 2026-07-20 — switched from CDI to Spring Framework; target WildFly +
MySQL; dropped Flyway (Hibernate-managed schema); flattened package structure
to one package per architectural layer instead of one per domain module.

## Purpose

Scaffold the full directory/package/file structure for the SACCO Management
System, without implementing business logic. Architecture: JSF (Jakarta
Faces) + PrimeFaces for views, Spring Framework for dependency injection and
services, Spring Data JPA + Hibernate for persistence, packaged as a WAR on
WildFly with a MySQL database.

## Architecture

- **View layer**: JSF (Jakarta Faces) with PrimeFaces components. `.xhtml`
  pages/templates/fragments.
- **DI/service layer**: Spring Framework (not CDI/Weld). Services and
  policies are `@Service`/`@Component` Spring beans. JSF backing beans are
  plain classes registered as Spring beans (`@Component`, constructor
  injection, `@RequestScope`/`@SessionScope`) and exposed to JSF EL via
  `org.springframework.web.jsf.el.SpringBeanFacesELResolver` — no
  `@Named`/`@Inject`/CDI scopes anywhere in the codebase.
- **Persistence**: Spring Data JPA repository interfaces
  (`extends JpaRepository<Entity, Long>`) with Hibernate as the JPA
  provider (supplied by WildFly, `provided` scope). No hand-written
  `*RepositoryImpl.java` classes. No `persistence.xml` — `PersistenceConfig`
  (Java config) defines the `DataSource`/`EntityManagerFactory`/
  `JpaTransactionManager` beans directly, reading MySQL connection details
  from `application.properties`. No Flyway — Hibernate manages the schema.
- **Bootstrapping**: `WebAppInitializer` implements Spring's
  `WebApplicationInitializer`, registering `ContextLoaderListener` (with an
  `AnnotationConfigWebApplicationContext` over `AppConfig` +
  `PersistenceConfig`) and `RequestContextListener`. This is the one config
  class with real wiring code, since without it Spring never starts.
- **Packaging**: WAR, deployed to WildFly. Not Spring Boot.
- **Package layout**: flat, one package per architectural layer under
  `org.joel.kimwanyisacco` — `model`, `dto`, `repository`, `service`,
  `policy`, `controller` — mirroring a classic MVC solution layout (all
  domains' classes live together in the same layer package; class names
  stay unique so there's no collision). `config` and `common`
  (`exception`, `util`) remain their own small packages since they aren't
  domain layers.

## Scope

Every file is a **stub**:

- Java classes/interfaces/enums compile and express their structural role
  (fields, method signatures) but contain no business logic — except
  `WebAppInitializer` and `PersistenceConfig`, which need real (if
  minimal/placeholder-valued) wiring code so Spring can actually start.
- Web/config/resource files are minimally valid, not empty-and-broken.
- No binary placeholder assets (diagram PNGs, logo.png) are created.

This is a structural scaffold, not a working application. Nothing here talks
to a real database or renders real functionality yet.

## Package structure (as built)

```
org.joel.kimwanyisacco/
├── config/            AppConfig, PersistenceConfig, WebAppInitializer
├── common/
│   ├── exception/      BusinessException, ResourceNotFoundException,
│   │                    DuplicateMemberException, InsufficientBalanceException,
│   │                    LoanNotEligibleException
│   └── util/            FacesMessageUtil, MembershipNumberGenerator, MoneyUtil
├── model/              BaseEntity + every JPA entity/enum across all domains
│                        (Role, UserAccount, UserRole, Member, MemberStatus,
│                        SavingsAccount, SavingsTransaction, TransactionType,
│                        Loan, LoanRepayment, LoanStatus)
├── dto/                Every DTO/form across all domains
├── repository/         Every Spring Data JpaRepository interface (no Impl)
├── service/            Every service interface + Impl across all domains
├── policy/             WithdrawalPolicy, SavingsInterestCalculator,
│                        LoanEligibilityPolicy, LoanInterestCalculator
└── controller/         Every JSF backing bean across all domains
```

`loan`, `statement`, and `dashboard` layer classes are bare package+class
shells (no fields/annotations) per a later scope-reduction request; `config`,
`common`, `authentication`→`model`/`dto`/`repository`/`service`/`controller`,
`member`, and `savings` classes carry full fields/annotations/constructor
injection.

Webapp (`src/main/webapp/`), resources (`application.properties`,
`logback.xml`), and tests (`src/test/`) are unchanged in shape from a
standard Maven WAR layout — see the file tree via `find src docs` for the
authoritative current state, since this doc is a design record rather than
a live index.

## Stub conventions

| File kind | Convention |
|---|---|
| Entities/DTOs/forms | Fields + getters/setters, no logic |
| Enums | Constants only, reasonable defaults inferred from domain (e.g. `MemberStatus { ACTIVE, INACTIVE, SUSPENDED, CLOSED }`) |
| Repository interfaces | `extends JpaRepository<Entity, Long>` + domain-specific finder method signatures (e.g. `findByMembershipNumber`). No `*RepositoryImpl` classes. |
| Service interfaces | Method signatures inferred from the entities/DTOs in the same domain |
| Service/policy `*Impl` classes | `@Service`/`@Component` Spring bean; every method body is `throw new UnsupportedOperationException("not implemented");` |
| JSF backing beans (`*Bean` controllers) | Plain class, constructor-injected service, `@Component("beanName")` + `@RequestScope`/`@SessionScope` (resolved into JSF EL via `SpringBeanFacesELResolver`), empty action methods returning `null` |
| Config classes | `@Configuration` class shell, no logic — except `WebAppInitializer` (Spring bootstrap) and `PersistenceConfig` (real, non-throwing `DataSource`/`EntityManagerFactory`/`JpaTransactionManager` beans, since Spring context beans must not throw during startup) |
| `.xhtml` pages | Minimal valid Facelets markup (`<ui:composition template="...">`) |
| `.xhtml` templates/fragments | Bare valid Facelets/HTML skeleton |
| `application.properties`, `logback.xml` | Minimal valid placeholder config |
| Test classes | `@Disabled("Scaffold only - implementation pending")` on otherwise-empty test classes, so `mvn test` passes cleanly instead of deliberately failing |
| `README.md`, `docs/setup-guide.md`, `docs/database-schema.html` | Minimal placeholder text/markup |

## pom.xml

- `jakarta.jakartaee-api` (`provided`) — JSF/JPA annotation API types,
  supplied by WildFly at runtime.
- `hibernate-core` (`provided`) — WildFly's native JPA provider; not
  bundled in the WAR to avoid classloader conflicts.
- `primefaces` (compile) — bundled in the WAR.
- Spring: `spring-context`, `spring-web`, `spring-orm`, `spring-tx`,
  `spring-data-jpa` (compile) — bundled in the WAR, not provided by WildFly.
- `mysql-connector-j` (compile) — JDBC driver, bundled in the WAR.
- `slf4j-api` + `logback-classic` (compile).
- JUnit 5 (test).
- No Flyway (removed — Hibernate manages schema). No Jersey/JAX-RS (no REST
  layer in this architecture).
- `maven.compiler.source`/`target` = 17 (required by Jakarta EE 11 and
  Spring 6).

## webapp restructure

- Deleted `index.jsp` (replaced by `index.xhtml`), `HelloServlet.java`, and
  `META-INF/beans.xml` (CDI marker, no longer used).
- `web.xml`: Faces Servlet (`*.xhtml` mapping) + welcome file only.
  `ContextLoaderListener`/`RequestContextListener` registration happens
  programmatically in `WebAppInitializer.java` (Spring auto-discovers it via
  the Servlet 3.0+ `ServletContainerInitializer` SPI).
- `faces-config.xml`: registers `SpringBeanFacesELResolver`.
- CSS/JS left as empty placeholders — front-end styling (Tailwind) is being
  built separately by the user, not part of this scaffold.

## Out of scope

- Any real business logic, persistence behavior, authentication, or
  validation.
- Binary image assets (`logo.png`, diagram `.png` files).
- CI/build pipeline changes.
- Tailwind/front-end build tooling — deferred to the user.

## Testing

N/A for this scaffold — test classes are `@Disabled` stubs. `mvn compile`,
`mvn test-compile`, and `mvn test` all succeed (verified).
