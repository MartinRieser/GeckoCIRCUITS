# GeckoCIRCUITS Modernization Plan

**Goal:** Ensure long-term maintainability for future OS and Java versions
**Context:** Academic usage, open source project
**Approach:** Incremental, non-breaking improvements

---

## Executive Summary

This plan focuses on **maintainability** over aesthetics. The codebase works correctly but lacks modern development practices that make it difficult for:
- Future maintainers to update Java versions
- New contributors to make safe changes
- Academic users to trust simulation results
- Automated testing across different OS/JDK combinations

**Total Estimated Effort:** 3-6 months (part-time)

---

## Phase 1: Critical Foundation (4-6 weeks)

### 1.1 Modernize Build Configuration ⭐ **HIGH PRIORITY** ✅ **COMPLETED**

**Problem:** Maven build had outdated configuration and manual plugin version management.

**Status:** ✅ **Completed**. The build configuration has been modernized to use **Java 21 (LTS)** as the target/source release. Dependency versions (JUnit 5.11.3, Log4j 2.24.3, GraalVM 24.1.1) are centralized under `<properties>` in [pom.xml](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/pom.xml). 

**Benefits:**
- Centralized dependency version properties make upgrades trivial.
- Enabled standard JUnit 5 testing infrastructure.

---

### 1.2 Fix Critical Security Vulnerability ⭐ **CRITICAL** ✅ **COMPLETED**

**Problem:** Legacy Log4j 1.2.17 library had known security vulnerabilities (CVE-2011-3389).

**Status:** ✅ **Completed**. The logging library was upgraded to Log4j 2.x, and all references across the codebase were migrated from `org.apache.log4j.Logger` to `org.apache.logging.log4j.Logger` / `LogManager`.

---

### 1.3 Add Warning-Free Policy for New Code ✅ **COMPLETED**

**Problem:** All warnings were historically suppressed globally, making it easy to introduce new warnings/type-safety issues.

**Status:** ✅ **Completed**. A massive warning cleanup campaign was performed in commits `d07b6b1` and `7e8ae47`, fixing compiler warnings, type safety, serialization issues (adding `serialVersionUID` and marking fields `transient` where appropriate) across hundreds of files. The following policy and configuration files were created:
- [`.editorconfig`](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/.editorconfig) — Ensures consistent indentation (4 spaces), UTF-8 encoding, LF line endings, and trailing whitespace trimming across all editors and IDEs.
- [`WARNING_POLICY.md`](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/WARNING_POLICY.md) — Documents the warning-free policy for new code, `@SuppressWarnings` annotation rules, legacy warning exemptions (remote interface layer, scripting API, legacy math structures, Swing dialogs), and contributor workflow guidelines.

---

## Phase 2: Maintainability Infrastructure (3-5 weeks)

### 2.1 Automated Testing Pipeline ⭐ **HIGH PRIORITY** ✅ **COMPLETED**

**Problem:** No automated testing was present, relying on manual execution.

**Status:** ✅ **Completed**. A single matrix-based GitHub Actions workflow `.github/workflows/build-test.yml` was created. It runs on `ubuntu-latest`, `windows-latest`, and `macos-latest` using JDK 21. It builds native JNI libraries, executes the Maven test suite, packages the application, and creates native platform installers via `jpackage`.

---

### 2.2 Test Coverage Metrics 🔄 **IN PROGRESS / POSTPONED**

**Problem:** Low test coverage makes it hard to gauge impact of refactoring.

**Status:** 🔄 **In Progress**. The `jacoco-maven-plugin` is integrated into the build plugins section of [pom.xml](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/pom.xml) to generate coverage reports, but code coverage thresholds are not currently enforced as a blocking gate on the CI pipeline.

---

### 2.3 Fix Excluded Tests ✅ **COMPLETED**

**Problem:** Previously, several tests were excluded or ignored due to NetBeans GUI dependencies.

**Status:** ✅ **Completed**. The test suite has been cleaned up. All 159 tests compile and pass successfully on CI/local builds (0 failures, 0 skipped). The only remaining `@Ignore` annotations are appropriately set on abstract test base classes (such as `AbstractTwoInputsMathFunctionTest`) to prevent JUnit from attempting to run them directly.

---

## Phase 3: Developer Experience Improvements (2-4 weeks)

### 3.1 Developer Documentation ✅ **COMPLETED**

**Problem:** Missing standard onboarding documentation for new contributors.

**Status:** ✅ **Completed**. The [DEVELOPMENT.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/DEVELOPMENT.md) guide has been created, covering environment setup, build commands, testing, code style, architecture, IDE configuration, CI/CD pipeline, native JNI libraries, i18n, GUI development with `.form` files, debugging, contribution workflow, and troubleshooting. Basic setup and GUI designing instructions also remain in the main [README.md](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/README.md).

---

### 3.2 Issue and PR Templates ❌ **NOT STARTED**

**Problem:** Contributor issues/PRs lack standard formats, causing slower triage times.

**Status:** ❌ **Not Started**. `.github/ISSUE_TEMPLATE.md` and `.github/PULL_REQUEST_TEMPLATE.md` files still need to be created.

---

### 3.3 Code Quality Tools ✅ **COMPLETED**

**Problem:** Code quality guidelines are hard to enforce without automated tooling.

**Status:** ✅ **Completed**. The `spotbugs-maven-plugin`, `maven-checkstyle-plugin`, and `maven-pmd-plugin` have been integrated into [pom.xml](file:///c:/Users/mhr/Documents/GeckoCIRCUITS/pom.xml) under the plugin management section.

---

## Phase 4: Incremental Code Modernization (Ongoing)

### 4.1 Gradual Warning Removal 🔄 **IN PROGRESS**

**Problem:** Standard Java warning patterns were suppressed globally.

**Status:** 🔄 **In Progress**. Major bulk refactoring has resolved thousands of compiler warnings, type safety issues, serialization problems, and modernized codebase patterns across 298+ files (commits `d07b6b1` and `7e8ae47`). Future modifications will continue to clean up remaining issues as they are touched.

---

### 4.2 Dependency Updates Strategy ❌ **NOT STARTED**

**Problem:** Dependencies can accumulate security vulnerabilities or become outdated over time.

**Status:** ❌ **Not Started**. The scheduled GitHub workflow (`dependency-check.yml`) has not been set up.

---

## Phase 5: Future Java Version Readiness (Ongoing)

### 5.1 Test with Java 23/24 Early ❌ **NOT STARTED**

**Goal:** Ensure compatibility with future JDK releases.

**Status:** ❌ **Not Started**. Testing on Java 23/24 early access or regular builds has not been added to the CI matrix yet.

---

### 5.2 Prepare for Module System (Optional) ⏭️ **DEFERRED**

**Goal:** Migrate to Java Platform Module System (JPMS).

**Status:** ⏭️ **Deferred (Optional)**. The project continues to run on classpath mode in Java 21, which is adequate for academic and desktop usage.

---

## Things NOT to Do (Avoid These)

### ❌ Do NOT Rewrite UI to JavaFX

**Reasons:**
- Swing is stable, works on all platforms
- Academic users don't need modern UI
- JavaFX adds complex dependency graph
- 1000+ files to rewrite (high risk)

**Better:** Keep Swing, improve dialogs only if users complain

---

### ❌ Do NOT Microservice Architecture

**Reasons:**
- Academic use is single-user simulation
- Networked simulation not a requirement
- Adds unnecessary complexity

**Better:** Keep monolithic desktop app

---

### ❌ Do NOT Full Test Rewrite

**Reasons:**
- Tests work (when not NetBeans-specific)
- 1000+ test cases, high risk of regression
- Opportunity cost too high

**Better:** Add new tests for bug fixes/features, gradually improve coverage

---

### ❌ Do NOT "Fix All Warnings" Campaign

**Reasons:**
- 1000+ warnings, takes months
- High chance of introducing bugs
- Academic focus is on correctness, not style

**Better:** Fix warnings incrementally as part of normal development (Phase 4.1)

---

## Implementation Roadmap

| Phase / Task | Effort Estimate | Actual Status | Notes / Blocked By |
|--------------|-----------------|---------------|--------------------|
| **Phase 1: Critical Foundation** | | | |
| 1.1 Build Configuration | 3-5 days | ✅ **COMPLETED** | Configured for Java 21 LTS |
| 1.2 Log4j Migration | 5-7 days | ✅ **COMPLETED** | Migrated fully to Log4j 2.x |
| 1.3 Warning Policy | 1-2 days | ✅ **COMPLETED** | `.editorconfig` and `WARNING_POLICY.md` created |
| **Phase 2: Maintainability Infrastructure** | | | |
| 2.1 CI/CD Pipeline | 2-3 days | ✅ **COMPLETED** | Consolidated `build-test.yml` matrix |
| 2.2 Test Coverage | 1-2 days | 🔄 **IN PROGRESS** | JaCoCo added to `pom.xml`, not blocking yet |
| 2.3 Fix Excluded Tests | 3-5 days | ✅ **COMPLETED** | All 159 tests passing, no exclusions |
| **Phase 3: Developer Experience** | | | |
| 3.1 Development Guide | 2-3 days | ✅ **COMPLETED** | `DEVELOPMENT.md` created with full onboarding guide |
| 3.2 Issue/PR Templates | 1 day | ❌ **NOT STARTED** | GitHub templates pending |
| 3.3 Code Quality Tools | 2-3 days | ✅ **COMPLETED** | SpotBugs/Checkstyle/PMD added to pom |
| **Phase 4: Incremental Modernization** | | | |
| 4.1 Gradual Warning Removal | Ongoing | 🔄 **IN PROGRESS** | Thousands of warnings resolved in bulk |
| 4.2 Dependency Updates | Monthly | ❌ **NOT STARTED** | Security workflow pending |
| **Phase 5: Future Readiness** | | | |
| 5.1 Test New Java Versions | Per release | ❌ **NOT STARTED** | Matrix does not test JDK 23/24 yet |
| 5.2 Module System (Optional) | 4-8 weeks | ⏭️ **DEFERRED** | Deferred as it is optional |

---

## Success Metrics

Track progress with these metrics:

### Code Quality
- [/] Build passes on Java 21, 23, 24 (Currently passing on Java 21)
- [x] CI pipeline green on all OS (Windows, Linux, Mac)
- [x] Zero critical security vulnerabilities (Log4j 1.x removed)
- [/] Test coverage ≥ 40% (JaCoCo reports enabled but coverage is still low)
- [x] Number of compiler warnings decreasing (Bulk resolution complete)

### Maintainability
- [x] New contributor can build in 15 minutes (Documented in README.md)
- [ ] PR template used for all changes
- [x] Development guide is accurate (`DEVELOPMENT.md` created)
- [x] Build configuration changes don't break existing workflows

### Community
- [ ] Issues with templates receive faster responses
- [ ] PRs reviewed within 2 weeks
- [ ] Contributions from non-original authors

---

## Quick Start / Completed History

### Phase 1 & 2 Milestones
1. [x] Create consolidated GitHub Actions matrix workflow (2.1)
2. [x] Migrate Log4j to 2.x (1.2)
3. [x] Upgrade JUnit dependency and centralize pom.xml properties (1.1)
4. [x] Integrate SpotBugs, PMD, Checkstyle, and JaCoCo plugins (3.3 / 2.2)
5. [x] Fix test exclusions to ensure all 159 tests pass cleanly (2.3)
6. [x] Massive bulk resolution of compiler warnings and type safety issues (4.1)
7. [x] Add `.editorconfig` and `WARNING_POLICY.md` for warning-free policy (1.3)

### Remaining Tasks
1. [x] Add `.editorconfig` (1.3)
2. [x] Add guidelines/warning policy to `WARNING_POLICY.md` (1.3)
3. [x] Create `DEVELOPMENT.md` (3.1)
4. [ ] Create issue/PR templates (3.2)
5. [ ] Set up automated dependency security checks (4.2)
6. [ ] Add future JDKs (Java 23/24) to the CI build matrix (5.1)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|-------|-------------|--------|------------|
| Log4j migration breaks logging | Medium | High | Thorough testing, gradual roll-out |
| CI flaky on Windows | Medium | Low | Test locally, use matrix builds |
| Tests pass but bugs exist | Low | High | Gradual coverage increase |
| Community resistance to changes | Low | Medium | Communicate benefits, solicit feedback |

---

## Post-Modernization Maintenance

Once phases complete, maintain with:

**Weekly:**
- Review PRs
- Fix bugs reported by users
- Incremental warning cleanup

**Monthly:**
- Dependency security check
- Review test coverage trends
- Check new Java EA releases

**Quarterly:**
- Update dependencies
- Review CI pipeline performance
- Gather community feedback

**Annually:**
- Evaluate new Java LTS for upgrade
- Architecture review (any big changes needed?)
- Update documentation

---

## Conclusion

This plan prioritizes **low-risk, high-value changes** that:
- ✅ Enable future Java version upgrades
- ✅ Make the codebase safer for contributions
- ✅ Maintain academic usability
- ✅ Improve long-term maintainability
- ✅ Respect open source community dynamics

**Total Investment:** 3-6 months (part-time)
**Expected Lifetime:** 10+ years of maintainability

---

## Related Documentation

- `DEVELOPMENT.md` - Developer onboarding guide (Phase 3.1)
- `WARNING_POLICY.md` - Current warning suppression rationale
- `README.md` - User-facing documentation
- `CLAUDE.md` - Architecture documentation
- `WARNING_FIX_GUIDE.md` - Systematic warning cleanup guide (if needed later)
