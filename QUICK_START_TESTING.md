# Quick Start: Golden Reference Testing

## TL;DR - Get Started in 3 Commands

```bash
# 1. Capture baselines (do this ONCE before refactoring)
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test"

# 2. Make your code changes...
# (edit Java files, refactor, etc.)

# 3. Verify results still match
mvn test -Dtest=GoldenReferenceTest
```

## What This Does

1. **Step 1** runs all circuits and saves their output signals
2. **Step 2** is where you make changes (refactor, upgrade Java, etc.)
3. **Step 3** runs the same circuits and compares against saved baselines

If Step 3 passes → ✅ Your changes didn't affect simulation results!
If Step 3 fails → ❌ Something changed - investigate why.

## Quick Test (5 minutes)

Test with just one subdirectory first:

```bash
# Capture just Topologies circuits (~10-15 circuits)
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dcircuits.filter="Topologies"

# Run tests
mvn test -Dtest=GoldenReferenceTest
```

## Common Commands

### Capture baselines for specific directory
```bash
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dcircuits.filter="Education_ETHZ"
```

### Re-capture (overwrite) existing baselines
```bash
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dbaseline.overwrite=true
```

### Test specific circuit
```bash
mvn test -Dtest=GoldenReferenceTest \
    -Dcircuit="Topologies/BuckBoost_thermal.ipes"
```

### Use strict tolerance (bit-identical)
```bash
mvn test -Dtest=GoldenReferenceTest -Dtolerance=STRICT
```

### Verify reproducibility (run twice, compare)
```bash
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dbaseline.verify=true
```

## Understanding Test Results

### ✅ PASS - Everything is good!
```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### ❌ FAIL - Something changed
```
[SEVERE] FAIL: Topologies/BuckBoost_thermal.ipes
Comparison Result: FAIL
Max Absolute Error: 1.234e-05
Signal with Max Error: scope_voltage[1523]
```

**What to do:**
1. Check the log for which signal differs
2. Verify if the change was intentional
3. If intentional and correct → recapture baseline
4. If NOT intentional → debug your code changes

## Where Are Baselines Stored?

```
src/test/resources/golden/baselines/
└── Topologies/
    └── BuckBoost_thermal/
        ├── _metadata.txt          ← Checksum and parameters
        ├── scope_voltage.csv      ← Time-series data
        ├── scope_current.csv
        └── scope_power.csv
```

Each circuit gets a directory with:
- Metadata file (simulation parameters)
- CSV file per signal (time and value columns)

## Troubleshooting

### "No baselines found"
→ You need to run `BaselineCapture` first

### "Simulation timeout"
→ Circuit takes > 5 minutes, increase timeout in `GoldenReferenceTestHarness.java`

### "No signals captured"
→ Circuit doesn't have scope elements, this is normal

### "OutOfMemoryError"
→ Increase heap: `export MAVEN_OPTS="-Xmx4G"`

## Workflow Example

```bash
# Before starting refactoring
git checkout -b refactor/java-upgrade
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test"
git add src/test/resources/golden/
git commit -m "Capture golden reference baselines"

# Make changes
# ... edit pom.xml, update Java version ...
# ... refactor code ...

# Verify after each change
mvn clean compile test-compile
mvn test -Dtest=GoldenReferenceTest

# If tests pass
git commit -am "Upgrade to Java 17 - all tests passing"

# If tests fail
# → investigate and fix
# → if change is intentional, recapture baselines
```

## Files You Need

| File | Purpose |
|------|---------|
| `TESTING_INFRASTRUCTURE.md` | Full documentation (read this) |
| `PHASE1_SUMMARY.md` | What was built (read this first) |
| `REFACTORING_PLAN.md` | Overall refactoring strategy |
| This file | Quick reference (keep this handy) |

## Need More Help?

1. Read `TESTING_INFRASTRUCTURE.md` for detailed documentation
2. Check `REFACTORING_PLAN.md` for the overall strategy
3. Look at test logs in `target/surefire-reports/`
4. Examine baseline CSV files to see what's being compared

---

**Remember**: The goal is to ensure simulation results DON'T change during refactoring!
