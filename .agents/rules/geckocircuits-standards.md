# GeckoCIRCUITS Development & Build Standards

## Compilation & Warning Standards
- **Zero-Warning Goal**: All code modifications must compile with 0 javac warnings.
- **Serialization**: Any class implementing `java.io.Serializable` or extending `Exception`/`RuntimeException` must declare `private static final long serialVersionUID = 1L;`.
- **Generics**: Use type-safe generic collections (`Collections.emptyList()`, `List<T>`) instead of legacy raw types (`Collections.EMPTY_LIST`, raw `Vector`).
- **Constructor Safety**: Prevent `this-escape` warnings by making constructor-called methods `private` or `final`, or adding `@SuppressWarnings("this-escape")` where architecturally necessary.

## Testing Standards
- When verifying changes or running tests, never skip tests (`-DskipTests` is only for rapid intermediate compile checks). Always run `mvn test` before finalizing or merging changes.

## CI & GitHub Actions Standards
- Maintain modern GitHub Actions compatible with Node 24 runners (`actions/checkout@v7`, `actions/setup-java@v5`, `actions/setup-python@v7`, `actions/upload-artifact@v7`, `actions/download-artifact@v8`, `softprops/action-gh-release@v3`).

## Desktop Packaging
- Local native packaging is executed via `scripts/package-desktop.bat` (Windows), `scripts/package-desktop.sh` (Linux/macOS), or `python scripts/package-desktop.py --type all`.
