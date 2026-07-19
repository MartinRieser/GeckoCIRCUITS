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
- [ ] #21 `ControlSlidingDFT.java:254` — delete duplicate commented `return`  (**START HERE**)
- [ ] #14 `GeneralPathWrapper.paintSymbols` — delete empty stub
- [ ] #15 `ControlJavaFunction._doDebug` — delete commented field
- [ ] #19 `DataTablePanelParameters.getCheckedData()` — delete dead method
- [ ] #20 `DialogSmallSignalAnalysis` — delete dead comments
- [ ] #13 `NodeLabel` — delete dead class + whitelist entry in `CorePackageValidationTest.java:97`

### Phase B — MEDIUM, mechanical (no decision needed)
- [ ] #6 Migrate ~3 call sites to `*Rgb()` variants, then remove deprecated methods
- [ ] #12 Align misleading `IGNORED:` Javadoc in `GeckoRemoteTest` with reality

### Phase C — MEDIUM, needs a decision (decide *before* coding)
For each: pick **delete** or **wire-up**, then execute.
- [ ] #5  `NativeCBlock` native-library unload reflection
- [ ] #7  `SimulationStateListener` orphan SPI
- [ ] #8  `UserParameterGUIAdapter` orphan adapter
- [ ] #9  `DialogExternalStorageConverter` orphan implementation
- [ ] #11 `AxisLimits` disabled user-scale override on import
- [ ] #10 `ControlOSZI` terminal rename propagation (also a latent bug — write a test)

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
mvn test result: only the 4 known CircuitIntegrationTest failures
```

---

## 4. Workflow per item

1. `git checkout -b cleanup/<n>-short-name`
2. Read the cited file(s) and surrounding context.
3. Make the change (edit, no extra refactors).
4. `mvn test` — confirm only the 4 known failures remain.
5. `git add -p && git commit` with the checklist as commit body.
6. Optionally open a PR for review.

For **bug fixes** (Phase C/D): step 2.5 is *write a failing test that reproduces the
bug*. Commit the test first (red), then the fix (green), in two commits on the same
branch.

---

## 5. Stop conditions

- Any new test failure ⇒ stop, investigate, do not commit until explained.
- Any item that touches more than ~5 files ⇒ split or ask for review before commit.
- Any "decide" item (Phase C/D) ⇒ do **not** guess; surface the question first.
