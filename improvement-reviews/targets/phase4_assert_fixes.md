# Phase 4: Assert-to-Exception Fixes

This file lists all the target files and specific tasks for Phase 4: Assert-to-Exception Fixes parsed from the review files.

Total target files: 8

## File and Task List

### [ACosCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ACosCalculator.java)
- Fix contradictory assert messages ("must be <= -PI/2" for upper bound check)

### [ASinCalculator.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ASinCalculator.java)
- Fix contradictory assert messages

### [AxisLinLog.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/AxisLinLog.java)
- Replace `assert false; return null;` in `getFromOrdinal` (line 36) with `throw new IllegalArgumentException` -- asserts can be disabled at runtime

### [Clipping.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/newscope/Clipping.java)
- Fix `assert false; return null;` pattern (risks NPE)

### [CompileStatus.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/javablock/CompileStatus.java)
- **Bug: `assert false; return null;`** in `getFromOrdinal` (lines 26-27). If assertions are disabled, this silently returns `null`. Replace with `throw new IllegalArgumentException`

### [MapList.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/circuit/MapList.java)
- Add Javadoc to `removeAll()` explaining `assert false` (intentionally unsupported)

### [Matrix.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Matrix.java)
- Fix `solve()` method (line 740): replace `assert false` with an explicit exception throw -- asserts can be disabled at runtime, causing silent `null` returns

### [MethodNameChecker.java](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/MethodNameChecker.java)
- Add Javadoc to `checkFabric()` explaining the assertion-based validation strategy

