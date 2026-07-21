# Shared Utils + Authentication & Access Control — Design

Date: 2026-07-21
Status: Approved

## Context

The Kimwanyi SACCO backend is scaffolded (interfaces + `UnsupportedOperationException`
stubs) across ~8 largely independent subsystems. This is the first sub-project in
building it out, chosen because everything downstream (Members, Savings, Loans,
Statements, Dashboard, Admin) depends on being able to hash/verify passwords, format
money, generate membership numbers, and gate page access by login state and role.

Scope is Java only. `.xhtml` templates/views are explicitly out of scope and stay
untouched; JSF managed-bean controllers (`LoginBean`, `UserSessionBean`) are in scope
since they're Java.

## Components

### 1. Shared Utils (`common/util/`)

- `MoneyUtil.add(BigDecimal a, BigDecimal b)` / `subtract(...)` — null-safe `BigDecimal`
  arithmetic. Throw `IllegalArgumentException` if either argument is null. No implicit
  scale/rounding beyond what `BigDecimal` does natively.
- `MoneyUtil.format(BigDecimal amount)` — returns a currency-formatted string, e.g.
  `KES 1,250.00`. Throws `IllegalArgumentException` on null.
- `MembershipNumberGenerator` — becomes a Spring-managed `@Component` (no longer static)
  because it needs `MemberRepository` to avoid collisions. `generate()` produces
  `KIM-YYYY-NNNN`:
  - `YYYY` = current year (`LocalDate.now().getYear()`)
  - `NNNN` = zero-padded 4-digit sequence, starting from count of existing members whose
    `membershipNumber` starts with `KIM-YYYY-` + 1
  - On collision (`MemberRepository.existsByMembershipNumber`), increment and retry
  - This replaces the ad-hoc `"KIM-" + System.currentTimeMillis()` logic currently
    inline in `MemberServiceImpl.generateMembershipNumber()` — that private method is
    deleted and `MemberServiceImpl` gets `MembershipNumberGenerator` injected instead.
- `FacesMessageUtil.addInfoMessage(String)` / `addErrorMessage(String)` — stay static;
  wrap `FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, message, message))`.

### 2. AuthenticationServiceImpl

- `authenticate(LoginForm form)`:
  1. Look up `UserAccount` via `UserAccountRepository.findByUsername(form.getUsername())`.
  2. If absent, or `!enabled`, or `passwordEncoder.matches(form.getPassword(), account.getPasswordHash())`
     is false → throw `AuthenticationException("Invalid username or password")`. Same
     message for "no such user" and "wrong password" to avoid username enumeration.
  3. On success, map to `LoggedInUserDto` (id, username, `roles` = singleton list
     containing `role.name()`).
- New unchecked exception `org.joel.kimwanyisacco.common.exception.AuthenticationException`.

### 3. Access control Filter

- New `org.joel.kimwanyisacco.config.SecurityFilter implements jakarta.servlet.Filter`,
  registered in `web.xml`, mapped to `*.xhtml`.
- Whitelisted (no auth required): `login.xhtml`, `index.xhtml`, `error.xhtml`,
  `access-denied.xhtml`, PrimeFaces resource requests (`/jakarta.faces.resource/*`).
- `/admin/*` requires a logged-in user with `Role.ADMIN`.
- `/members/*`, `/loans/*`, `/savings/*`, `/statements/*` require any logged-in user.
- Reads the `UserSessionBean` Spring `@SessionScope` bean from the `HttpSession`
  attribute (Spring stores session-scoped beans as session attributes under a
  `scopedTarget.<beanName>`-derived key; filter resolves it via
  `WebApplicationContextUtils.getWebApplicationContext(...).getBean(UserSessionBean.class)`
  bound to the current `HttpServletRequest`, which is the standard way to reach a
  session-scoped Spring bean from a raw servlet Filter).
- Not logged in → redirect to `login.xhtml`.
- Logged in but wrong role → redirect to `access-denied.xhtml`.

### 4. LoginBean / UserSessionBean wiring

- `LoginBean.login()`:
  - Calls `authenticationService.authenticate(loginForm)`.
  - On success: sets `userSessionBean.setLoggedInUser(dto)`, returns
    `dto.getRoles().contains("ADMIN") ? "/admin/dashboard?faces-redirect=true" : "/index?faces-redirect=true"`.
  - On `AuthenticationException`: calls `FacesMessageUtil.addErrorMessage(e.getMessage())`,
    returns `null` (redisplay login page with the message).
- `LoginBean.logout()`: invalidates the session via
  `FacesContext.getCurrentInstance().getExternalContext().invalidateSession()`, returns
  `"/login?faces-redirect=true"`.
- `UserSessionBean` unchanged (already complete).

## Error handling

- `AuthenticationException extends RuntimeException` — bad credentials or disabled
  account. Caught only in `LoginBean`.
- `MoneyUtil` throws `IllegalArgumentException` for null inputs — fail fast, no silent
  zero-defaulting, since silently treating a null amount as zero could mask a bug
  upstream in a financial calculation.

## Testing

- `MoneyUtilTest` — add/subtract/format incl. negative amounts and null-argument
  rejection.
- `MembershipNumberGeneratorTest` — first member of a year, collision increments
  sequence, year rollover resets sequence; `MemberRepository` mocked.
- `AuthenticationServiceImplTest` — success, unknown username, wrong password, disabled
  account; `UserAccountRepository` and `PasswordEncoder` mocked.
- `SecurityFilterTest` — whitelisted path passes through unauthenticated;
  protected path redirects to login when no session user; protected admin path
  redirects to access-denied for a MEMBER role; allowed through for correct role.
  Uses mocked `HttpServletRequest`/`HttpServletResponse`/`FilterChain`, not a full
  servlet container.

## Out of scope (deferred to later sub-projects)

- Finishing `MemberServiceImpl` beyond wiring in `MembershipNumberGenerator`
  (that's the "finish Member Management" sub-project).
- Savings, Loans, Statements, Dashboard, Admin backend logic.
- Any `.xhtml` changes.
