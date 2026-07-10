# GeckoCIRCUITS Improvement Execution Plan

## Goal
Apply all improvements documented in `improvement-reviews/01-13*.md` to the codebase, adding test cases alongside, using AI agents efficiently with minimal token usage.

## Build & Verify Commands
```bash
mvn -T 1C clean compile                   # Compile gate (fast parallel build)
mvn -T 1C test                            # Run all tests (159 baseline)
mvn test -Dtest="ClassName"               # Run a single test class
mvn test -Dtest="*Pattern*"               # Run tests matching a pattern
mvn jacoco:report                         # Generate test coverage report
mvn spotbugs:check                        # SpotBugs on changed code (advisory after high-risk phases)
```

> **Note on static analysis:** The codebase has never been checkstyle/spotbugs-clean,
> so running `checkstyle:check` or `spotbugs:check` as a hard gate produces hundreds
> of pre-existing findings that drown out new issues. Instead, run `spotbugs:check`
> **advisory** (informational, non-blocking) after Phases 5, 6, and 7 to catch
> *new* bugs introduced by the changes. Filter the report to only review files
> touched in that phase. Defer a full `checkstyle:check` gate until a separate
> code-style cleanup phase with a proper `checkstyle.xml` ruleset.

---

## Execution Principles for Token Efficiency & Velocity

1. **Leverage Modern Large Context Windows:** Instead of very small batches (3-5 files), use larger, logical batches (15-20 files for code edits, 40-50 files for bulk documentation like Javadoc). This reduces startup overhead and token waste from repeating project context.
2. **Batch by Task Type, Not Package:** An agent doing typo fixes or dead-code removal across 20 files is extremely fast and efficient because the conceptual pattern is identical.
3. **Test-Driven Bug Fixing:** Write tests (Phase 5) *before* fixing the bugs (Phase 6). This provides proof that the bug existed and is correctly solved.
4. **Write Tests BEFORE translations/refactorings:** Adding a comprehensive test suite before making high-risk changes (like German-to-English renames) provides a strong verification gate.
5. **Javadocs Last:** Complete all code cleanups, bug fixes, and renames before Javadoc generation. This ensures parameters and method names in the documentation match the final English names (e.g., `@param nodeIndex` rather than `@param knotenIndex`).
6. **Phase-Level Commits:** Git commit and compile after every single batch to ensure errors can be easily bisected.

---

## Phase Order (Optimized Dependency Flow)

| Phase | Type | Risk | Files | AI Sessions | Notes / Goals |
|-------|------|------|-------|-------------|---------------|
| 1 | Dead code & debug prints removal | Very Low | ~60 | 3 batches of ~20 | Clean clutter |
| 2 | Typo fixes in identifiers & comments | Low | ~25 | 1-2 batches | Fix misspelled public/private keys |
| 3 | Magic number extraction | Low | ~30 | 2 batches | Constant extraction |
| 4 | Assert-to-exception fixes | Medium | ~10 | 1 batch | Replace assert-false with explicit exceptions |
| 5 | Test cases (new) | Medium | ~100 | 5 batches of 20 | Safety net for bugs and renames |
| 6 | Bug fixes (Test-Driven) | Medium-High | ~15 | 1-2 batches | Correct functional errors using new tests |
| 7 | German-to-English translations | High | ~50 | 3 batches | Cross-file refactoring + serialization aliases |
| 8 | Javadoc -- class-level + method-level | Low (volume)| ~785 | 16 batches of ~50 | Document the clean, final English codebase |

---

## Phase 1: Dead Code & Debug Print Removal
**Risk:** Very low. Only deletes commented-out code and `System.out.println` debug lines.
**Verification:** `mvn -T 1C clean compile` -- no behavior change.

### Agent prompt template (copy per batch):
```
Read the improvement review section for these files from <REVIEW_FILE_PATH>.
For EACH file below, read the source, then apply ONLY the dead-code and debug-print
tasks listed in its review section:
- Remove commented-out code blocks (not license headers or Javadoc)
- Remove unused imports
- Remove System.out.println debug statements (replace with Logger if review says so)
- Remove dead local variables

DO NOT make any other changes. DO NOT rename anything. DO NOT add Javadoc.

Files (base: src/main/java/ch/technokrat/gecko/geckocircuits/):
1. <path1>
2. <path2>
...
20. <path20>

After editing, run: mvn -T 1C clean compile
Report any compilation errors.
```

### Batch breakdown:
* **Batch 1:** Files from `04-gecko-root.md` and `08-circuitcomponents.md` (commented-out / debug prints)
* **Batch 2:** Files from `09/10-control*.md` and `11-calculators.md`
* **Batch 3:** Files from `12-misc-packages.md`, `13-newscope.md`, `05/06-circuit*.md`

**Gate:** `mvn -T 1C clean compile` passes. Git commit: `"Remove dead code and debug prints"`.

---

## Phase 2: Typo Fixes
**Risk:** Low (file-local) to Medium (public identifiers need caller updates).
**Verification:** `mvn -T 1C clean compile`.

### Targeted Typos:
* `DEFAULT_FREQENCY` -> `DEFAULT_FREQUENCY` in `ControlSlidingDFT.java`
* `COMPILED_SUCCESSFULL` -> `COMPILED_SUCCESSFUL` in both nativec and javablock `CompileStatus.java` enums.
* `LIGTHGRAY` -> `LIGHTGRAY` in `GraferV3.java`
* `savegetFile` -> `safeGetFile` & `savegetFileName` -> `safeGetFileName` in `NativeCLibraryFile.java`
* `severeErrorOccured` -> `severeErrorOccurred` in `ControlNativeC.java`
* `SMALL_SIGNAL_ANALYIS` -> `SMALL_SIGNAL_ANALYSIS` in `I18nKeys`
* Comment typos (e.g. `Paramter` -> `Parameter` in `UserParameter.java` / `GlobalParameterUndoable` undo presentation name)

### WARNING -- I18nKeys and Resource Bundle Verification:
Several typo fixes affect `I18nKeys` enum constants and `UserParameter` string
identifiers that are used as lookup keys in `.properties` resource bundles and
serialization token maps. When renaming any of these:
1. Grep for ALL references across `src/main/` AND `src/test/`
2. Check `resources/*.properties` files for matching keys
3. If a key exists in a `.properties` file, rename it there too
4. For serialization tokens (used in `.ipes` file save/load), add the old name
   as a backward-compatible alias rather than renaming outright (see Phase 7's
   alias system for the full mechanism)

Affected typos requiring this caution:
* `SMALL_SIGNAL_ANALYIS` -> `SMALL_SIGNAL_ANALYSIS` (I18nKeys enum + properties)
* `COMPILED_SUCCESSFULL` -> `COMPILED_SUCCESSFUL` (used in serialization)

### Agent prompt template:
```
Fix typos in these files per the review at <REVIEW_FILE_PATH>.
For each identifier rename, FIRST run grep to find all references across the codebase
(including src/main/, src/test/, and resources/*.properties),
THEN rename in all files.

CAUTION: If a renamed identifier is an I18nKeys constant or serialization token,
also update any matching keys in resources/*.properties files. For serialization
tokens, add the old name as a backward-compatible alias (see Phase 7).

Files:
1. <path1>
2. <path2>
...

After editing, run: mvn -T 1C clean compile
Report any compilation errors and list ALL files modified.
```

**Gate:** `mvn -T 1C clean compile`. Git commit: `"Fix typos in identifiers and comments"`.

---

## Phase 3: Magic Number Extraction
**Risk:** Low. Extract literal values into `public/private static final` constants.

### Key extractions:
* `HiLoData.java` (scope): Extract `1E30f` / `-1E30f` sentinels into `public static final float SENTINEL_LO = 1E30f;` and `SENTINEL_HI = -1E30f;` (used by other classes).
* `HiLoData.java` (newscope): Extract `1E30f` constant.
* `GraferImplementation.java`: `1000` (index encoding), `10000`, `0.6f`.
* `ControlSaveData.java`: `100` (MAX_FILE_COUNTER).
* `ControlSlidingDFT.java`: `2` (frequency doubling).
* `NativeCDialog.java`: `0.3` (DIALOG_SIZE_RATIO).

### Agent prompt template:
```
For each file below, read the source and the review at <REVIEW_FILE_PATH>.
Apply ONLY the magic-number extraction tasks from the review:
- Replace bare numeric literals with named constants at class level
- Name constants in UPPER_SNAKE_CASE
- DO NOT change any logic or behavior

Files:
1. <path1>
...

After editing, run: mvn -T 1C clean compile
```

**Gate:** `mvn -T 1C clean compile`. Git commit: `"Extract magic numbers into named constants"`.

---

## Phase 4: Assert-to-Exception Fixes
**Risk:** Medium. Changes runtime behavior when invalid input is given.
**Verification:** `mvn -T 1C clean compile && mvn -T 1C test`.

### Targets:
* `CompileStatus.java` -- `getFromOrdinal`: `assert false; return null` -> `throw new IllegalArgumentException("Invalid ordinal: " + ordinal);`
* `AxisLinLog.java` -- `getFromOrdinal`: same pattern.
* `Matrix.java` -- `solve()`: `assert false; return null` -> `throw new IllegalArgumentException("Matrix must be square to solve.");`

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test`. Git commit: `"Replace assert-false with IllegalArgumentException"`.

---

## Phase 5: New Test Cases (Safety Net)
**Risk:** None (additive only). High value. Adds coverage before bug fixing and refactoring.

### Test Strategy:
1. **Calculator tests:** Extend `AbstractSimpleMathFunctionTest`, `AbstractTwoInputsMathFunctionTest`, and `AbstractMultiInputFunctionTest`. Write test files for the ~15 untested calculators.
2. **Math tests:** Create JUnit 5 tests for `Matrix.java`, `BigMatrix.java`, `NComplex.java`, `LUDecomposition.java`, and `CholeskyDecomposition.java`.
3. **DataContainer tests:** Test `HiLoData.java` and `DataContainerSimple.java`.

### Agent prompt template:
```
TASK: Write JUnit 5 tests for <ClassName>. Read the source file and the
existing test pattern at <TEMPLATE_PATH> for style reference.

Create a test class at src/test/java/<mirrored_path>/<ClassName>Test.java
Cover happy paths, edge cases (NaN, Infinities, negative, zero), and regression cases.

After writing, run: mvn test -Dtest="<ClassName>Test"
```

**Gate:** `mvn -T 1C test` and `mvn jacoco:report`. (Advisory: `mvn spotbugs:check` — review only NEW findings in test classes.) Git commit: `"Add unit test safety net for <scope>"`.

---

## Phase 6: Bug Fixes (Test-Driven)
**Risk:** Medium-High. Each fix needs careful analysis.
**Verification:** `mvn -T 1C clean compile && mvn -T 1C test` verifies the fixes against Phase 5 tests.

### Critically Identified Bugs:

| File | Bug Details | Correct Fix Action |
|------|-------------|--------------------|
| `LUDecomposition.java:129` | `&` should be `&&` | Use short-circuiting `&&` to avoid index bounds exception on `LU[j][j]`. |
| `BigLUDecomposition.java:132` | Same `&` vs `&&` bug | Replace `&` with `&&`. |
| `CholeskyDecomposition.java:79,82` | Same `&` vs `&&` bug | Replace logical `&` with short-circuiting `&&`. |
| `FourierDiagramm.java:247-259` | Duplicate zoom conditions + self-comparison | Simplify nested duplicate/buggy `if` conditions with: `g.drawRect(Math.min(x1Zoom, x2Zoom), Math.min(y1Zoom, y2Zoom), Math.abs(x2Zoom - x1Zoom), Math.abs(y2Zoom - y1Zoom));` |
| `FourierKurvenRekonstruktion.java:202-214` | Same zoom condition bug | Apply same `Math.min`/`Math.abs` simplification. |
| `FourierDiagramm.java:563-587` | `getPixelFromValue` returns `{-1, -1}` | Implement correct math formulas: `xPix = (xAchseTyp_ == ACHSE_LOG) ? (int) Math.round(xAchseX_ + Math.log10(xWert / achseXmin_) * sfX_) : (int) Math.round(xAchseX_ + (xWert - achseXmin_) * sfX_);` (similarly for `yPix`). |
| `FourierKurvenRekonstruktion.java:444-469` | Same `getPixelFromValue` bug | Apply the same math formulas for logarithmic and linear pixel calculations. |
| `GraferImplementation.java:1323-1331` | Shadowed variables create no-op self-assignment | Resolve shadowing of the fields `xTickAutoSpacing` and `yTickAutoSpacing` by referencing `this.xTickAutoSpacing` and `this.yTickAutoSpacing`. |
| `HiLoData.java` (newscope): `179-190` | `correctWithNotANumber` dead logic bug | Fix `correctWithNotANumber` to return `value1` in the `else` branch (currently returns `value2`). |
| `ControlSPARSEMATRIX.java` | Output counts mismatch | Align outputs: `getOutputNames()` returns 9, `getOutputDescription()` returns 8. Fix duplication on line 34 ("uN2" -> "uN3"). |
| `NativeCBlock.java:108` | Infinity values check missing | Add check for `Double.isInfinite()` alongside NaN checks in `checkOutputsForNANorINFValues`. |
| `CompiledClassContainer.java:51` | NPE risk | Add null check for `_classBytes` in `getClassBytes()`. |
| `NativeCDialog.java:191` | Printing stack trace array hash | Replace `println(exc.getStackTrace())` with `exc.printStackTrace(System.err)`. |

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test` all pass. (Advisory: `mvn spotbugs:check` — review only NEW findings in bug-fix files.) Git commit: `"Fix identified bugs using test-driven development"`.

---

## Phase 7: German-to-English Translations & Serialization Aliases
**Risk:** High. Renaming serialized keys could break loading older schematic files.
**Verification:** `mvn -T 1C clean compile && mvn -T 1C test` + GUI validation.

### Required Alias System Implementation:
Modify [UserParameter.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/general/UserParameter.java) to support legacy aliases:
1. Add `_legacyIdentifiers` list to `UserParameter` and its `Builder`.
2. In `readFromTokenMap`, fallback to checking legacy keys if the new identifier is not found in `TokenMap`:
   ```java
   String foundIdentifier = null;
   if (tokenMap.containsToken(_identifier)) {
       foundIdentifier = _identifier;
   } else {
       for (String legacyId : _legacyIdentifiers) {
           if (tokenMap.containsToken(legacyId)) {
               foundIdentifier = legacyId;
               break;
           }
       }
   }
   ```

### German -> English Translation Targets:
* `anzXIN` -> `numberInputTerminals` (use `"anzXIN"` as a legacy identifier for backward compatibility)
* `anzYOUT` -> `numberOutputTerminals` (use `"anzYOUT"` as a legacy identifier)
* `baueGuiIndividual` -> `buildIndividualGUI` (coordinate across dialog subclasses)
* `baueGUI` -> `buildGUI`
* `istAngeklickt` -> `isClicked`
* `knotenIndex` -> `nodeIndex`
* `mausModus` -> `mouseMode`
* `_xKlick*` -> `_xClick*`
* `SaveModus` -> `SaveMode`

*Note: Verify `I18nKeys.java` and `*.properties` resource bundles so that strings displayed in the GUI are correctly mapped without breaking translation keys.*

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test`. (Advisory: `mvn spotbugs:check` — review only NEW findings in renamed files.) Git commit: `"Translate German identifiers to English and add serialization aliases"`.

---

## Phase 8: Javadoc Documentation (Bulk)
**Risk:** Very low. Documentation only.
**Strategy:** Run in parallel batches of 50 files. Skip classes already documented in earlier phases.

### Agent prompt template (High Efficiency):
```
TASK: Add Javadoc only. Do NOT change any code.
The Javadoc tasks for each file are in <REVIEW_FILE_PATH>.

For each file:
1. Read the source file
2. Add class-level Javadoc before the class/interface/enum declaration
3. Add method Javadoc with @param/@return for public methods mentioned in the review
4. DO NOT modify any code, imports, or existing comments

Files:
<file1> ... <file50>

After editing, run: mvn -T 1C clean compile
```

**Gate:** `mvn -T 1C clean compile` per batch. Git commit: `"Add Javadoc: <scope> (<N> files)"`.

---

## Summary: AI Session Budget

| Phase | Batches | Files/batch | Est. tokens/batch | Total est. tokens |
|-------|---------|-------------|-------------------|-------------------|
| 1: Dead code | 3 | 20 | 25K | 75K |
| 2: Typos | 1 | 25 | 30K | 30K |
| 3: Magic numbers | 2 | 15 | 20K | 40K |
| 4: Assert fixes | 1 | 10 | 15K | 15K |
| 5: New test cases | 5 | 20 | 45K | 225K |
| 6: Bug fixes | 1 | 15 | 40K | 40K |
| 7: German->English | 3 | 15 | 40K | 120K |
| 8: Javadoc | 16 | 50 | 35K | 560K |
| **Total** | **32** | | | **~1.1M** |

*Note: By scaling batch sizes appropriately for modern models, the number of sessions was reduced from **90** to **32**, significantly accelerating execution speed and lowering workspace bisection overhead.*

---

## Execution Checklist

```
[ ] Phase 1: Dead code removal         (3 batches)   mvn -T 1C clean compile
[ ] Phase 2: Typo fixes                (1 batch)     mvn -T 1C clean compile
[ ] Phase 3: Magic number extraction   (2 batches)   mvn -T 1C clean compile
[ ] Phase 4: Assert -> exception       (1 batch)     mvn -T 1C clean compile && mvn -T 1C test
[ ] Phase 5: New test cases            (5 batches)   mvn -T 1C test && mvn jacoco:report
[ ] Phase 6: Bug fixes + regression    (1 batch)     mvn -T 1C clean compile && mvn -T 1C test
[ ] Phase 7: German -> English         (3 batches)   mvn -T 1C clean compile && mvn -T 1C test
[ ] Phase 8: Javadoc (bulk)            (16 batches)  mvn -T 1C clean compile
[ ] Final: Full verification                         mvn -T 1C clean package assembly:single
```
