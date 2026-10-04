# GeckoCIRCUITS Development & Build Standards

## Compilation & Warning Standards
- **Zero-Warning Goal**: All code modifications must compile with 0 javac warnings.
- **Architectural Fixes Over Suppression**: When addressing deprecations, build warnings, or runtime warnings, never merely suppress, silence, or disable warning output (e.g., passing JVM suppression flags like `--sun-misc-unsafe-memory-access=allow`, adding catch-all compiler suppression, or silencing stderr). Always investigate and fix the root architectural cause by migrating to standard APIs, modern class-loading strategies, or compliant configurations.
- **Serialization**: Any class implementing `java.io.Serializable` or extending `Exception`/`RuntimeException` must declare `private static final long serialVersionUID = 1L;`.
- **Generics**: Use type-safe generic collections (`Collections.emptyList()`, `List<T>`) instead of legacy raw types (`Collections.EMPTY_LIST`, raw `Vector`).
- **Constructor Safety**: Prevent `this-escape` warnings by making constructor-called methods `private` or `final`, or adding `@SuppressWarnings("this-escape")` where architecturally necessary.

## Modern JDK (Java 25+) & Build Tooling Standards
- **Maven Guice Compatibility**: When running Maven builds on modern JDKs (JDK 24+ / 25), configure Guice to use child classloaders via `-Dguice_custom_class_loading=CHILD` (in `.mvn/jvm.config` and launcher scripts `MAVEN_OPTS`). This avoids terminally deprecated `sun.misc.Unsafe.staticFieldBase` calls from `HiddenClassDefiner` and uses standard `ClassLoader` hierarchies instead.
- **Repository JVM Config**: Always keep `.mvn/jvm.config` checked into source control so that command-line, IDE, and CI builds automatically inherit compliant JVM parameters.

## Testing & Verification Standards
- **No Test Skipping**: When verifying changes or running tests, never skip tests (`-DskipTests` is only for rapid intermediate compile checks). Always run `mvn test` before finalizing or merging changes.
- **Exhaustive Field Fidelity**: When implementing parsers, serializers, or data models, tests must assert exact equality for every single modeled attribute, array, connection, identifier, flag, and metadata field. Never settle for superficial smoke checks.
- **Complete Downstream Integration**: When adding core capabilities (e.g., serialization, new model types), always complete the integration in downstream consumers (services, controllers, netlist builders) and add full test coverage across all layers.

## Example-Circuit Quality Gate (MANDATORY before declaring example work done)
Bundled examples in `frontend/src/model/examples.ts` are user-facing products, not test fixtures. A change to an example (or to any engine semantics an example depends on) is **not done** until ALL of the following pass:

1. **Headless simulation gate**: `python scripts/verify-examples.py` (all examples, exit 0). It runs every example through the REST engine and asserts structural validity (COMPLETED, all `dataContainerSignals[]` recorded, finite, non-degenerate) plus per-example physics plausibility (steady-state windows, ripple bounds, power factor) in its `ASSERTIONS` table.
2. **New examples get assertions**: When adding an example, add its entry to the `ASSERTIONS` table in `scripts/verify-examples.py` with windows derived from circuit theory (V = Vin·D, 1/(1−D), τ = RC, P/(ω·C·V) ripple, diode drops), not from eyeballing the simulation output. Ranges ±20% or generous — the gate catches broken physics, not engineering taste.
3. **Frontend suite**: `npx vitest run` + `npx tsc --noEmit` in `frontend/` (includes `examples.connectivity.test.ts`: orthogonal wires, pins touched, no badge/label collisions).
4. **GUI smoke test**: Load the changed example in the web editor (Examples menu → run F5) and confirm the scope renders the promised channels with plausible shapes. Static tests cannot catch a flat-lined scope channel.
5. **Name-coupling rule**: every scope input label, `dataContainerSignals[]` entry, and `savedSignalNames[]` entry must exactly match a producer's output label (`labelEndKnoten[]`). A mismatch records a dangling constant-zero signal — the signature bug of example breakage (caught by check 1's stuck-at-zero rule).

Known-good reference values live in the `ASSERTIONS` table; if physics-verification fails, fix the example (or the engine), never the window — windows are derived from theory, and widening one requires a stated physical justification in the commit message.

## Task Completeness & Definition of Done
- **No Premature Completion**: Never declare a task, goal, or milestone complete until all subtasks, edge cases, downstream integrations, and architectural plan requirements are fully implemented and verified.
- **Plan Cross-Check**: Before finishing any phase or task, explicitly review the requirements checklist in the plan/specification document line-by-line.
- **Clean Refactoring**: Eliminate code duplication immediately (e.g., share utility methods across core and builders) rather than leaving duplicate logic in place.

## CI & GitHub Actions Standards
- Keep GitHub Actions on current major versions (`actions/checkout@v7`, `actions/setup-java@v6`, `actions/setup-python@v7`, `actions/setup-node@v7`, `actions/upload-artifact@v7`, `actions/download-artifact@v8`, `actions/cache@v6`, `softprops/action-gh-release@v3`).

## Desktop Packaging
- Local native packaging is executed via `scripts/package-desktop.bat` (Windows), `scripts/package-desktop.sh` (Linux/macOS), or `python scripts/package-desktop.py --type all`.

## Systematic Code Review & Refactoring Invariants
When conducting codebase reviews or refactorings across GeckoCIRCUITS (frontend or backend):
- **Documentation**: Every class, interface, enum, and method must have complete Javadoc/TSDoc detailing parameters, return values, exceptions, and physical/electrical meaning.
- **No Magic Numbers**: Numeric literals (tolerances, port IDs, solver codes, step counts) and sentinel strings must be centralized as typed enums or named constants in dedicated constants classes.
- **No Ad-Hoc Heuristics**: Prefer typed polymorphism, component schemas, or explicit metadata over fragile name-prefix checking.
- **No Unfinished Implementations**: Never leave stub methods or incomplete downstream consumers. Ensure full end-to-end integration.
- **Zero Duplication**: Extract shared algorithms and utilities to common packages immediately.
- **High Test Coverage**: Every refactored or new class must be paired with comprehensive unit tests achieving maximum feasible coverage (>90%).
- **Strict Typechecking Gate**: In TypeScript frontend modules, test verification must enforce full compiler typechecking (`tsc --noEmit`) in addition to runtime unit tests, ensuring no literal type narrowing, invalid props, or mock type mismatches go undetected.
- **Phased Commit Cadence**: Maintain an active review plan markdown document, verify clean test runs at each phase (zero javac warnings, zero test failures), and commit progress progressively.

## Frontend & Cross-Layer Defensive Design Standards
- **Defensive Helper Scrutiny & Strict Isolation**: Before reusing an existing helper function (especially for filtering, net resolution, or signal extraction), inspect its implementation for hidden fallback behaviors. If a requirement mandates strict isolation (e.g. "no signals outside this instrument"), implement or verify a strict non-fallback variant (`strictScopeChannels`) rather than silently inheriting loose fallbacks.
- **Cross-Layer Single Source of Truth for Model Keys**: When introducing component parameters, persistence keys, or IPC messages shared between backend and frontend (or across parser, writer, and REST API), always declare a centralized constants definition (e.g. `ScopeSettingsKeys.java` in Java and `SCOPE_SETTING_KEYS` in TypeScript `constants.ts`). Never duplicate raw string literals across modules.
- **Type-Safe Workspace Navigation & Identifiers**: Never perform ad-hoc string slicing or manual prefix concatenation (`id.slice('scope:'.length)`) in UI components. Centralize navigation IDs into typed constructor and discriminator functions (e.g. `scopeTabId(name)`, `isScopeTab(id)`, `extractScopeName(id)`).
- **Defensive Action & Concurrency Guards**: Any action that launches asynchronous execution, simulation runs, or server operations (including keyboard shortcuts like `F5` or toolbar buttons) must guard against concurrent re-entry or double-submission (e.g., verifying `!isSimRunning`).
- **Zero Inline Layout Styling**: Avoid inline JSX `style={{ ... }}` objects for layout containers, progress bars, spinners, or indicators. Define explicit CSS classes in `styles.css` using theme design tokens.

