---
name: systematic-code-review
description: Comprehensive runbook for conducting systematic, layer-by-layer source code reviews with Javadoc documentation, elimination of magic numbers, duplication removal, and maximizing unit test coverage.
---

# Systematic Codebase Review Methodology

Use this skill when tasked with reviewing, refactoring, documenting, or maximizing test coverage across a module or subsystem (backend Java or frontend TypeScript).

## The 6 Core Quality Invariants
Every source file reviewed must satisfy all six invariants:
1. **Well Documented**: Comprehensive Javadoc (backend) or TSDoc (frontend) on every exported/public class, interface, method, parameter, return value, exception, and enum.
2. **Zero Magic Numbers**: Centralize numeric literals (tolerances, port IDs, orientation numbers, step counts, timeouts) and sentinel strings in dedicated enums or constants classes (e.g. `Constants.java`, `constants.ts`).
3. **General Solutions**: Replace fragile heuristics (such as name-prefix string checking or ad-hoc workarounds) with strongly typed domain contracts and component schemas.
4. **No Unfinished Implementations**: Implement complete error handling, edge cases, and all downstream consumer integrations.
5. **Zero Duplication**: Deduplicate shared logic into common domain utilities immediately.
6. **Maximum Test Coverage**: Write granular unit tests targeting >90% statement/branch coverage with 100% pass rate.

---

## Review Workflow for GeckoCIRCUITS Backend

### Step 1: Save the Review Plan
Before editing code, formulate a structured, phased plan and persist it to a markdown file in the project root (e.g. `BACKEND_REVIEW_PLAN.md`):
- Phase 0: Baseline & Coverage Infrastructure (JaCoCo / Maven setup).
- Phase 1: Core Domain Models, Types & Constants (`gecko-simulation-core/model`).
- Phase 2: Numerics, Solvers & Calculation Engine (`gecko-simulation-core/solver`).
- Phase 3: Serialization, Parsers & Schemas (`.ipes` parser, serialization).
- Phase 4: API & Service Layer (`gecko-rest-api`, Javalin routes, WebSocket).
- Phase 5: MCP & Integration Tools (`gecko-mcp`).
- Phase 6: Final Verification & Coverage Audit.

### Step 2: Establish Coverage Baseline (Phase 0)
Run Maven test and coverage commands:
```powershell
# Run backend tests and verify baseline
mvn test
# Or generate JaCoCo report if configured
mvn jacoco:report
```
Record initial passed test counts and baseline coverage in the plan document.

### Step 3: Layer-by-Layer Execution (Phases 1..N)
For each file in the active phase:
1. **Read & Understand**: Inspect existing imports, constants, methods, and test coverage.
2. **Audit Constants**: Extract magic numbers into `*Constants.java` or typed `enum`s.
3. **Document**: Add Javadoc describing architectural context, physical electrical units, `@param`, `@return`, and `@throws`.
4. **Refactor & Deduplicate**: Extract duplicated calculations or parser helpers into reusable shared utilities.
5. **Author Tests**: Create or expand JUnit 5 tests (using AssertJ, Mockito if needed). Cover nominal cases, boundary conditions, edge cases, and invalid inputs.

### Step 4: Verification & Atomic Commit Gate
At the end of each phase:
1. **Compile Check**: `mvn test-compile` — must have **0 javac warnings**.
2. **Test Run**: `mvn test` — **100% pass rate, 0 failures, 0 errors**.
3. **Plan Update**: Check off completed items in `BACKEND_REVIEW_PLAN.md`.
4. **Git Commit**: Commit with semantic title:
   ```powershell
   git add src; git commit -m "refactor(backend): review [subsystem] (phase N)"
   ```
