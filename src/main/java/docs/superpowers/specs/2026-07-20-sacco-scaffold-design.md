# Kimwanyi SACCO Management System — Project Scaffold Design

Date: 2026-07-20
Revised: 2026-07-20 (switched from CDI to Spring Framework for DI/services)

## Purpose

Scaffold the full directory/package/file structure for the SACCO Management
System as specified by the user, without implementing business logic. This
establishes the architectural skeleton — JSF (Jakarta Faces) + PrimeFaces for
views, Spring Framework for dependency injection and services, Spring Data
JPA + Hibernate for persistence, layered by domain module: authentication,
member, savings, loan, statement, dashboard — so implementation can proceed
module by module afterward.

## Architecture (revised)

- **View layer**: JSF (Jakarta Faces) with PrimeFaces components. `.xhtml`
  pages/templates/fragments as originally specified.
- **DI/service layer**: Spring Framework (not CDI/Weld). Services and
  policies are `@Service`/`@Component` Spring beans. JSF backing beans are
  plain classes registered as Spring beans (`@Component`, scoped with
  `@Scope("request")` or `@Scope("session")`) and exposed to JSF EL via
  `org.springframework.web.jsf.el.SpringBeanFacesELResolver` — no
  `@Named`/`@Inject`/CDI scopes anywhere in the codebase.
- **Persistence**: Spring Data JPA repository interfaces
  (`extends JpaRepository<Entity, Long>`) with Hibernate as the JPA
  provider. No hand-written `*RepositoryImpl.java` classes — Spring
  generates the implementation, so those files are dropped entirely from
  the original tree.
- **Bootstrapping**: `WebAppInitializer` implements Spring's
  `org.springframework.web.WebApplicationInitializer`, registering
  `ContextLoaderListener` (with an `AnnotationConfigWebApplicationContext`
  pointed at `AppConfig`) and `RequestContextListener` (required for
  request/session-scoped beans to resolve outside of a `DispatcherServlet`
  request, since JSF's `FacesServlet` — not Spring MVC — handles requests
  here). This is the one config class with real wiring code, since without
  it Spring never starts.
- **Packaging**: WAR, deployed to an external Jakarta EE-compatible app
  server (matches the existing `packaging=war` + `jakarta.jakartaee-api`
  `provided`-scope setup already in the repo). Not Spring Boot.

## Scope

Create every file/folder in the target tree below as a **stub**:

- Java classes/interfaces/enums compile and express their structural role
  (fields, method signatures) but contain no business logic.
- Web/config/resource files are minimally valid, not empty-and-broken.
- No binary placeholder assets (diagram PNGs, logo.png) are created.

This is a structural scaffold, not a working application. Nothing here talks
to a real database or renders real functionality yet.

## Target tree

```
Kimwanyi-SACCO-Management-System/
│
├── pom.xml
├── mvnw / mvnw.cmd
├── README.md
├── docs/
│   ├── database-schema.html
│   └── setup-guide.md
│       (use-case-diagram.png / class-diagram.png intentionally skipped — no
│        meaningful blank-image placeholder)
│
└── src/
    ├── main/
    │   ├── java/org/joel/kimwanyisacco/
    │   │   ├── config/
    │   │   │   ├── AppConfig.java
    │   │   │   ├── PersistenceConfig.java
    │   │   │   ├── TransactionConfig.java
    │   │   │   └── WebAppInitializer.java
    │   │   ├── common/
    │   │   │   ├── exception/ (BusinessException, ResourceNotFoundException,
    │   │   │   │   DuplicateMemberException, InsufficientBalanceException,
    │   │   │   │   LoanNotEligibleException)
    │   │   │   ├── util/ (FacesMessageUtil, MembershipNumberGenerator, MoneyUtil)
    │   │   │   └── model/BaseEntity.java
    │   │   ├── authentication/ (model, dto, repository [JpaRepository, no Impl], service, controller)
    │   │   ├── member/ (model, dto, repository [JpaRepository, no Impl], service, controller)
    │   │   ├── savings/ (model, dto, repository [JpaRepository, no Impl], service, policy, controller)
    │   │   ├── loan/ (model, dto, repository [JpaRepository, no Impl], service, policy, controller)
    │   │   ├── statement/ (dto, service, controller)
    │   │   └── dashboard/ (dto, service, controller)
    │   ├── resources/
    │   │   ├── application.properties
    │   │   ├── logback.xml
    │   │   ├── META-INF/persistence.xml
    │   │   └── db/migration/V1..V4__*.sql
    │   └── webapp/
    │       ├── index.xhtml, login.xhtml, access-denied.xhtml, error.xhtml
    │       ├── members/, savings/, loans/, statements/, admin/
    │       ├── resources/css/main.css, resources/js/main.js
    │       └── WEB-INF/
    │           ├── web.xml, faces-config.xml
    │           ├── templates/ (main-template.xhtml, login-template.xhtml)
    │           └── fragments/ (header, sidebar, footer, messages).xhtml
    └── test/
        ├── java/org/joel/kimwanyisacco/
        │   ├── member/MemberServiceTest.java
        │   ├── savings/SavingsServiceTest.java, WithdrawalPolicyTest.java
        │   ├── loan/LoanServiceTest.java, LoanEligibilityPolicyTest.java
        │   └── repository/MemberRepositoryIntegrationTest.java
        └── resources/application-test.properties
```

(Full file list matches exactly what the user provided in their message —
this doc doesn't re-enumerate every filename to avoid duplication, the
implementation plan will.)

## Stub conventions

| File kind | Convention |
|---|---|
| Entities/DTOs/forms | Fields + getters/setters, no logic |
| Enums | Constants only, reasonable defaults inferred from domain (e.g. `MemberStatus { ACTIVE, INACTIVE, SUSPENDED, CLOSED }`) |
| Repository interfaces | `extends JpaRepository<Entity, Long>` + domain-specific finder method signatures (e.g. `findByMembershipNumber`). No `*RepositoryImpl` classes — Spring Data JPA generates the implementation. |
| Service interfaces | Method signatures inferred from the entities/DTOs in the same package |
| Service/policy `*Impl` classes | `@Service`/`@Component` Spring bean; every method body is `throw new UnsupportedOperationException("not implemented");` |
| JSF backing beans (`*Bean` controllers) | Plain class, `@Component` + `@Scope("request")`/`@Scope("session")` (resolved into JSF EL via `SpringBeanFacesELResolver` — no `@Named`/`@Inject`), empty action methods returning `null` |
| Config classes | `@Configuration` class shell with placeholder Spring/JPA annotations, no logic — except `WebAppInitializer`, which needs real `ContextLoaderListener`/`RequestContextListener` registration to bootstrap Spring at all |
| `.xhtml` pages | Minimal valid Facelets markup (`<ui:composition template="...">` referencing the shared template) |
| `.xhtml` templates/fragments | Bare valid Facelets/HTML skeleton |
| SQL migrations | Comment header only (e.g. `-- V1__create_user_tables.sql`), no DDL |
| `application.properties`, `logback.xml` | Minimal valid placeholder config, no environment-specific values |
| Test classes | `@Test` stub methods calling `Assertions.fail("not implemented")`, matching given file/method intent |
| `README.md`, `docs/setup-guide.md`, `docs/database-schema.html` | Minimal placeholder text/markup (heading + one-line description) |

## pom.xml changes

- Keep `jakarta.jakartaee-api` (`provided`) — still needed at compile time
  for the `jakarta.faces.*` (JSF) and `jakarta.persistence.*` (JPA
  annotation) API types; an app server supplies the runtime.
- Remove `jersey-container-servlet`, `jersey-media-json-jackson`,
  `jersey-hk2` — no JAX-RS/REST layer exists in the target architecture.
- Add `org.primefaces:primefaces:13.0.9` (Jakarta-namespace compatible).
- Add Spring: `org.springframework:spring-context:6.1.14`,
  `spring-web:6.1.14`, `spring-orm:6.1.14`, `spring-tx:6.1.14`.
- Add `org.springframework.data:spring-data-jpa:3.3.5`.
- Add `org.hibernate.orm:hibernate-core:6.6.2.Final` (JPA provider).
- Add `org.flywaydb:flyway-core` (for `db/migration`).
- Add `org.slf4j:slf4j-api` + `ch.qos.logback:logback-classic` (for
  `logback.xml`).
- Keep JUnit 5 (`junit-jupiter-api`/`-engine`), already present.

## webapp restructure

- Delete `index.jsp` (replaced by `index.xhtml`).
- Delete `HelloServlet.java` (not part of the target package tree).
- Delete `META-INF/beans.xml` (CDI marker file — no longer used).
- Rewrite `web.xml` to register the Faces Servlet (`*.xhtml` mapping) and
  welcome file. `ContextLoaderListener`/`RequestContextListener`
  registration happens programmatically in `WebAppInitializer.java`
  (Spring auto-discovers it via the Servlet 3.0+ `ServletContainerInitializer`
  SPI, no `web.xml` entry needed for it), so `web.xml` stays JSF-only.
- Add `WEB-INF/faces-config.xml` registering
  `org.springframework.web.jsf.el.SpringBeanFacesELResolver` as the
  `<el-resolver>`.

## Out of scope

- Any real business logic, persistence behavior, authentication, or
  validation.
- Binary image assets (`logo.png`, diagram `.png` files).
- CI/build pipeline changes.
- Database connection configuration beyond placeholder properties.

## Testing

N/A for this scaffold — test classes themselves are stubs (`fail("not
implemented")`), asserting nothing yet. `mvn compile` and `mvn test-compile`
must succeed (compilation is the only success criterion at this stage).
