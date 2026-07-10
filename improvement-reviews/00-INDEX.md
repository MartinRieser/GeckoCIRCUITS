# GeckoCIRCUITS Java Improvement Review Index

## Overview
This directory contains per-file improvement tasks for all 791 main source `.java` files
in the GeckoCIRCUITS project. Each file was reviewed for possible improvements with
**limited impact to other files**. The primary focus is on:

1. **Missing Javadoc/inline documentation** on classes, methods, and parameters
2. **Minor code quality** improvements (magic numbers, null checks, dead code)
3. **Naming/clarity** issues (German names, typos, unclear identifiers)
4. **Dead/commented-out code** removal candidates
5. **Bug risks** identified during review

## File Index

| File | Package | Files Covered |
|------|---------|--------------|
| `01-com-intel-mkl.md` | com/intel/mkl/ | 3 |
| `02-expressionscripting.md` | expressionscripting/ | 5 |
| `03-modelviewcontrol.md` | modelviewcontrol/ | 9 |
| `04-gecko-root.md` | gecko/ (root) | 32 |
| `05-circuit-part1.md` | circuit/ (A-M) | 46 |
| `06-circuit-part2.md` | circuit/ (N-Z) | 46 |
| `07-losscalculation.md` | circuit/losscalculation/ | 22 |
| `08-circuitcomponents.md` | circuit/circuitcomponents/ | 138 |
| `09-control-part1.md` | control/ (A-ControlPTDialog) | 84 |
| `10-control-part2.md` | control/ (ControlTAN-VariableTerminalNumber) | 84 |
| `11-calculators.md` | control/calculators/ | 73 |
| `12-misc-packages.md` | javablock/ + datacontainer/ + scope/ + math/ + nativec/ + geckoscript/ + general/ + i18n/ | ~210 |
| `13-newscope.md` | newscope/ | 99 |
| **Total** | | **791** |

## Cross-Cutting Themes

These issues appear across many files:

1. **Missing Javadoc** (nearly universal) -- class-level and method-level documentation is absent in most files
2. **Magic parameter array indices** (`parameter[n]`) without named constants -- affects ~40 component/calculator files
3. **German comments/variable names** mixed with English -- ~50+ files (`drehzahl`, `verluste`, `Ankerstrom`, `zeichne`, etc.)
4. **Commented-out debug `System.out.println`** calls -- ~20+ files
5. **Static mutable state** as global flags -- ~10 calculator files
6. **Typos** in method names, identifiers, and comments -- ~20+ files
7. **Dead/commented-out code blocks** -- ~30+ files
8. **Assert-based error handling** (`assert false; return null;`) risks NPEs in production -- ~10 files
9. **Legacy `java.util.Stack`** usage instead of `ArrayDeque` -- ~5 files
