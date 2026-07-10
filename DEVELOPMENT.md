# GeckoCIRCUITS Development Guide

This guide is for developers who want to build, modify, and contribute to
GeckoCIRCUITS. It covers environment setup, build commands, testing,
architecture, code style, and the contribution workflow.

**Related documents:**
- [README.md](README.md) — User-facing overview and quick start
- [WARNING_POLICY.md](WARNING_POLICY.md) — Compiler warning and suppression policy
- [NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md) — Editing Swing `.form` files
- [MODERNIZATION_PLAN.md](MODERNIZATION_PLAN.md) — Ongoing modernization roadmap

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Building](#building)
- [Running the Application](#running-the-application)
- [Testing](#testing)
- [Code Quality Tools](#code-quality-tools)
- [Code Style & Conventions](#code-style--conventions)
- [Project Architecture](#project-architecture)
- [IDE Configuration](#ide-configuration)
- [CI/CD Pipeline](#cicd-pipeline)
- [Native JNI Libraries](#native-jni-libraries)
- [Internationalization (i18n)](#internationalization-i18n)
- [GUI Development with .form Files](#gui-development-with-form-files)
- [Debugging](#debugging)
- [Contribution Workflow](#contribution-workflow)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Version | Notes |
|-------------|---------|-------|
| **JDK** | 21 (LTS) | Temurin (Adoptium) recommended. Set `JAVA_HOME`. |
| **Maven** | 3.6+ | Used for all build, test, and analysis tasks. |
| **Git** | Any recent | For cloning and contributing. |
| **NetBeans** | Latest (optional) | Required only for visual editing of `.form` files. |
| **C compiler** | GCC / MinGW (optional) | Only for building native JNI test libraries. |

---

## Environment Setup

### 1. Clone the repository

```bash
git clone https://github.com/technokrat/gecko.git
cd gecko
```

### 2. Verify Java and Maven

```bash
java -version    # Must show version 21
mvn -version     # Must show 3.6+
```

### 3. Set JAVA_HOME (if not already set)

**Windows:**
1. Press `Win+R`, type `sysdm.cpl`
2. Go to "Advanced" → "Environment Variables"
3. Add `JAVA_HOME` pointing to your JDK 21 installation (e.g., `C:\Program Files\Java\jdk-21`)
4. Restart your terminal

**Linux/macOS:**
```bash
export JAVA_HOME=/path/to/jdk-21
```

### 4. Verify the build

```bash
mvn clean compile
```

If this succeeds, your environment is ready.

---

## Building

### Full build (compile + test + package)

```bash
mvn clean package assembly:single
```

This produces `target/gecko-1.0-jar-with-dependencies.jar` — a fat JAR
containing all dependencies.

### Quick build (skip tests)

```bash
mvn clean package assembly:single -DskipTests
```

### Compile only (no tests, no packaging)

```bash
mvn clean compile
```

### Clean build artifacts

```bash
mvn clean
```

---

## Running the Application

### Standard (standalone desktop mode)

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar
```

### With a circuit file

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar resources/Education_ETHZ/ex_1.ipes
```

### HiDPI displays

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar target/gecko-1.0-jar-with-dependencies.jar
```

### Remote access mode (for MATLAB/Simulink integration)

```bash
java -jar target/gecko-1.0-jar-with-dependencies.jar -p <port_number>
```

### Memory-mapped file mode

```bash
java -jar target/gecko-1.0-jar-with-dependencies.jar -mm <filename> <filesize>
```

### JVM flags explained

| Flag | Purpose |
|------|---------|
| `-Xmx3G` | Allocates 3 GB heap. Increase for large simulations (e.g., `-Xmx6G`). |
| `-Dpolyglot.js.nashorn-compat=true` | Required for GraalVM JavaScript engine (scripting support). |
| `-Dsun.java2d.uiScale=2` | Enables 2x scaling for HiDPI displays. |

---

## Testing

### Run all tests

```bash
mvn test
```

All 159 tests should pass with 0 failures and 0 skipped.

### Run a single test class

```bash
mvn test -Dtest=GeckoRemoteTest
```

### Run tests matching a pattern

```bash
mvn test -Dtest="*Calculator*"
```

### Test categories

| Category | Description | Location |
|----------|-------------|----------|
| **Unit tests** | Calculator, math function, and component tests | `src/test/java/.../circuit/`, `.../control/` |
| **Integration tests** | `ModelResultsTest` — loads and simulates real `.ipes` circuit files | `src/test/java/.../circuit/` |
| **API tests** | `GeckoRemoteTest` — verifies the `GeckoRemote` API matches `GeckoRemoteInterface` | `src/test/java/.../GeckoRemoteTest.java` |
| **Native JNI tests** | Tests that load native `.so`/`.dll`/`.dylib` libraries | `src/test/java/.../nativec/` |

### Test framework

- **JUnit 5** (Jupiter) — primary framework
- **JUnit 4 vintage engine** — included for any remaining JUnit 4 tests
- Surefire plugin configured with `trimStackTrace=false` for full error output

### Writing new tests

1. Place test classes under `src/test/java/` mirroring the main source package.
2. Use JUnit 5 annotations (`@Test`, `@BeforeEach`, `@DisplayName`).
3. Use `@Ignore` only on abstract base classes — never on concrete test classes.
4. Tests run headless on CI. On Ubuntu, Xvfb provides a virtual display for
   Swing-dependent tests.

### Test mode

The application detects test execution via `GeckoSim._isTestingMode`. This flag
is checked at various points to skip GUI initialization during automated tests.

---

## Code Quality Tools

Three static analysis plugins are registered in `pom.xml` (under
`pluginManagement`):

| Plugin | Maven Goal | Purpose |
|--------|------------|---------|
| **SpotBugs** | `mvn spotbugs:check` | Bug-pattern detection |
| **Checkstyle** | `mvn checkstyle:check` | Code-style conformance |
| **PMD** | `mvn pmd:check` | Code-quality rule violations |

Run all three:

```bash
mvn checkstyle:check pmd:check spotbugs:check
```

### Test coverage (JaCoCo)

JaCoCo is integrated and generates coverage reports during the `test` phase.
After running tests, open the report at:

```
target/site/jacoco/index.html
```

Coverage thresholds are not yet enforced as a CI gate (see
[Modernization Plan 2.2](MODERNIZATION_PLAN.md)).

---

## Code Style & Conventions

### Editor configuration

The project uses [`.editorconfig`](.editorconfig) to enforce consistent
formatting across all editors and IDEs:

| Setting | Value |
|---------|-------|
| Charset | UTF-8 |
| Line endings | LF |
| Indentation | 4 spaces (never tabs in Java) |
| Max line length (Java) | 120 characters |
| Trailing whitespace | Trimmed (except in Markdown) |
| Final newline | Inserted |

### Warning policy

The build compiles with `-Xlint:all`. The guiding principle is:

> **New and modified code must be warning-free.**

Key rules (see [WARNING_POLICY.md](WARNING_POLICY.md) for full details):

- Fix all warnings in files you add or modify.
- Never add a blanket `@SuppressWarnings("all")` to new code.
- If a warning is genuinely unavoidable, add a **scoped**
  `@SuppressWarnings` with a trailing comment explaining why.
- Legacy warnings are grandfathered and removed incrementally.

Example of correct suppression:

```java
@SuppressWarnings("this-escape")  // JDialog superclass fully initialized, safe
public DialogAbout() { ... }
```

### Language

All code, comments, variables, and user-facing strings should be in **English**.
The codebase was translated from German to English as part of the modernization
effort.

### Serialization

- Swing component subclasses (`JDialog`, `JFrame`) that are `Serializable`
  should have a `serialVersionUID`.
- Mark non-serializable fields as `transient` where appropriate.

---

## Project Architecture

### Main entry point

**`ch.technokrat.gecko.GeckoSim`** — the main class that:
- Parses command-line arguments to determine the operating mode.
- Configures memory settings (may trigger a JVM restart with more memory).
- Initializes the main application window (`Fenster`).

### Operating modes

Defined in the `OperatingMode` enum:

| Mode | Description |
|------|-------------|
| `STANDALONE` | Normal desktop application (default) |
| `REMOTE` | Remote access via Java RMI (for MATLAB integration) |
| `MMF` | Memory-mapped file communication |
| `SIMULINK` | MATLAB Simulink co-simulation |
| `EXTERNAL` | External tool integration |

### Package structure

```
ch.technokrat
├── expressionscripting/       Expression evaluation and JavaScript support
├── modelviewcontrol/          MVC framework utilities
└── gecko
    ├── GeckoSim.java          Main entry point
    ├── GeckoRemote*.java      RMI remote interface layer
    ├── GeckoCustom*.java      Custom remote/MMF objects
    ├── geckocircuits/         Core simulation engine
    │   ├── circuit/           Circuit sheet, components, terminals, couplings
    │   ├── control/           Control blocks, Java/script blocks, measurements
    │   ├── datacontainer/     Data storage and signal management
    │   ├── general/           Dialogs, file management, global settings
    │   ├── math/              Mathematical utilities (matrix, FFT, solver)
    │   ├── nativec/           Native C integration (JNI)
    │   ├── newscope/          Modern oscilloscope/visualization
    │   └── scope/             Legacy oscilloscope components
    ├── geckoscript/           JavaScript scripting engine (GraalVM)
    └── i18n/                  Internationalization
        ├── I18nKeys.java      968+ translation keys
        ├── GuiFabric.java     Factory for localized UI components
        └── EnglishMapper.java English translations
```

### Key dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| GraalVM Polyglot (JS) | 24.1.1 | JavaScript scripting engine |
| JUnit (Jupiter) | 5.11.3 | Testing framework |
| Log4j 2 | 2.24.3 | Logging |
| JTransforms | 2.4 | FFT operations |
| Apache Batik | 1.7 | SVG generation/export |
| SyntaxPane | 1.3.0 | Code editor component |
| RSyntaxTextArea | 3.3.4 | Syntax-highlighted text areas |
| JNA | 5.18.1 | Java Native Access |
| AbsoluteLayout (NetBeans) | RELEASE280 | NetBeans GUI designer layout |

Dependency versions are centralized in `<properties>` in `pom.xml` for easy
upgrades.

### File formats

- **`.ipes`** — GeckoCIRCUITS circuit files (GZIP-compressed). The primary
  format for saving and loading circuits.
- **`.form`** — NetBeans GUI Designer descriptor files (paired with `.java`
  source files). See [GUI Development](#gui-development-with-form-files).

### Property files

| File | Location | Purpose |
|------|----------|---------|
| `defaultProperties.prp` | Inside JAR (`/`) | Bundled defaults |
| `GeckoProperties.prp` | User's local app data directory | User overrides (memory, recent files, settings) |

---

## IDE Configuration

### VSCode (recommended for code editing)

The project includes pre-configured `.vscode/` settings:

- **Build tasks** (`Ctrl+Shift+B`): Build with or without tests
- **Run configurations**: Standard, HiDPI, debug, with file, with example
- **Test runner**: Run tests via the Test task

See [`.vscode/README.md`](.vscode/README.md) for the full VSCode guide.

### NetBeans (required for `.form` editing)

NetBeans includes the GUI Designer — the only reliable way to visually edit
Swing `.form` files:

1. **Open Project**: File → Open Project → select `pom.xml`
2. **Open `.form`**: Shift+double-click any `.form` file
3. **Edit visually**: Drag components, set properties in Properties panel
4. **Switch to Source**: Click `[Source]` tab for business logic
5. **Save**: `Ctrl+S`

See [NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md) for the complete guide.

### IntelliJ IDEA

1. File → Open → select the project directory (Maven auto-import)
2. Configure Project SDK to JDK 21
3. Use Maven tool window for build/test/run
4. Note: `.form` files require the NetBeans GUI Designer; IntelliJ's GUI
   designer is not compatible with these files.

---

## CI/CD Pipeline

The project uses a single GitHub Actions workflow:
[`.github/workflows/build-test.yml`](.github/workflows/build-test.yml)

### Matrix

The pipeline runs on three platforms in parallel:

| OS | JDK | Notes |
|----|-----|-------|
| `ubuntu-latest` | 21 (Temurin) | Xvfb virtual display for GUI tests |
| `windows-latest` | 21 (Temurin) | MinGW for native DLL builds |
| `macos-latest` | 21 (Temurin) | GCC for native `.dylib` builds |

### Pipeline stages

1. **Checkout** code
2. **Set up JDK 21** (with Maven cache)
3. **Build native JNI libraries** (platform-specific)
4. **Compile** (`mvn clean compile -DskipTests`)
5. **Start Xvfb** (Linux only, for GUI tests)
6. **Run tests** (`mvn test`)
7. **Verify** (`mvn verify -DskipTests`)
8. **Package JAR** (`mvn package assembly:single -DskipTests`)
9. **Create native installer** via `jpackage` (platform-specific)
10. **Upload artifacts** (JAR and installer, 30-day retention)

### Triggers

- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Manual (`workflow_dispatch`)

---

## Native JNI Libraries

Native C libraries are used for JNI testing under
`src/test/java/ch/technokrat/gecko/geckocircuits/nativec/testJNI_DLL/`.

### Building locally

**Linux/macOS:**
```bash
cd src/test/java/ch/technokrat/gecko/geckocircuits/nativec/testJNI_DLL
chmod +x build_native_libraries.sh
./build_native_libraries.sh
```

**Windows (MinGW):**
```bash
cd src/test/java/ch/technokrat/gecko/geckocircuits/nativec/testJNI_DLL
gcc -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/win32" \
    -o libtestJNI_DLL.dll testJNI_DLL.c -Wl,--kill-at
cp libtestJNI_DLL.dll libtestJNI_DLL2.dll
```

Native libraries are not required for the standard build — only for the JNI
test suite. CI builds them automatically on all platforms.

---

## Internationalization (i18n)

GeckoCIRCUITS supports multiple languages via the `ch.technokrat.gecko.i18n`
package:

- **`I18nKeys.java`** — Enum with 968+ translation keys.
- **`GuiFabric.java`** — Factory for localized Swing components (labels,
  buttons, dialogs).
- **`EnglishMapper.java`** — English language translations.

When adding user-facing text, use `I18nKeys` rather than hardcoded strings.
See existing dialogs for usage patterns.

---

## GUI Development with .form Files

The project has 50+ `.form` files for Swing dialogs, each paired with a `.java`
source file. These are NetBeans GUI Designer files.

### Editing workflow

1. **Open in NetBeans**: Shift+double-click the `.form` file
2. **Design**: Drag components, set properties, wire events
3. **Source**: Click `[Source]` tab to edit business logic
4. **Save**: `Ctrl+S` — NetBeans regenerates the guarded code blocks

### Important

- **Never** manually edit the auto-generated code sections (gray/blue
  background in NetBeans). These are regenerated from the `.form` file.
- Always commit both `.form` and `.java` files together.
- For the full guide, see [NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md).

---

## Debugging

### VSCode

Use the "Run GeckoCIRCUITS (Debug Mode)" launch configuration. Set breakpoints
in the editor and press `F5`.

### Command-line debugging

```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=4000 \
     -Xmx3G -Dpolyglot.js.nashorn-compat=true \
     -jar target/gecko-1.0-jar-with-dependencies.jar
```

Connect your IDE's remote debugger to port 4000.

### Logging

The project uses **Log4j 2**. Loggers are obtained via:

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

private static final Logger LOGGER = LogManager.getLogger(MyClass.class);
```

### Common debugging tips

- **Simulation doesn't start**: Check the status bar and console output for
  errors. Verify circuit has a ground reference.
- **Out of memory**: Increase `-Xmx` (e.g., `-Xmx6G` or `-Xmx8G`).
- **Native library error**: Ensure JNI libraries match your platform and
  architecture.
- **Scripting errors**: Verify `-Dpolyglot.js.nashorn-compat=true` is set.

---

## Contribution Workflow

### 1. Create a branch

```bash
git checkout -b feature/my-feature
# or
git checkout -b fix/issue-123
```

### 2. Make changes

Follow the [code style](#code-style--conventions) and
[warning policy](warning-policy).

### 3. Test locally

```bash
mvn clean package assembly:single
```

Ensure the build succeeds and all tests pass. Run static analysis:

```bash
mvn checkstyle:check pmd:check spotbugs:check
```

### 4. Commit

Use clear, descriptive commit messages following the existing convention:

```
Verb + short description of what changed

Optional longer description explaining why.
```

Examples from the project history:
```
Fix compiler warnings, type safety, serialization, and modernize codebase patterns
Translate project packages, files, classes, methods, and comments to English
Refactor: Extract view command handlers from actionPerformed()
```

### 5. Push and create a Pull Request

```bash
git push origin feature/my-feature
```

Create a PR targeting `main` or `develop`. CI will run automatically across
all three platforms.

---

## Troubleshooting

### Build fails: "invalid target release: 21"

Your JDK is not version 21. Verify:
```bash
java -version
echo $JAVA_HOME
```

### Build fails: Maven not found

Install Maven 3.6+ and ensure it is on your `PATH`.

### Tests fail: Native library not found

Build the native JNI libraries (see [Native JNI Libraries](#native-jni-libraries)),
or skip tests with `-DskipTests` if you are not working on JNI code.

### Tests fail: Headless environment

On Linux without a display, tests requiring Swing may fail. Set up Xvfb:
```bash
Xvfb :99 -screen 0 1024x768x24 &
export DISPLAY=:99
mvn test
```

### `.form` file shows as XML in IDE

Open it in NetBeans with Shift+double-click. Other IDEs may not recognize the
NetBeans GUI Designer format.

### Out of memory during simulation

Increase heap: `-Xmx6G` or `-Xmx8G`. For extremely large circuits, the
application may auto-restart with more memory via `JavaMemoryRestart`.

### GraalVM/JavaScript errors

Ensure `-Dpolyglot.js.nashorn-compat=true` is in the JVM arguments. This flag
enables Nashorn compatibility mode in the GraalVM JavaScript engine.

---

## Quick Reference

| Task | Command |
|------|---------|
| Build (skip tests) | `mvn clean package assembly:single -DskipTests` |
| Build (with tests) | `mvn clean package assembly:single` |
| Compile only | `mvn clean compile` |
| Run tests | `mvn test` |
| Run single test | `mvn test -Dtest=TestClassName` |
| Static analysis | `mvn checkstyle:check pmd:check spotbugs:check` |
| Coverage report | `mvn test` → open `target/site/jacoco/index.html` |
| Run application | `java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar` |
| Clean | `mvn clean` |
