# Shared Utils + Authentication & Access Control Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `MoneyUtil`, `FacesMessageUtil`, `MembershipNumberGenerator`, `AuthenticationServiceImpl`, a page-level `SecurityFilter`, and `LoginBean`/`UserSessionBean` wiring, per `docs/superpowers/specs/2026-07-21-shared-utils-and-authentication-design.md`.

**Architecture:** Thin utility classes under `common/util/`, a Spring `@Service` for authentication backed by `UserAccountRepository` + the existing `BCryptPasswordEncoder`, a raw `jakarta.servlet.Filter` that reads the Spring session-scoped `UserSessionBean` (resolvable because `RequestContextListener` is already registered in `WebAppInitializer`), and JSF managed beans wired to that service.

**Tech Stack:** Java 17, Spring Framework 6 (context/web/orm/tx), Spring Data JPA, JSF (MyFaces) managed beans, JUnit 5.13.2, Mockito 5.23.0 (new test dependency).

## Global Constraints

- Do **NOT** run `git commit` or `git add` for any step in this plan — leave all changes in the working tree uncommitted. The user will review and commit manually. Skip any step that would otherwise say "Commit".
- `.xhtml` files are out of scope — do not modify any file under `src/main/webapp/**.xhtml`.
- Currency code is fixed as `"KES"` in `MoneyUtil.format`.
- Membership number format is `KIM-YYYY-NNNN` (4-digit zero-padded sequence, reset each calendar year).
- Both roles are `Role.ADMIN` and `Role.MEMBER` (see `org.joel.kimwanyisacco.model.enums.Role`) — no other roles exist.
- `AuthenticationException` must give the same message for "unknown user" and "wrong password" (no username enumeration).

---

### Task 1: `MoneyUtil`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/common/util/MoneyUtil.java`
- Test: `src/test/java/org/joel/kimwanyisacco/common/util/MoneyUtilTest.java`

**Interfaces:**
- Produces: `MoneyUtil.add(BigDecimal, BigDecimal): BigDecimal`, `MoneyUtil.subtract(BigDecimal, BigDecimal): BigDecimal`, `MoneyUtil.format(BigDecimal): String`. All three throw `IllegalArgumentException` if any argument is null.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/common/util/MoneyUtilTest.java`:

```java
package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void addsTwoPositiveAmounts() {
        assertEquals(
                new BigDecimal("300.00"),
                MoneyUtil.add(new BigDecimal("100.00"), new BigDecimal("200.00"))
        );
    }

    @Test
    void addThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.add(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.add(BigDecimal.TEN, null));
    }

    @Test
    void subtractsAmountsAllowingNegativeResult() {
        assertEquals(
                new BigDecimal("-50.00"),
                MoneyUtil.subtract(new BigDecimal("100.00"), new BigDecimal("150.00"))
        );
    }

    @Test
    void subtractThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.subtract(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.subtract(BigDecimal.TEN, null));
    }

    @Test
    void formatsAmountWithCurrencyCodeAndThousandsSeparator() {
        assertEquals("KES 1,250.00", MoneyUtil.format(new BigDecimal("1250.00")));
    }

    @Test
    void formatThrowsOnNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.format(null));
    }
}
```

Remove the `@Disabled` scaffold if one exists at this path (there is none for `MoneyUtilTest` yet — this is a new file).

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=MoneyUtilTest`
Expected: FAIL (compilation error or `UnsupportedOperationException`, since `MoneyUtil` methods currently throw).

- [ ] **Step 3: Implement `MoneyUtil`**

Replace the contents of `src/main/java/org/joel/kimwanyisacco/common/util/MoneyUtil.java`:

```java
package org.joel.kimwanyisacco.common.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public final class MoneyUtil {

    private static final String CURRENCY_CODE = "KES";
    private static final String DECIMAL_PATTERN = "#,##0.00";

    private MoneyUtil() {
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        requireNonNull(a, b);
        return a.add(b);
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        requireNonNull(a, b);
        return a.subtract(b);
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        DecimalFormat decimalFormat = new DecimalFormat(DECIMAL_PATTERN);
        return CURRENCY_CODE + " " + decimalFormat.format(amount);
    }

    private static void requireNonNull(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("arguments must not be null");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=MoneyUtilTest`
Expected: PASS (5 tests, 0 failures)

---

### Task 2: Add Mockito test dependency + `FacesMessageUtil`

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/org/joel/kimwanyisacco/common/util/FacesMessageUtil.java`
- Test: `src/test/java/org/joel/kimwanyisacco/common/util/FacesMessageUtilTest.java`

**Interfaces:**
- Produces: `FacesMessageUtil.addInfoMessage(String): void`, `FacesMessageUtil.addErrorMessage(String): void` — both add a `FacesMessage` to `FacesContext.getCurrentInstance()` with `null` client id.

- [ ] **Step 1: Add Mockito dependencies to `pom.xml`**

In `pom.xml`, inside the `<dependencies>` block, immediately after the existing `junit-jupiter-engine` dependency (currently the last `<dependency>` before `</dependencies>`), add:

```xml
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.23.0</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>5.23.0</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/common/util/FacesMessageUtilTest.java`:

```java
package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class FacesMessageUtilTest {

    @Test
    void addInfoMessageAddsInfoSeverityMessage() {
        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            FacesMessageUtil.addInfoMessage("Saved successfully");

            ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), captor.capture());
            assertEquals(FacesMessage.SEVERITY_INFO, captor.getValue().getSeverity());
            assertEquals("Saved successfully", captor.getValue().getSummary());
        }
    }

    @Test
    void addErrorMessageAddsErrorSeverityMessage() {
        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            FacesMessageUtil.addErrorMessage("Something went wrong");

            ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), captor.capture());
            assertEquals(FacesMessage.SEVERITY_ERROR, captor.getValue().getSeverity());
            assertEquals("Something went wrong", captor.getValue().getSummary());
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw test -Dtest=FacesMessageUtilTest`
Expected: FAIL (`UnsupportedOperationException`, since `FacesMessageUtil` methods currently throw).

- [ ] **Step 4: Implement `FacesMessageUtil`**

Replace the contents of `src/main/java/org/joel/kimwanyisacco/common/util/FacesMessageUtil.java`:

```java
package org.joel.kimwanyisacco.common.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public final class FacesMessageUtil {

    private FacesMessageUtil() {
    }

    public static void addInfoMessage(String message) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, message, message));
    }

    public static void addErrorMessage(String message) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=FacesMessageUtilTest`
Expected: PASS (2 tests, 0 failures)

---

### Task 3: `AuthenticationException` + `AuthenticationServiceImpl`

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/common/exception/AuthenticationException.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/service/AuthenticationServiceImpl.java`
- Test: `src/test/java/org/joel/kimwanyisacco/service/AuthenticationServiceImplTest.java`

**Interfaces:**
- Consumes: `UserAccountRepository.findByUsername(String): Optional<UserAccount>` (existing), `PasswordEncoder.matches(CharSequence, String): boolean` (existing Spring bean), `UserAccount.isEnabled()/getPasswordHash()/getId()/getUsername()/getRole()` (existing), `LoginForm.getUsername()/getPassword()` (existing), `LoggedInUserDto.setId/setUsername/setRoles` (existing).
- Produces: `AuthenticationException(String message)` — unchecked. `AuthenticationServiceImpl.authenticate(LoginForm): LoggedInUserDto`, throwing `AuthenticationException` on any failure (unknown user, wrong password, disabled account) with message `"Invalid username or password"`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/service/AuthenticationServiceImplTest.java`:

```java
package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(userAccountRepository, passwordEncoder);
    }

    private UserAccount buildEnabledAccount() {
        UserAccount account = new UserAccount();
        account.setUsername("jmugole");
        account.setPasswordHash("hashed-password");
        account.setEmail("joel@example.com");
        account.setFirstName("Joel");
        account.setLastName("Mugole");
        account.setRole(Role.MEMBER);
        account.setEnabled(true);
        return account;
    }

    private LoginForm formFor(String username, String password) {
        LoginForm form = new LoginForm();
        form.setUsername(username);
        form.setPassword(password);
        return form;
    }

    @Test
    void authenticateReturnsLoggedInUserForValidCredentials() {
        UserAccount account = buildEnabledAccount();
        LoginForm form = formFor("jmugole", "correct-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);

        LoggedInUserDto dto = authenticationService.authenticate(form);

        assertEquals("jmugole", dto.getUsername());
        assertEquals(List.of("MEMBER"), dto.getRoles());
    }

    @Test
    void authenticateThrowsWhenUsernameNotFound() {
        LoginForm form = formFor("unknown", "whatever");
        when(userAccountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }

    @Test
    void authenticateThrowsWhenPasswordDoesNotMatch() {
        UserAccount account = buildEnabledAccount();
        LoginForm form = formFor("jmugole", "wrong-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }

    @Test
    void authenticateThrowsWhenAccountDisabled() {
        UserAccount account = buildEnabledAccount();
        account.setEnabled(false);
        LoginForm form = formFor("jmugole", "correct-password");

        when(userAccountRepository.findByUsername("jmugole")).thenReturn(Optional.of(account));

        assertThrows(AuthenticationException.class, () -> authenticationService.authenticate(form));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=AuthenticationServiceImplTest`
Expected: FAIL (compilation error — `AuthenticationException` and the two-arg `AuthenticationServiceImpl` constructor don't exist yet).

- [ ] **Step 3: Create `AuthenticationException`**

Create `src/main/java/org/joel/kimwanyisacco/common/exception/AuthenticationException.java`:

```java
package org.joel.kimwanyisacco.common.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Implement `AuthenticationServiceImpl`**

Replace the contents of `src/main/java/org/joel/kimwanyisacco/service/AuthenticationServiceImpl.java`:

```java
package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationServiceImpl(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoggedInUserDto authenticate(LoginForm loginForm) {
        UserAccount userAccount = userAccountRepository.findByUsername(loginForm.getUsername())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS_MESSAGE));

        if (!userAccount.isEnabled()) {
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (!passwordEncoder.matches(loginForm.getPassword(), userAccount.getPasswordHash())) {
            throw new AuthenticationException(INVALID_CREDENTIALS_MESSAGE);
        }

        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccount.getId());
        dto.setUsername(userAccount.getUsername());
        dto.setRoles(List.of(userAccount.getRole().name()));
        return dto;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=AuthenticationServiceImplTest`
Expected: PASS (4 tests, 0 failures)

---

### Task 4: `MembershipNumberGenerator`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/repository/MemberRepository.java`
- Modify: `src/main/java/org/joel/kimwanyisacco/common/util/MembershipNumberGenerator.java`
- Test: `src/test/java/org/joel/kimwanyisacco/common/util/MembershipNumberGeneratorTest.java`

**Interfaces:**
- Consumes: `MemberRepository.existsByMembershipNumber(String): boolean` (existing).
- Produces: `MemberRepository.countByMembershipNumberStartingWith(String): long` (new). `MembershipNumberGenerator` becomes a Spring `@Component` with constructor `MembershipNumberGenerator(MemberRepository)` and instance method `generate(): String` returning `KIM-YYYY-NNNN`.

- [ ] **Step 1: Add the count query to `MemberRepository`**

In `src/main/java/org/joel/kimwanyisacco/repository/MemberRepository.java`, add this method inside the interface, after `countByStatus`:

```java
    long countByMembershipNumberStartingWith(String prefix);
```

- [ ] **Step 2: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/common/util/MembershipNumberGeneratorTest.java`:

```java
package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipNumberGeneratorTest {

    @Mock
    private MemberRepository memberRepository;

    @Test
    void generatesFirstMembershipNumberOfTheYear() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(0L);
        when(memberRepository.existsByMembershipNumber(prefix + "0001")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0001", generator.generate());
    }

    @Test
    void continuesSequenceFromExistingCountForTheYear() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(41L);
        when(memberRepository.existsByMembershipNumber(prefix + "0042")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0042", generator.generate());
    }

    @Test
    void incrementsPastCollisionsUntilAnUnusedNumberIsFound() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(0L);
        when(memberRepository.existsByMembershipNumber(prefix + "0001")).thenReturn(true);
        when(memberRepository.existsByMembershipNumber(prefix + "0002")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0002", generator.generate());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./mvnw test -Dtest=MembershipNumberGeneratorTest`
Expected: FAIL (compilation error — no constructor taking `MemberRepository` exists yet).

- [ ] **Step 4: Implement `MembershipNumberGenerator`**

Replace the contents of `src/main/java/org/joel/kimwanyisacco/common/util/MembershipNumberGenerator.java`:

```java
package org.joel.kimwanyisacco.common.util;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class MembershipNumberGenerator {

    private final MemberRepository memberRepository;

    public MembershipNumberGenerator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "KIM-" + year + "-";

        long sequence = memberRepository.countByMembershipNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (memberRepository.existsByMembershipNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=MembershipNumberGeneratorTest`
Expected: PASS (3 tests, 0 failures)

---

### Task 5: Wire `MembershipNumberGenerator` into `MemberServiceImpl`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java`

**Interfaces:**
- Consumes: `MembershipNumberGenerator.generate(): String` (from Task 4).
- Produces: no change to `MemberService.registerMember(MemberRegistrationForm): Member` signature.

There is no dedicated test for this step — `MemberServiceTest` is `@Disabled` (full `MemberServiceImpl` test coverage is out of scope for this sub-project per the spec). This task is verified by the full-suite run in Step 2 below.

- [ ] **Step 1: Inject `MembershipNumberGenerator` and remove the ad-hoc generator**

In `src/main/java/org/joel/kimwanyisacco/service/MemberServiceImpl.java`:

1. Add the import: `import org.joel.kimwanyisacco.common.util.MembershipNumberGenerator;`
2. Add a field `private final MembershipNumberGenerator membershipNumberGenerator;`
3. Add `MembershipNumberGenerator membershipNumberGenerator` as a constructor parameter and assign it, e.g.:

```java
    public MemberServiceImpl(
            UserAccountRepository userAccountRepository,
            MemberRepository memberRepository,
            MemberConverter memberConverter,
            PasswordEncoder passwordEncoder,
            MembershipNumberGenerator membershipNumberGenerator
    ) {
        this.userAccountRepository = userAccountRepository;
        this.memberRepository = memberRepository;
        this.memberConverter = memberConverter;
        this.passwordEncoder = passwordEncoder;
        this.membershipNumberGenerator = membershipNumberGenerator;
    }
```

4. In `registerMember`, replace the line:

```java
        String membershipNumber =
                generateMembershipNumber();
```

with:

```java
        String membershipNumber =
                membershipNumberGenerator.generate();
```

5. Delete the now-unused private method:

```java
    private String generateMembershipNumber() {
        return "KIM-" + System.currentTimeMillis();
    }
```

- [ ] **Step 2: Run the full test suite to verify nothing broke**

Run: `./mvnw test`
Expected: BUILD SUCCESS, all tests pass (the `@Disabled` `MemberServiceTest` is skipped, not failed).

---

### Task 6: `SecurityFilter`

**Files:**
- Create: `src/main/java/org/joel/kimwanyisacco/config/SecurityFilter.java`
- Modify: `src/main/webapp/WEB-INF/web.xml`
- Test: `src/test/java/org/joel/kimwanyisacco/config/SecurityFilterTest.java`

**Interfaces:**
- Consumes: `UserSessionBean.isLoggedIn(): boolean`, `UserSessionBean.getLoggedInUser(): LoggedInUserDto` (existing), `LoggedInUserDto.getRoles(): List<String>` (existing), `Role.ADMIN` (existing enum).
- Produces: `SecurityFilter implements jakarta.servlet.Filter` — registered for `*.xhtml`, redirects unauthenticated requests to `/login.xhtml` and role-mismatched requests to `/access-denied.xhtml`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/config/SecurityFilterTest.java`:

```java
package org.joel.kimwanyisacco.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.joel.kimwanyisacco.controller.UserSessionBean;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

class SecurityFilterTest {

    private final ServletContext servletContext = mock(ServletContext.class);
    private final FilterConfig filterConfig = mock(FilterConfig.class);
    private final WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
    private final UserSessionBean userSessionBean = mock(UserSessionBean.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    private SecurityFilter filter;

    @BeforeEach
    void setUp() {
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(webApplicationContext.getBean(UserSessionBean.class)).thenReturn(userSessionBean);
        when(request.getContextPath()).thenReturn("/kimwanyi-sacco");

        try (MockedStatic<WebApplicationContextUtils> mockedStatic = mockStatic(WebApplicationContextUtils.class)) {
            mockedStatic.when(() -> WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext))
                    .thenReturn(webApplicationContext);
            filter = new SecurityFilter();
            assertDoesNotThrow(() -> filter.init(filterConfig));
        }
    }

    @Test
    void allowsWhitelistedPathThroughWithoutCheckingSession() throws Exception {
        when(request.getServletPath()).thenReturn("/login.xhtml");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).sendRedirect(any());
    }

    @Test
    void redirectsToLoginWhenNotAuthenticated() throws Exception {
        when(request.getServletPath()).thenReturn("/members/list.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/kimwanyi-sacco/login.xhtml");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void redirectsToAccessDeniedWhenMemberRequestsAdminPath() throws Exception {
        when(request.getServletPath()).thenReturn("/admin/dashboard.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("MEMBER"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/kimwanyi-sacco/access-denied.xhtml");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsAdminThroughToAdminPath() throws Exception {
        when(request.getServletPath()).thenReturn("/admin/dashboard.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("ADMIN"));
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsLoggedInMemberThroughToMemberPath() throws Exception {
        when(request.getServletPath()).thenReturn("/members/list.xhtml");
        when(userSessionBean.isLoggedIn()).thenReturn(true);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=SecurityFilterTest`
Expected: FAIL (compilation error — `SecurityFilter` doesn't exist yet).

- [ ] **Step 3: Implement `SecurityFilter`**

Create `src/main/java/org/joel/kimwanyisacco/config/SecurityFilter.java`:

```java
package org.joel.kimwanyisacco.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.joel.kimwanyisacco.controller.UserSessionBean;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SecurityFilter implements Filter {

    private static final String[] PUBLIC_PATHS = {
            "/login.xhtml",
            "/index.xhtml",
            "/error.xhtml",
            "/access-denied.xhtml"
    };

    private WebApplicationContext webApplicationContext;

    @Override
    public void init(FilterConfig filterConfig) {
        webApplicationContext = WebApplicationContextUtils
                .getRequiredWebApplicationContext(filterConfig.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getServletPath();

        if (isPublicPath(path) || isFacesResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        UserSessionBean userSessionBean = webApplicationContext.getBean(UserSessionBean.class);

        if (!userSessionBean.isLoggedIn()) {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }

        if (isAdminPath(path) && !userSessionBean.getLoggedInUser().getRoles().contains(Role.ADMIN.name())) {
            response.sendRedirect(request.getContextPath() + "/access-denied.xhtml");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFacesResource(String path) {
        return path.startsWith("/jakarta.faces.resource");
    }

    private boolean isAdminPath(String path) {
        return path.startsWith("/admin/");
    }
}
```

- [ ] **Step 4: Register the filter in `web.xml`**

In `src/main/webapp/WEB-INF/web.xml`, add the following immediately after the closing `</servlet-mapping>` tag and before `<welcome-file-list>`:

```xml
    <filter>
        <filter-name>SecurityFilter</filter-name>
        <filter-class>org.joel.kimwanyisacco.config.SecurityFilter</filter-class>
    </filter>

    <filter-mapping>
        <filter-name>SecurityFilter</filter-name>
        <url-pattern>*.xhtml</url-pattern>
    </filter-mapping>
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./mvnw test -Dtest=SecurityFilterTest`
Expected: PASS (5 tests, 0 failures)

---

### Task 7: `LoginBean`

**Files:**
- Modify: `src/main/java/org/joel/kimwanyisacco/controller/LoginBean.java`
- Test: `src/test/java/org/joel/kimwanyisacco/controller/LoginBeanTest.java`

**Interfaces:**
- Consumes: `AuthenticationService.authenticate(LoginForm): LoggedInUserDto` (throws `AuthenticationException`, from Task 3), `UserSessionBean.setLoggedInUser(LoggedInUserDto): void` (existing), `FacesMessageUtil.addErrorMessage(String): void` (from Task 2).
- Produces: `LoginBean.login(): String` — returns `"/admin/dashboard?faces-redirect=true"` for an ADMIN, `"/index?faces-redirect=true"` for a MEMBER, or `null` (with an error message added) on `AuthenticationException`. `LoginBean.logout(): String` — invalidates the session, returns `"/login?faces-redirect=true"`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/org/joel/kimwanyisacco/controller/LoginBeanTest.java`:

```java
package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginBeanTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserSessionBean userSessionBean;

    private LoginForm formFor(String username, String password) {
        LoginForm form = new LoginForm();
        form.setUsername(username);
        form.setPassword(password);
        return form;
    }

    @Test
    void loginRedirectsToAdminDashboardForAdminUser() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("admin1", "secret"));
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("ADMIN"));
        when(authenticationService.authenticate(loginBean.getLoginForm())).thenReturn(dto);

        String outcome = loginBean.login();

        assertEquals("/admin/dashboard?faces-redirect=true", outcome);
        verify(userSessionBean).setLoggedInUser(dto);
    }

    @Test
    void loginRedirectsToIndexForMemberUser() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("member1", "secret"));
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setRoles(List.of("MEMBER"));
        when(authenticationService.authenticate(loginBean.getLoginForm())).thenReturn(dto);

        String outcome = loginBean.login();

        assertEquals("/index?faces-redirect=true", outcome);
        verify(userSessionBean).setLoggedInUser(dto);
    }

    @Test
    void loginReturnsNullAndAddsErrorMessageOnBadCredentials() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);
        loginBean.setLoginForm(formFor("member1", "wrong"));
        when(authenticationService.authenticate(loginBean.getLoginForm()))
                .thenThrow(new AuthenticationException("Invalid username or password"));

        FacesContext facesContext = mock(FacesContext.class);
        ExternalContext externalContext = mock(ExternalContext.class);
        when(facesContext.getExternalContext()).thenReturn(externalContext);

        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = loginBean.login();

            assertNull(outcome);
            verify(facesContext).addMessage(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void logoutInvalidatesSessionAndReturnsLoginOutcome() {
        LoginBean loginBean = new LoginBean(authenticationService, userSessionBean);

        FacesContext facesContext = mock(FacesContext.class);
        ExternalContext externalContext = mock(ExternalContext.class);
        when(facesContext.getExternalContext()).thenReturn(externalContext);

        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = loginBean.logout();

            assertEquals("/login?faces-redirect=true", outcome);
            verify(externalContext).invalidateSession();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=LoginBeanTest`
Expected: FAIL (compilation error — no two-arg `LoginBean` constructor exists yet).

- [ ] **Step 3: Implement `LoginBean`**

Replace the contents of `src/main/java/org/joel/kimwanyisacco/controller/LoginBean.java`:

```java
package org.joel.kimwanyisacco.controller;

import jakarta.faces.context.FacesContext;
import org.joel.kimwanyisacco.common.exception.AuthenticationException;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.LoginForm;
import org.joel.kimwanyisacco.service.AuthenticationService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loginBean")
@RequestScope
public class LoginBean {

    private final AuthenticationService authenticationService;
    private final UserSessionBean userSessionBean;

    private LoginForm loginForm = new LoginForm();

    public LoginBean(AuthenticationService authenticationService, UserSessionBean userSessionBean) {
        this.authenticationService = authenticationService;
        this.userSessionBean = userSessionBean;
    }

    public LoginForm getLoginForm() {
        return loginForm;
    }

    public void setLoginForm(LoginForm loginForm) {
        this.loginForm = loginForm;
    }

    public String login() {
        try {
            LoggedInUserDto loggedInUser = authenticationService.authenticate(loginForm);
            userSessionBean.setLoggedInUser(loggedInUser);
            return loggedInUser.getRoles().contains("ADMIN")
                    ? "/admin/dashboard?faces-redirect=true"
                    : "/index?faces-redirect=true";
        } catch (AuthenticationException e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/login?faces-redirect=true";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=LoginBeanTest`
Expected: PASS (4 tests, 0 failures)

- [ ] **Step 5: Run the full test suite and full compile**

Run: `./mvnw test`
Expected: BUILD SUCCESS, all tests pass across the whole module.

Run: `./mvnw compile`
Expected: BUILD SUCCESS (confirms `MemberServiceImpl`'s new constructor param and all wiring compiles cleanly).

---

## Final Verification

- [ ] Run `./mvnw test` one more time from the project root and confirm `BUILD SUCCESS` with no skipped-but-should-run tests failing.
- [ ] Leave all changes uncommitted in the working tree (per Global Constraints) for the user to review.
