# GeckoCIRCUITS Warning Policy

This document defines the project's policy on compiler warnings, static analysis
findings, and the use of `@SuppressWarnings` annotations.

**Related:** [MODERNIZATION_PLAN.md](MODERNIZATION_PLAN.md) — Phases 1.3 & 4.1

---

## Guiding Principle

> **New and modified code must be warning-free.**
> Legacy warnings from the original codebase are tolerated but are being
> removed incrementally as files are touched during normal development.

This "ratcheting" approach ensures the warning count only ever goes down,
without the risk of a large-scale rewrite.

---

## Compiler Configuration

The Maven build (`pom.xml`) compiles all Java source with:

```
<compilerArgs>
  <arg>-Xlint:all</arg>
  <arg>-proc:none</arg>
</compilerArgs>
```

`-Xlint:all` enables every javac lint category (unchecked, rawtypes,
deprecation, serial, this-escape, etc.). The build **does not** use
`-Werror`, so warnings do not fail the build — but contributors should
resolve them for any code they add or modify.

---

## Static Analysis Tools

Three analysis plugins are registered in `pom.xml` (plugin management):

| Plugin | Purpose | Maven Goal |
|--------|---------|------------|
| **SpotBugs** | Bug-pattern detection | `spotbugs:check` |
| **Checkstyle** | Code-style conformance | `checkstyle:check` |
| **PMD** | Code-quality rule violations | `pmd:check` |

These are available on demand. Run them locally with:

```bash
mvn checkstyle:check pmd:check spotbugs:check
```

---

## `@SuppressWarnings` Annotation Rules

### When suppression IS acceptable

| Category | Example | Rationale |
|----------|---------|-----------|
| **`PMD`** (broad) | `@SuppressWarnings("PMD")` | Intentional pattern that PMD flags but is correct by design (e.g., empty catch blocks in shutdown hooks). |
| **Specific PMD rules** | `@SuppressWarnings("PMD.ExcessivePublicCount")` | Remote-interface classes that expose many public methods by contract. |
| **`deprecation`** | `@SuppressWarnings("deprecation")` | Legacy public API used by external integrators (MATLAB/Simulink). Cannot be removed without breaking downstream users. |
| **`unchecked` / `rawtypes`** | `@SuppressWarnings({"unchecked", "rawtypes"})` | Legacy data structures using pre-generics collection APIs (e.g., `BigMatrix`, `Matrix`). Fixed incrementally. |
| **`serial`** | `@SuppressWarnings("serial")` | Swing components (`JDialog`, `JFrame` subclasses) where `serialVersionUID` management is not meaningful. |
| **`this-escape`** | `@SuppressWarnings("this-escape")` | Swing constructors that call overridable methods after the superclass is fully initialized. A known Swing pattern; safe in practice. |

### When suppression is NOT acceptable

- To silence a warning in **new** code that could instead be fixed.
- Broad `@SuppressWarnings("PMD")` or `@SuppressWarnings("all")` on newly
  written classes or methods.
- Suppressing warnings without a comment explaining **why**.

### Documentation requirement

Every `@SuppressWarnings` annotation in new or modified code **must** include a
trailing comment explaining the rationale. The existing codebase follows this
convention — for example:

```java
@SuppressWarnings("PMD") // here, we really want an empty catch block!
public static void shutdown() { ... }

@SuppressWarnings("this-escape")  // JDialog superclass is fully initialized,
                                  // setIconImage() call is safe
public DialogAbout() { ... }

@SuppressWarnings("PMD") // CHECKSTYLE:OFF I cannot rename this method,
                          // since it is already used by Gecko-Users!
public void oldPublicAPI() { ... }
```

---

## Legacy Warning Exemptions

The following areas contain known, accepted warnings from the original
codebase. They are **grandfathered** and will be cleaned up over time
(Phase 4.1 — Gradual Warning Removal):

### 1. Remote Interface Layer (`GeckoRemote*` classes)

**Files:** `GeckoRemote.java`, `GeckoRemoteObject.java`, `GeckoRemoteMMFObject.java`

- Broad `@SuppressWarnings("PMD")` on numerous methods (empty catch blocks
  in RMI shutdown/cleanup paths).
- `PMD.ExcessivePublicCount` — the remote interface exposes 100+ public
  methods by contract for MATLAB integration.
- `PMD.NullAssignment` — intentional null sentinel assignments.
- `deprecation` — uses legacy `java.net.URL` constructors.

### 2. Public Scripting API (`AbstractGeckoCustom`)

**File:** `AbstractGeckoCustom.java`

- `deprecation` — many methods are deprecated but retained for
  backward compatibility with user-written JavaScript scripts.
- `PMD` — naming conventions intentionally violated for methods that
  users already call by name.

### 3. Legacy Math/Data Structures

**Files:** `BigMatrix.java`, `Matrix.java`, `NativeCDialog.java`

- `unchecked` / `rawtypes` — pre-generics collection usage.

### 4. Swing Dialogs and Components

**Files:** All `Dialog*.java`, `JFrame`/`JDialog` subclasses

- `this-escape` — calling overridable Swing methods from constructors.
- `serial` — missing `serialVersionUID` on visual components.
- `deprecation` — legacy `URL` and `Date` constructors.

---

## Workflow for Contributors

### When writing new code

1. Ensure `javac` produces **zero warnings** for your new/modified files.
2. If a warning is genuinely unavoidable, add a **scoped**
   `@SuppressWarnings` with a comment explaining why.
3. Never add a class-level or blanket suppression to silence new warnings.

### When modifying existing code

1. Fix any warnings in the methods/classes you touch.
2. Leave surrounding legacy warnings alone (unless trivially fixable).
3. The net warning count should never increase.

### When reviewing a PR

- Verify no new `@SuppressWarnings("all")` or undocumented suppressions are added.
- Confirm new code compiles cleanly under `-Xlint:all`.
- Check that any new suppression includes an explanatory comment.

---

## Tracking Progress

Warning reduction is tracked as part of Phase 4.1 in the
[Modernization Plan](MODERNIZATION_PLAN.md). Major bulk cleanup was already
completed in commits `d07b6b1` and `7e8ae47`, resolving thousands of warnings
across 298+ files (type safety, `serialVersionUID`, `transient` fields, etc.).
