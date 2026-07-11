# GeckoCIRCUITS Agent Rules

## 1. Terminal Command Chaining in Windows PowerShell
When executing or proposing chained terminal commands on Windows, do NOT use `&&` as a command separator (e.g. `git add . && git commit`). The target shell is PowerShell, which does not support `&&` by default in all versions and throws a parser error.
- **Correct**: Use `;` as the statement separator (e.g. `git add .; git commit -m "..."`).

## 2. AWT/Swing Unit Test Headless Safety
Any new or modified JUnit tests testing components that interact with AWT/Swing GUIs (like `SpaceVectorDisplay`) must be safe for headless execution (common in server builds and Maven CI tasks).
- Check `java.awt.GraphicsEnvironment.isHeadless()` at the beginning of the test or test methods.
- Gracefully return or bypass graphics-dependent assertions when running in headless mode to prevent `HeadlessException` from breaking the build.
