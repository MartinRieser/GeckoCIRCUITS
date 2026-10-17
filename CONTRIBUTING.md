# Contributing to GeckoCIRCUITS

Thank you for your interest in contributing!

For the full contributing guide, including development setup, code architecture, and PR process, see our **[Contributing Guide](https://martinrieser.github.io/GeckoCIRCUITS/resources/contributing/)** (or [`docs/resources/contributing.md`](docs/resources/contributing.md)).

## Quick Start

```bash
git clone https://github.com/MartinRieser/GeckoCIRCUITS.git
cd GeckoCIRCUITS

# Build backend modules
mvn clean package -DskipTests

# Run unit tests
mvn test
npm --prefix frontend test
npm --prefix frontend run lint
```

## Questions?

Open a [GitHub Issue](https://github.com/MartinRieser/GeckoCIRCUITS/issues) or [Discussion](https://github.com/MartinRieser/GeckoCIRCUITS/discussions).
