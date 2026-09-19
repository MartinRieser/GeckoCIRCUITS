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

## Task Completeness & Definition of Done
- **No Premature Completion**: Never declare a task, goal, or milestone complete until all subtasks, edge cases, downstream integrations, and architectural plan requirements are fully implemented and verified.
- **Plan Cross-Check**: Before finishing any phase or task, explicitly review the requirements checklist in the plan/specification document line-by-line.
- **Clean Refactoring**: Eliminate code duplication immediately (e.g., share utility methods across core and builders) rather than leaving duplicate logic in place.

## CI & GitHub Actions Standards
- Keep GitHub Actions on current major versions (`actions/checkout@v7`, `actions/setup-java@v6`, `actions/setup-python@v7`, `actions/setup-node@v7`, `actions/upload-artifact@v7`, `actions/download-artifact@v8`, `actions/cache@v6`, `softprops/action-gh-release@v3`).

## Desktop Packaging
- Local native packaging is executed via `scripts/package-desktop.bat` (Windows), `scripts/package-desktop.sh` (Linux/macOS), or `python scripts/package-desktop.py --type all`.
