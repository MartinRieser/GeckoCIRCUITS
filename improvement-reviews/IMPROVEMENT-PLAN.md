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

1. **Sequential Branch Strategy:** Execute all phases sequentially on the same feature branch. Each phase commits on top of the previous one. Do not branch per phase. This avoids merge conflicts between code cleanups, bug fixes, and renames.
2. **LLM Switching Workflow:** Switch to the recommended model in the IDE dropdown *before* starting each phase. Because you cannot change the model while an agent is running, request execution phase-by-phase (e.g., "Run Phase 1 and stop") to allow switching.
3. **Leverage Modern Large Context Windows:** Instead of very small batches (3-5 files), use larger, logical batches (15-20 files for code edits, 40-50 files for Javadoc). This reduces startup overhead and token waste from repeating project context.
4. **Batch by Task Type, Not Package:** An agent doing typo fixes or dead-code removal across 20 files is extremely fast and efficient because the conceptual pattern is identical.
5. **Test-Driven Bug Fixing:** Write tests (Phase 5) *before* fixing the bugs (Phase 6). This provides proof that the bug existed and is correctly solved.
6. **Write Tests BEFORE translations/refactorings:** Adding a comprehensive test suite before making high-risk changes (like German-to-English renames) provides a strong verification gate.
7. **Javadocs Last:** Complete all code cleanups, bug fixes, and renames before Javadoc generation. This ensures parameters and method names in the documentation match the final English names (e.g., `@param nodeIndex` rather than `@param knotenIndex`).
8. **Phase-Level Commits:** Git commit and compile after every single batch to ensure errors can be easily bisected.
9. **Mandatory Use of Target Markdown Files:** For every phase, the executing AI agent MUST NOT rely on the partial or illustrative lists printed directly in this plan. Instead, the agent MUST read the comprehensive, file-by-file targets list located in [improvement-reviews/targets/](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets) (e.g. `phase2_typo_fixes.md`, `phase3_magic_numbers.md`, etc.). These files have been pre-compiled from all 791 reviewed source files. The lists printed in this plan are only key examples and warnings.

---

## Phase Order (Optimized Dependency Flow)

| Phase | Type | Risk | Files | AI Sessions | Recommended LLM | Notes / Goals | Target list reference |
|-------|------|------|-------|-------------|-----------------|---------------|-----------------------|
| 1 | Dead code & debug prints removal | Very Low | 143 | ~7 batches of ~20 | **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** | Clean clutter | [phase1_dead_code.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase1_dead_code.md) |
| 2 | Typo fixes in identifiers & comments | Low | 62 | ~3 batches of ~20 | **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** | Fix misspelled public/private keys | [phase2_typo_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase2_typo_fixes.md) |
| 3 | Magic number extraction | Low | 70 | ~3 batches of ~20 | **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** | Constant extraction | [phase3_magic_numbers.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase3_magic_numbers.md) |
| 4 | Assert-to-exception fixes | Medium | 8 | 1 batch | **Gemini 3.5 Flash (High)** | Replace assert-false with explicit exceptions | [phase4_assert_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase4_assert_fixes.md) |
| 5 | Test cases (new safety net) | Medium | ~25 | 2 batches | **Gemini 3.5 Flash (High)** | Safety net for bugs and renames | *Self-contained package tests* |
| 6 | Bug fixes (Test-Driven) | Medium-High | 71 | ~3-4 batches | **Gemini 3.5 Flash (High)** | Correct functional errors using new tests | [phase6_bug_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase6_bug_fixes.md) |
| 7 | German-to-English translations | High | 68 | ~3-4 batches | **Gemini 3.5 Flash (High)** | Cross-file refactoring + serialization aliases | [phase7_translations.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase7_translations.md) |
| 8 | Javadoc -- class-level + method-level | Low (volume)| 772 | 16 batches of ~50 | **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** | Document final English codebase | [phase8_javadoc.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase8_javadoc.md) |

---

## Phase 1: Dead Code & Debug Print Removal
* **Recommended LLM:** **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** (Fast & cost-effective for bulk code removal)
* **Risk:** Very low. Only deletes commented-out code and `System.out.println` debug lines.
* **Verification:** `mvn -T 1C clean compile` -- no behavior change.

### Target Files List:
The complete, comprehensive list of 143 files is located in [phase1_dead_code.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase1_dead_code.md). Note that Phase 1 has already been fully executed and committed.

### Agent prompt template (copy per batch):
```
Using the target files and tasks list from phase1_dead_code.md:
For EACH file in the current batch, read the source, then apply ONLY the dead-code, debug-print, and redundant element tasks specified:
- Remove commented-out code blocks (not license headers or Javadoc)
- Remove unused imports, fields, variables, parameters, and methods
- Remove System.out.println / printStackTrace debug statements
- Remove duplicate check structures or redundant checks

DO NOT make any other changes. DO NOT rename anything. DO NOT add Javadoc.

After editing, run: mvn -T 1C clean compile
Report any compilation errors.
```

**Gate:** `mvn -T 1C clean compile` passes. Git commit: `"Remove dead code and debug prints"`.

---

## Phase 2: Typo Fixes
* **Recommended LLM:** **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** (Fast & cost-effective for string replacements)
* **Risk:** Low (file-local) to Medium (public identifiers need caller updates).
* **Verification:** `mvn -T 1C clean compile`.

### Targeted Typos:
The complete, comprehensive list of 62 target files and their tasks is located in [phase2_typo_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase2_typo_fixes.md). Key examples include:
* `DEFAULT_FREQENCY` -> `DEFAULT_FREQUENCY` in `ControlSlidingDFT.java`
* `COMPILED_SUCCESSFULL` -> `COMPILED_SUCCESSFUL` in both nativec and javablock `CompileStatus.java` enums.
* `LIGTHGRAY` -> `LIGHTGRAY` in `GraferV3.java`
* `savegetFile` -> `safeGetFile` & `savegetFileName` -> `safeGetFileName` in `NativeCLibraryFile.java`
* `severeErrorOccured` -> `severeErrorOccurred` in `ControlNativeC.java`
* `SMALL_SIGNAL_ANALYIS` -> `SMALL_SIGNAL_ANALYSIS` in `I18nKeys`

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

### Agent prompt template:
```
TASK: Fix typos and rename identifiers in target files.
Read target files and tasks from phase2_typo_fixes.md.

For each file in the current batch:
1. For each identifier rename or typo fix task, FIRST run grep to find all references across the codebase (including src/main/, src/test/, and resources/*.properties).
2. Rename in all files.
3. CAUTION: If a renamed identifier is an I18nKeys constant or serialization token, also update any matching keys in resources/*.properties files. For serialization tokens, add the old name as a backward-compatible alias (see Phase 7).

After editing, run: mvn -T 1C clean compile
Report any compilation errors and list ALL files modified.
```

**Gate:** `mvn -T 1C clean compile`. Git commit: `"Fix typos in identifiers and comments"`.

---

## Phase 3: Magic Number Extraction
* **Recommended LLM:** **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** (Highly structured constant extraction)
* **Risk:** Low. Extract literal values into `public/private static final` constants.

### Key Target Files:
The complete, comprehensive list of 70 target files is located in [phase3_magic_numbers.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase3_magic_numbers.md). Examples include:
* `src/main/java/ch/technokrat/gecko/geckocircuits/scope/HiLoData.java` (Extract `1E30f` / `-1E30f` sentinels)
* `src/main/java/ch/technokrat/gecko/geckocircuits/newscope/HiLoData.java` (Extract `1E30f` sentinel)
* `src/main/java/ch/technokrat/gecko/geckocircuits/scope/GraferImplementation.java` (`1000` index encoding, `10000`, `0.6f` line widths)
* `src/main/java/ch/technokrat/gecko/geckocircuits/control/ControlSaveData.java` (`100` file count threshold)

### Agent prompt template:
```
TASK: Extract magic numbers into named constants in the target files.
Read target files and tasks from phase3_magic_numbers.md.

For each file in the current batch, apply only the magic-number/constant extraction tasks:
- Replace bare numeric literals or hardcoded index offsets with named constants at class level.
- Name constants in UPPER_SNAKE_CASE.
- DO NOT change any logic or behavior.

After editing, run: mvn -T 1C clean compile
```

**Gate:** `mvn -T 1C clean compile`. Git commit: `"Extract magic numbers into named constants"`.

---

## Phase 4: Assert-to-Exception Fixes
* **Recommended LLM:** **Gemini 3.5 Flash (High)** (Requires careful selection of Exception messages and imports)
* **Risk:** Medium. Changes runtime behavior when invalid input is given.
* **Verification:** `mvn -T 1C clean compile && mvn -T 1C test`.

### Targets:
The complete list of 8 target files is located in [phase4_assert_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase4_assert_fixes.md). It includes:
* `CompileStatus.java` -- `getFromOrdinal`: `assert false; return null` -> `throw new IllegalArgumentException("Invalid ordinal: " + ordinal);`
* `AxisLinLog.java` -- `getFromOrdinal`: same pattern.
* `Matrix.java` -- `solve()`: `assert false; return null` -> `throw new IllegalArgumentException("Matrix must be square to solve.");`
* `Clipping.java`, `ACosCalculator.java`, `ASinCalculator.java`, `MapList.java`, `MethodNameChecker.java`.

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test`. Git commit: `"Replace assert-false with IllegalArgumentException"`.

---

## Phase 5: New Test Cases (Safety Net)
* **Recommended LLM:** **Gemini 3.5 Flash (High)** (Strong reasoning required to construct comprehensive JUnit tests from scratch)
* **Risk:** None (additive only). High value. Adds coverage before bug fixing and refactoring.

### Test Template:
`src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbsCalculatorTest.java`

### Target Untested Classes:
1. **Math classes:**
   * `src/main/java/ch/technokrat/gecko/geckocircuits/math/LUDecomposition.java` (Test factorization & pivot conditions)
   * `src/main/java/ch/technokrat/gecko/geckocircuits/math/CholeskyDecomposition.java` (Test positive-definite checks)
   * `src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java` (Test solving and dimension assertions)
   * `src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/math/NComplex.java`
2. **Untested control calculators:**
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ABCDQCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/NothingToDoCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmControlCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PmsmModulatorCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SlidingDFTCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SmallSignalCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SpaceVectorCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SparseMatrixCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ThyristorControlCalculator.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ViewMotorCalculator.java`
3. **Data classes:**
   * `src/main/java/ch/technokrat/gecko/geckocircuits/scope/HiLoData.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/newscope/HiLoData.java`
   * `src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerSimple.java`

### Agent prompt template:
```
TASK: Write JUnit 5 tests for <ClassName>. Read the source file and the
existing test pattern at src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbsCalculatorTest.java for style reference.

Create a test class at src/test/java/<mirrored_path>/<ClassName>Test.java
Cover happy paths, edge cases (NaN, Infinities, negative, zero), and regression cases.

After writing, run: mvn test -Dtest="<ClassName>Test"
```

**Gate:** `mvn -T 1C test` and `mvn jacoco:report`. (Advisory: `mvn spotbugs:check` — review only NEW findings in test classes.) Git commit: `"Add unit test safety net for <scope>"`.

---

## Phase 6: Bug Fixes (Test-Driven)
* **Recommended LLM:** **Gemini 3.5 Flash (High)** (TDD requires analytical ability to trace failing test cases to exact fix logic)
* **Risk:** Medium-High. Each fix needs careful analysis.
* **Verification:** `mvn -T 1C clean compile && mvn -T 1C test` verifies the fixes against Phase 5 tests.

### Target Files List:
The complete, comprehensive list of 71 bug, logic, and safety fixes is located in [phase6_bug_fixes.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase6_bug_fixes.md). A few high-priority cases include:
* Short-circuiting operator bugs (`&` instead of `&&`) in `LUDecomposition.java`, `BigLUDecomposition.java`, and `CholeskyDecomposition.java`.
* Duplicate zoom conditions / self-comparisons in `FourierDiagramm.java` and `FourierKurvenRekonstruktion.java`.
* Incorrect pixel formula rendering in `FourierDiagramm.java` and `FourierKurvenRekonstruktion.java`.
* Shadowed field variables causing no-op self-assignments in `GraferImplementation.java`.
* NPE risks and missing null/infinity checks in `CompiledClassContainer.java`, `NativeCBlock.java`, and others.

### Agent prompt template:
```
TASK: Apply critical bug, logic, and safety fixes in a Test-Driven manner.
Read targets and tasks from phase6_bug_fixes.md.

For each file in the current batch:
1. Identify the bug details and target class.
2. Write a regression test in its corresponding test class (created/extended in Phase 5) that reproduces the bug.
3. Modify the source file to resolve the issue as described.
4. Verify the test now passes.

After writing the tests and code fixes, run: mvn test -Dtest="<TestClass>"
```

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test` all pass. (Advisory: `mvn spotbugs:check` — review only NEW findings in bug-fix files.) Git commit: `"Fix identified bugs using test-driven development"`.

---

## Phase 7: German-to-English Translations & Serialization Aliases
* **Recommended LLM:** **Gemini 3.5 Flash (High)** (Extremely risky refactoring; requires precise multi-file string replacements and alias mapping)
* **Risk:** High. Renaming serialized keys could break loading older schematic files.
* **Verification:** `mvn -T 1C clean compile && mvn -T 1C test` + GUI validation.

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
The complete, comprehensive list of 68 files requiring translations is located in [phase7_translations.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/improvement-reviews/targets/phase7_translations.md).

#### (A) Serialized Keys Needing Legacy Aliases:
* `anzXIN` -> `numberInputTerminals` (use `"anzXIN"` as a legacy identifier for backward compatibility)
* `anzYOUT` -> `numberOutputTerminals` (use `"anzYOUT"` as a legacy identifier)
* `COMPILED_SUCCESSFULL` -> `COMPILED_SUCCESSFUL` (used in serialization / persistent state)

#### (B) Plain Identifiers Needing Grep-and-Replace (No Aliases Required):
Key examples:
* `baueGuiIndividual` -> `buildIndividualGUI` (dialog subclasses)
* `baueGUI` -> `buildGUI`
* `istAngeklickt` -> `isClicked`
* `knotenIndex` -> `nodeIndex`
* `mausModus` -> `mouseMode`
* `_xKlick*` -> `_xClick*`
* `SaveModus` -> `SaveMode`
* `_drehzahl` -> `_rotationalSpeed` (in `AbstractMotor.java`)
* `_Ankerstrom` -> `_armatureCurrent` (in `AbstractMotorDC.java`)

*Note: Verify `I18nKeys.java` and `*.properties` resource bundles so that strings displayed in the GUI are correctly mapped without breaking translation keys.*

### Agent prompt template:
```
TASK: Rename target German variable/class/method identifiers to English.
Read targets and tasks from phase7_translations.md.

For each translation target:
1. Grep for all occurrences of the German name in the codebase.
2. Replace with the English name.
3. If category A (serialized keys/tokens), ensure UserParameter.java is modified to register the German name as an alias.
4. Run mvn -T 1C clean compile to verify the build.
```

**Gate:** `mvn -T 1C clean compile && mvn -T 1C test`. (Advisory: `mvn spotbugs:check` — review only NEW findings in renamed files.) Git commit: `"Translate German identifiers to English and add serialization aliases"`.

---

## Phase 8: Javadoc Documentation (Bulk)
* **Recommended LLM:** **DeepSeek V4 Flash** or **Gemini 3.5 Flash (Medium)** (Excellent candidate for saving tokens on massive boilerplate documentation)
* **Risk:** Very low. Documentation only.
* **Strategy:** Run in parallel batches of 50 files. Skip classes already documented in earlier phases.

### Agent prompt template (High Efficiency):
```
TASK: Add Javadoc only. Do NOT change any code.
Read target files and tasks from phase8_javadoc.md.

For each file:
1. Read the source file.
2. Add class-level Javadoc before the class/interface/enum declaration.
3. Add method Javadoc with @param/@return tags for public methods specified in phase8_javadoc.md.
4. DO NOT modify any code, imports, or existing comments.

After editing, run: mvn -T 1C clean compile
```

**Gate:** `mvn -T 1C clean compile` per batch. Git commit: `"Add Javadoc: <scope> (<N> files)"`.

---

## Summary: AI Session Budget

| Phase | Batches | Files/batch | Est. tokens/batch | Total est. tokens |
|-------|---------|-------------|-------------------|-------------------|
| 1: Dead code | 7 | 20 | 25K | 175K |
| 2: Typos | 3 | 20 | 30K | 90K |
| 3: Magic numbers | 3 | 20 | 25K | 75K |
| 4: Assert fixes | 1 | 8 | 15K | 15K |
| 5: New test cases | 2 | 13 | 45K | 90K |
| 6: Bug fixes | 4 | 20 | 40K | 160K |
| 7: German->English | 4 | 20 | 40K | 160K |
| 8: Javadoc | 16 | 50 | 35K | 560K |
| **Total** | **40** | | | **~1.3M** |

---

## Execution Checklist

```
[x] Phase 1: Dead code removal         (7 batches)   mvn -T 1C clean compile
[x] Phase 2: Typo fixes                (3 batches)   mvn -T 1C clean compile
[x] Phase 3: Magic number extraction   (3 batches)   mvn -T 1C clean compile
[x] Phase 4: Assert -> exception       (1 batch)     mvn -T 1C clean compile && mvn -T 1C test
[x] Phase 5: New test cases            (2 batches)   mvn -T 1C test && mvn jacoco:report
[ ] Phase 6: Bug fixes + regression    (4 batches)   mvn -T 1C clean compile && mvn -T 1C test
[ ] Phase 7: German -> English         (4 batches)   mvn -T 1C clean compile && mvn -T 1C test
[ ] Phase 8: Javadoc (bulk)            (16 batches)  mvn -T 1C clean compile
[ ] Final: Full verification                         mvn -T 1C clean verify
```
