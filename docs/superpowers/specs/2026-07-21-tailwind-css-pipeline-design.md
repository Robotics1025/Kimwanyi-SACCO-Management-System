# Tailwind CSS Build Pipeline — Design

Date: 2026-07-21

## Purpose

Set up Tailwind CSS v4 as the styling foundation for the SACCO Management
System's UI, replacing PrimeFaces's default component theme going forward.
No JSF views are being restyled yet — no custom components have been coded
yet. This spec covers only the build pipeline: getting a compiled Tailwind
stylesheet produced by the Maven build and linked from the app's template,
so future page/component work has a working `app.css` to build against.

## Non-goals

- Restyling any existing `.xhtml` view or overriding PrimeFaces component
  CSS. That happens later, page by page, once components exist.
- Dark mode toggle wiring. The `.dark` class variant is included in the
  theme tokens (carried over from the pasted source) but nothing in the app
  sets `.dark` on `<html>`/`<body>` yet.
- Font loading (Inter / Noto Serif Georgian / JetBrains Mono). The theme
  references these font-family tokens; actual `@font-face`/Google Fonts
  wiring is out of scope and falls back to system fonts until added.

## Architecture

- **Tailwind CLI**: standalone binary (no Node.js/npm, no `package.json`,
  no `node_modules`). Downloaded once per developer machine via a setup
  script, not committed to git.
- **Build integration**: `exec-maven-plugin`, bound to the
  `generate-resources` phase, invokes the CLI to compile the stylesheet.
  Runs automatically on every `mvn compile`/`package`/`install`.
- **Source of truth**: `src/main/webapp/resources/css/app.css` — Tailwind
  v4 CSS-first config (`@import`, `@theme inline`, `@custom-variant`, CSS
  custom properties). Committed to git.
- **Compiled output**: `src/main/webapp/resources/css/app.generated.css` —
  build artifact, gitignored, regenerated every build.
- **Template wiring**: `main-template.xhtml` links the compiled stylesheet
  via `<h:outputStylesheet library="css" name="app.generated.css"/>`.

## Components

### `bin/setup-tailwind.sh`

One-time, per-developer-machine script:
- Detects OS (`Linux`/`Darwin`) and arch (`x64`/`arm64`).
- Downloads the matching `tailwindcss-<platform>-<arch>` standalone binary
  from the official Tailwind CSS GitHub releases into `bin/tailwindcss`.
- `chmod +x` on the downloaded binary.
- Idempotent: skips the download if `bin/tailwindcss` already exists and is
  executable.

`bin/tailwindcss` (the binary itself) is gitignored.

### `src/main/webapp/resources/css/app.css`

The Tailwind source file. Content is the theme provided by the user,
verbatim, with one addition — an explicit `@source` directive so Tailwind's
class scanner covers `.xhtml` files, since that extension isn't in its
default auto-detected list:

```css
@import "tailwindcss";
@source "../../../../**/*.xhtml";

@custom-variant dark (&:is(.dark *));

:root {
  /* ... pasted oklch tokens, unchanged ... */
}

.dark {
  /* ... pasted oklch tokens, unchanged ... */
}

@theme inline {
  /* ... pasted token mappings, unchanged ... */
}

@layer base {
  * {
    @apply border-border outline-ring/50;
  }
  body {
    @apply bg-background text-foreground;
  }
}
```

The `@source` path is relative to `app.css`'s location
(`src/main/webapp/resources/css/`) and needs to resolve up to the project
root to reach `src/main/webapp/**/*.xhtml` (views, `WEB-INF/templates`,
`WEB-INF/fragments`).

### `pom.xml` — `exec-maven-plugin`

New plugin execution:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>tailwind-build</id>
            <phase>generate-resources</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>${project.basedir}/bin/tailwindcss</executable>
                <arguments>
                    <argument>-i</argument>
                    <argument>${project.basedir}/src/main/webapp/resources/css/app.css</argument>
                    <argument>-o</argument>
                    <argument>${project.basedir}/src/main/webapp/resources/css/app.generated.css</argument>
                    <argument>--minify</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

If `bin/tailwindcss` is missing or not executable, this fails the build
with Maven's standard "cannot run program" error — the fix (run
`./bin/setup-tailwind.sh`) is a one-line follow-up, not silently swallowed.

### `.gitignore`

Add:
```
bin/tailwindcss
src/main/webapp/resources/css/app.generated.css
```

### `main-template.xhtml`

Add to `<h:head>`:
```xml
<h:outputStylesheet library="css" name="app.generated.css"/>
```

## Error handling

- Missing binary: Maven build fails at `generate-resources` with a clear
  "Cannot run program" error naming the missing executable path.
- Malformed `app.css`: Tailwind CLI exits non-zero, Maven build fails with
  the CLI's own syntax error output surfaced in the Maven log.
- No `.xhtml` content matching Tailwind classes yet (no components built):
  expected and harmless — the compiled CSS just contains the base
  layer/tokens with no utility classes generated, which is correct for an
  empty-content state.

## Testing / verification

- Run `./bin/setup-tailwind.sh`, confirm `bin/tailwindcss --version` works.
- Run `mvn generate-resources`, confirm
  `src/main/webapp/resources/css/app.generated.css` is created and
  non-empty.
- Run `mvn package`, confirm the generated CSS is present inside the built
  WAR at `resources/css/app.generated.css`.
- Load the app in a browser, confirm the stylesheet `<link>` tag is present
  in the rendered HTML `<head>` and returns 200.
