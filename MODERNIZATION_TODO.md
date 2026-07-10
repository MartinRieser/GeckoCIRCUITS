# Code Modernization and Software Improvement Plan

Generated from review of changes since commit 3a72b5fa59a7499133e89c1165e28f00e662276d.

---

## 1. High Priority Issues

### 1.1 Resource Leaks - Use Try-With-Resources ✅ COMPLETED
**File**: MainWindow.java
- [x] GZIPOutputStream and BufferedWriter converted to try-with-resources
- [x] ZipFile, ZipOutputStream, BufferedInputStream converted to try-with-resources
- [x] GZIPInputStream, BufferedReader, InflaterInputStream converted to try-with-resources

### 1.2 Empty Catch Blocks ✅ COMPLETED
**Files**: MainWindow.java, ProjectData.java
- [x] Added proper Logger logging for empty catches in MainWindow.java initialization
- [x] Added proper logging for file read/save errors
- [x] Replaced legacy System.out.println / System.err.println with Logger

### 1.3 Long Method Refactoring ✅ COMPLETED
**Files**:
- [x] **MainWindow.java: `baueGUI()` (508 lines):** Fully refactored into 11 focused helper methods (`buildMenuBar()`, `buildMainPanel()`, etc.).
- [x] **MainWindow.java: `actionPerformed()` (391 lines):** Fully refactored by extracting command processing logic (File, View, Edit, Simulation, Tools/Dialogs) into cohesive private helper methods.
- [x] **ProjectData.java: `importASCII()` (256 lines):** Fully refactored and deconstructed into cohesive private helpers (`importBasicSettings()`, `importWorksheetSize()`, `importDisplayAndScriptSettings()`, `loadCircuitComponents()`, and `loadGeckoFileManager()`).

---

## 2. Medium Priority Issues

### 2.1 Legacy Collections Replacement ✅ COMPLETED
**Files**:
- [x] **ProjectData.java:** Investigated signature replacement for `exportASCII(StringBuffer)`. Since it is implemented by 100+ classes, it was deferred to prevent breaking changes, but local legacy collections like `Vector` were replaced with `ArrayList` where safe.
- [x] **MainWindow.java:** Replaced legacy `Vector` usage with `ArrayList`.
- [x] **GeckoJavaCompiler.java:** Replaced legacy `Vector` usage with `ArrayList`.

### 2.2 Missing @Override Annotations 🔄 PARTIALLY COMPLETED
- [/] Run compiler check with `-Xlint:all` to ensure all implementing methods carry `@Override`. Added overrides to hundreds of files during bulk warning cleanup (commits `d07b6b1` and `7e8ae47`). Remaining interface methods should be updated as files are modified.

### 2.3 Unnecessary SuppressWarnings Annotations 🔄 IN PROGRESS
- [/] Refactor code to avoid suppression where possible. Unavoidable suppressions (e.g., `this-escape` in Swing dialog constructors or `PMD` in legacy APIs/remote RMI layers) are now explicitly documented with rationale comments as defined in `WARNING_POLICY.md`.

---

## 3. Low Priority Issues

### 3.1 Naming Conventions ✅ COMPLETED
- [x] **MainWindow.java:** Translated remaining German variable names (`simulatorAktiviert` -> `simulatorActivated`, `speicherVorgangLaeuft` -> `saveInProgress`, `aktuellerDateiName` -> `currentFileName`) and helper methods (`baueGUI` -> `buildGUI`, `modifiziereTitel` -> `modifyTitle`, `schliesseProgramm` -> `closeProgram`, `aktualisierePropertiesRECENT` -> `updateRecentProperties`) to English.

### 3.2 Magic Numbers ✅ COMPLETED
- [x] **MainWindow.java:** Extracted timer delay/period and scroll unit increments to named constants.

### 3.3 Public Fields Encapsulation ✅ COMPLETED
- [x] **MainWindow.java:** Encapsulated mutable fields with private scopes and getters/setters, updating references in all dependent classes.

### 3.4 Inconsistent Use of Final 🔄 PARTIALLY COMPLETED
- [/] Mark local variables, fields, and parameters as `final` where applicable. Substantial progress was made in the compiler warnings cleanups (commits `d07b6b1` and `7e8ae47`).

### 3.5 Missing SerialVersionUID ✅ COMPLETED
- [x] **ProjectData.java:39:** Added `private static final long serialVersionUID = 1L;` to prevent serialization incompatibility.

### 3.6 Unused Imports ✅ COMPLETED
- [x] Cleaned up unused imports across the codebase during bulk warning cleanup.

### 3.7 Incomplete Translation ✅ COMPLETED
- [x] Translated remaining German comments in `ProjectData.java` (e.g., `// Hilfsfunktionen:` -> `// Helper functions:`, `// Simulationsparameter` -> `// Simulation parameters`) and `MainWindow.java` to English.

### 3.8 Builder Pattern Opportunity ❌ PENDING
- [ ] **MainWindow.java:** Refactor complex constructor setups using the builder pattern where appropriate.

### 3.9 Raw Type Usage ✅ COMPLETED
- [x] Resolved raw type declarations (e.g., swing components) across the codebase during the warnings cleanup.

### 3.10 Static Collections ✅ COMPLETED
- [x] **MainWindow.java:** Made public static fields private and added static getters/setters.

### 3.11 Code Duplication ✅ COMPLETED
- [x] **CodeWindowModern.java:** Unified duplicate identical `KeyListener` implementations into a single private transient field `_dirtyFlagKeyListener`.

## Priority Execution Order & Active Plan

### Next Recommended Refactoring Iterations

For developers looking to continue code improvements, follow this order of execution:

1. **MainWindow.actionPerformed() Command Extraction (High Priority)**
   * **Goal:** Completely clean up the remaining `actionPerformed` if-else chain.
   * **Steps:** 
     a. Extract **Edit menu commands** (Undo, Redo, Copy, Move, Delete, Deselect, SelectAll, Enable/Disable, Short).
     b. Extract **Simulation commands** (Run/Init & Start, Pause, Continue).
     c. Extract **Tools & Dialog commands** (Parameters, Find, Settings, About, Feedback, Licensing, Updates).
   * **Testing:** Manually test the respective menu action after each extraction step.

2. **ProjectData.importASCII() Method Decomposition (High Priority)**
   * **Goal:** Deconstruct this 250+ line parsing method.
   * **Steps:** Extract into separate helper methods:
     * `importBasicSettings()` - parses basic simulation properties (tDURATION, dt, paths)
     * `importWorksheetSize()` - parses sheet dimension settings
     * `importDisplaySettings()` - parses domain-specific view parameters (LK, CONTROL, THERM)
     * `loadCircuitComponents()` - parses circuit and terminal blocks
     * `loadGeckoFileManager()` - handles additional external file registrations
   * **Testing:** Verify import compatibility by opening several sample circuit files (`resources/Education_ETHZ/*.ipes`).

3. **Legacy Collections and Thread Safety Improvements (Medium Priority)**
   * Replace `StringBuffer` with `StringBuilder` in [ProjectData.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/ProjectData.java) and [MainWindow.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/MainWindow.java).
   * Replace obsolete, synchronized `Vector` with `ArrayList` or unmodifiable lists where thread safety is not needed, or `CopyOnWriteArrayList` where concurrency occurs.

4. **German Variable & Comment Translations (Low Priority)**
   * Translate remaining GUI-specific German variable names (`simulatorAktiviert`, `speicherVorgangLaeuft`, `aktuellerDateiName`) and methods in `MainWindow` to English.
   * Translate remaining German comments (e.g. `ProjectData.java:210`).

---

## Testing & Verification Guidelines

Ensure that the project's build and verification standards are met after any change:

1. **Verify Local Build and Test Run:**
   ```bash
   mvn clean package assembly:single
   ```
   * Confirm that all **159 automated tests** pass cleanly with no errors.
2. **Review Static Analysis Checks:**
   ```bash
   mvn checkstyle:check pmd:check spotbugs:check
   ```
   * Ensure that no new styling issues, bugs, or quality violations are introduced.
3. **Verify Warnings & Suppressions Policy:**
   * Build using `mvn compile` and verify no new warnings are generated.
   * If a suppression is unavoidable, ensure it is documented with a rationale comment as detailed in [WARNING_POLICY.md](WARNING_POLICY.md).
