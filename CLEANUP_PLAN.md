# Cleanup Work Plan

Companion to `CLEANUP_TODO.md`. Captures the *process* for working through the 21
items so each fix is reviewable, testable, and revertable independently.

---

## 0. Baseline (one-time)

Run `mvn test` on the clean tree. Recorded baseline (2026-07-19):

- **5378 tests, 0 failures, 0 errors, 0 skipped** — fully green.
- The `CircuitIntegrationTest` failures mentioned in `CLEANUP_TODO.md` have been
  fixed at some point; they no longer reproduce.

**Rule:** after every fix, `mvn test` must show **0 failures, 0 errors**. Any new
failure is the fix's responsibility — investigate before committing.

---

## 1. Golden rule

**One item → one branch → one commit.**

Branch naming: `cleanup/<n>-short-name` (e.g. `cleanup/21-sliding-dft-dead-comment`).

Benefits:
- Independent review (one change at a time = easy to understand).
- Trivial `git revert` if a fix is wrong.
- Clean PR-per-item if discussion is needed.

---

## 2. Recommended order (easiest → hardest)

### Phase A — LOW priority, pure deletions (workflow shakedown)
- [x] #21 `ControlSlidingDFT.java:254` — delete duplicate commented `return`  → `cleanup/21-sliding-dft-dead-comment` @ 0ad20070
- [x] #14 `GeneralPathWrapper.paintSymbols` — delete empty stub  → `cleanup/14-generalpathwrapper-paintsymbols-stub` @ 28459dba
- [x] #15 `ControlJavaFunction._doDebug` — delete commented field  → `cleanup/15-controljavafunction-dodebug` @ 7f1e0cb3
- [x] #19 `DataTablePanelParameters.getCheckedData()` — delete dead method  → `cleanup/19-datatablepanelparameters-getcheckeddata` @ 6eb30a58
- [x] #20 `DialogSmallSignalAnalysis` — delete dead comments  → `cleanup/20-dialogsmallsignalanalysis-dead-comments` @ b46e7396
- [x] #13 `NodeLabel` — delete dead class + whitelist entry in `CorePackageValidationTest.java:97`  → `cleanup/13-nodelabel-dead-class` @ d32730f0

### Phase B — MEDIUM, mechanical (no decision needed)
- [x] #6 Migrate ~3 call sites to `*Rgb()` variants, then remove deprecated methods  → `cleanup/06-connectortype-deprecated-color-methods` @ d9271f21
- [x] #12 Align misleading `IGNORED:` Javadoc in `GeckoRemoteTest` with reality  → `cleanup/12-geckoremotetest-javadoc` @ cb4221bc

### Phase C — MEDIUM, needs a decision (decide *before* coding)
For each: pick **delete** or **wire-up**, then execute.
- [x] #5  `NativeCBlock` native-library unload reflection  → `cleanup/05-nativecblock-copy-on-load` @ 7114bd53
- [x] #7  `SimulationStateListener` orphan SPI  → `cleanup/07-simulationstatelistener-wire-up` @ 4e089753
- [x] #8  `UserParameterGUIAdapter` orphan adapter  → `cleanup/08-userparameterguiadapter-orphan` @ 1709c237
- [x] #9  `DialogExternalStorageConverter` orphan implementation  → `cleanup/09-dialogexternalstorageconverter-orphan` @ b8f6b0e5
- [x] #11 `AxisLimits` disabled user-scale override on import  → `cleanup/11-axislimits-user-scale-import` @ 09213753
- [x] #10 `ControlOSZI` terminal rename propagation  → `cleanup/10-controloszi-orphan-methods` @ 00caf948

### Phase D — HIGH priority, real bugs and big refactors (last)
For each bug: **write a failing regression test first**, then make it pass.
- [ ] #3  `ProjectData.java:357` — restore `dataContainerSignals[]` on file load
- [ ] #4  `StateSpaceCalculator.initializeWithNewDt` — confirm/fix dt-change handling
- [ ] #1  Orphan API interfaces (`IMainWindow`, `ICircuitSheet`, `ICircuitEditor`) — decide complete vs delete
- [ ] #2  Finish `scope/` → `newscope/` migration, then delete legacy package

---

## 3. Per-item checklist

Paste into each commit body so the change is self-documenting:

```
Item: CLEANUP_TODO #<n>
Files touched: <list>
Behaviour change: yes / no
Test added/updated: <name or "none — pure deletion">
mvn test result: 0 failures, 0 errors (matches baseline of 5378 run / 0 fail / 0 err / 0 skip)
```

---

## 4. Workflow per item

1. `git checkout -b cleanup/<n>-short-name`
2. Read the cited file(s) and surrounding context.
3. Make the change (edit, no extra refactors).
4. `mvn test` — confirm 0 failures, 0 errors (matches baseline).
5. `git add -A && git commit` with the checklist as commit body.
6. Optionally open a PR for review.

For **bug fixes** (Phase C/D): step 2.5 is *write a failing test that reproduces the
bug*. Commit the test first (red), then the fix (green), in two commits on the same
branch.

---

## 5. Stop conditions

- Any new test failure ⇒ stop, investigate, do not commit until explained.
- Any item that touches more than ~5 files ⇒ split or ask for review before commit.
- Any "decide" item (Phase C/D) ⇒ do **not** guess; surface the question first.

---

## 6. Lessons learned (process notes)

These are pitfalls hit while running Phase A. Read before starting a new item.

### 6.1 Never run two git-mutating bash calls in parallel

`git commit`, `git checkout`, and `git checkout -b` all take `.git/index.lock`. If
two of them race, one will fail with `fatal: Unable to create '.git/index.lock'`.
Worse: a `git checkout -b new-branch` issued in parallel with a `git commit` on
the *old* branch can land the commit on the wrong branch (happened during Phase A
— had to repoint both branches with `git branch -f`).

**Rule:** chain git-mutating steps with `&&` *inside a single* bash call, or run
the bash calls **sequentially** (one message, one bash block, wait for result).
Read-only commands (`git log`, `git status`, `git diff`, `grep`, `mvn test`) may
stay in parallel.

### 6.2 Ignore ERROR/WARN log lines during `mvn test`

Tests that exercise error paths (e.g. `DataContainerCompressableErrorPathsTest`,
20 tests, all pass) intentionally produce ERROR/WARN log output as the code-under-test
does its job. The console noise is meaningless. **Trust only**:

- the per-class `Tests run: N, Failures: F, Errors: E, Skipped: S` lines in
  `target/surefire-reports/*.txt`, and
- the aggregate `mvn` build result line (`BUILD SUCCESS` / `BUILD FAILURE`).

Quick summary after a run:

```
grep -hE "^Tests run" target/surefire-reports/*.txt \
  | awk '{r+=$3; f+=$5; e+=$7; s+=$9} END {print "run="r" fail="f" err="e" skip="s}'
```

### 6.3 Verify "no callers" before deleting

Every deletion in Phase A was preceded by a `grep` for the symbol across the whole
codebase to confirm zero live callers. The TODO file is reliable but not infallible
— re-verify each claim with a fresh search before editing.
