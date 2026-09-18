# GeckoCIRCUITS Releases

Complete history of GeckoCIRCUITS releases with download links, release notes, and changelogs.

---

## Current Release

The latest stable release is **v3.0.0** (released February 2026).

[Download v3.0.0](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v3.0.0){ .md-button .md-button--primary }
[View Release Notes](./3000.md){ .md-button }

---

## Version History

### v3.x Series — Major Platform Release

#### [v3.0.0](./3000.md) — Complete REST API Platform (Major Milestone)

**Released:** 2026-02-18

Consolidates all v2.18.0–v2.22.0 features into a production-ready web platform with 32 comprehensive REST & WebSocket endpoints, headless core segregation, and 7,426 automated tests.

[Release Notes](./3000.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v3.0.0){ .md-button }

---

### v2.x Series — Modern API & Core Development

#### [v2.22.0](./2220.md) — Security & Authentication

**Released:** 2026-02-17

Opt-in API key security model (`X-API-Key`) with Spring Security integration and configurable access control.

[Release Notes](./2220.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.22.0){ .md-button }

---

#### [v2.21.0](./2210.md) — Signal Analysis Endpoints

**Released:** 2026-02-17

Automated signal characteristics, Fourier harmonic decomposition (THD), and RMS calculations via dedicated REST endpoints.

[Release Notes](./2210.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.21.0){ .md-button }

---

#### [v2.20.0](./2200.md) — Real-Time Streaming & WebSockets

**Released:** 2026-02-16

Server-Sent Events (SSE) progress streaming, STOMP over WebSocket, and raw WebSocket support for high-throughput live simulation data.

[Release Notes](./2200.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.20.0){ .md-button }

---

#### [v2.19.0](./2190.md) — Batch Simulations & Parameter Sweeps

**Released:** 2026-02-16

Multi-run simulation queue, linear and logarithmic parameter sweeps, concurrent batch management, and cancellation endpoints.

[Release Notes](./2190.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.19.0){ .md-button }

---

#### [v2.18.0](./2180.md) — Circuit Cloning & Parameter Overrides

**Released:** 2026-02-15

In-memory circuit cloning, pre-simulation parameter overrides via `ParameterOverrideApplicator`, and non-destructive experimentation.

[Release Notes](./2180.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.18.0){ .md-button }

---

#### [v2.17.0](./2170.md) — Release Automation & Desktop Packaging

**Released:** 2026-02-15

Automated native packaging workflows for Windows (MSI/portable), macOS (DMG/app), and Linux (DEB/RPM).

[Release Notes](./2170.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.17.0){ .md-button }

---

#### [v2.16.0](./2160.md) — REST API Launch (Sprint 5)

**Released:** 2026-02-14

Initial release of the Spring Boot REST API service with simulation lifecycle management and circuit execution.

[Release Notes](./2160.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.16.0){ .md-button }

---

#### [v2.15.0](./2150.md) — Loss Calculation Migration (Sprint 4b)

**Released:** 2026-02-14

Thermal and semiconductor loss calculation algorithms extracted into headless, testable core services.

[Release Notes](./2150.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.15.0){ .md-button }

---

#### [v2.14.0](./2140.md) — GeckoFile Migration (Sprint 4a)

**Released:** 2026-02-14

Modernized file parser and serializer for `.ipes` circuit models, enabling programmatic model inspection.

[Release Notes](./2140.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.14.0){ .md-button }

---

#### [v2.13.0](./2130.md) — Terminal and Component Package Migration

**Released:** 2026-02-13

Component terminal management and netlist connectivity decoupled from classic UI layers.

[Release Notes](./2130.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.13.0){ .md-button }

---

#### [v2.12.0](./2120.md) — Static Analysis & Code Quality Sprint

**Released:** 2026-02-12

Comprehensive SpotBugs clean-up, Checkstyle enforcement, and initial unit test harness expansion.

[Release Notes](./2120.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.12.0){ .md-button }

---

#### [v2.11.0](./2110.md) — Core Module Foundation

**Released:** 2026-02-12

Creation of the standalone `gecko-simulation-core` Maven module, isolating MNA matrix stampers and numerical solvers.

[Release Notes](./2110.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.11.0){ .md-button }

---

#### [v2.10.0](./2100.md) — Java 21 Migration (GROUND ZERO)

**Released:** 2025-08-24

Initial baseline porting the heritage GeckoCIRCUITS codebase to modern Java with automated Maven builds.

[Release Notes](./2100.md){ .md-button }
[Download](https://github.com/MartinRieser/GeckoCIRCUITS/releases/tag/v2.10.0){ .md-button }

---

## Upstream Lineage

| Version | Source | Description |
|---------|--------|-------------|
| v2.03-spotbugs-clean | This Fork | All 1,096 SpotBugs violations fixed to zero |
| v2.04-repo-reorg | This Fork | Repository reorganization with modern JDK workflows |
| v2.02 | geckocircuits/GeckoCIRCUITS | Last legacy upstream release |

---

## Support

- **Documentation:** [MartinRieser.github.io/GeckoCIRCUITS](https://MartinRieser.github.io/GeckoCIRCUITS/)
- **GitHub Issues:** [Report bugs or request features](https://github.com/MartinRieser/GeckoCIRCUITS/issues)
- **GitHub Discussions:** [Ask questions and share ideas](https://github.com/MartinRieser/GeckoCIRCUITS/discussions)
