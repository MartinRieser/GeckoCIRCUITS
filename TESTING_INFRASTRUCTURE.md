# Golden Reference Testing Infrastructure

## Overview

This document describes the golden reference testing infrastructure created for GeckoCIRCUITS refactoring. The infrastructure ensures that simulation results remain identical throughout code modernization.

## Architecture

### Core Components

```
src/test/java/ch/technokrat/regression/
├── GoldenReferenceTestHarness.java  - Runs simulations headlessly
├── SimulationResult.java            - Container for simulation data
├── CircuitDiscovery.java            - Finds all .ipes circuit files
├── ResultSerializer.java            - Saves/loads baselines (CSV format)
├── ResultComparator.java            - Compares results with tolerance
├── GoldenReferenceTest.java         - JUnit parameterized tests
└── BaselineCapture.java             - Utility to create baselines

src/test/resources/golden/
├── baselines/                       - Stored simulation results
│   └── [circuit-path]/
│       ├── _metadata.txt            - Circuit metadata & checksum
│       └── [signal-name].csv        - Time-series data per signal
└── metadata/                        - Additional metadata
```

## Usage Guide

### Step 1: Capture Baseline Data

**Before any refactoring**, capture the current simulation results as the golden reference:

```bash
# Capture ALL circuits (takes a long time - 100+ circuits)
mvn test-compile exec:java -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
                           -Dexec.classpathScope="test"

# Capture only specific directory (recommended for testing)
mvn test-compile exec:java -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
                           -Dexec.classpathScope="test" \
                           -Dcircuits.filter="Topologies"

# Overwrite existing baselines
mvn test-compile exec:java -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
                           -Dexec.classpathScope="test" \
                           -Dbaseline.overwrite=true

# Verify reproducibility (runs each circuit twice)
mvn test-compile exec:java -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
                           -Dexec.classpathScope="test" \
                           -Dbaseline.verify=true
```

**Output:**
- Creates `src/test/resources/golden/baselines/[circuit-path]/` directories
- Each circuit gets a `_metadata.txt` file with simulation parameters and checksum
- Each signal gets a `[signal-name].csv` file with time-series data

### Step 2: Run Golden Reference Tests

After making code changes, verify results still match:

```bash
# Run all circuits with baselines
mvn test -Dtest=GoldenReferenceTest

# Run specific circuit
mvn test -Dtest=GoldenReferenceTest -Dcircuit="Topologies/BuckBoost_thermal.ipes"

# Use strict tolerance (bit-identical)
mvn test -Dtest=GoldenReferenceTest -Dtolerance=STRICT

# Use relaxed tolerance (for performance optimizations)
mvn test -Dtest=GoldenReferenceTest -Dtolerance=RELAXED
```

**Test Results:**
- ✅ **PASS**: Results match baseline within tolerance
- ❌ **FAIL**: Results differ - see detailed error report in log

### Step 3: Analyze Failures

When tests fail, check the logs for detailed comparison:

```
[SEVERE] FAIL: Topologies/BuckBoost_thermal.ipes
Comparison Result: FAIL
Max Absolute Error: 1.234e-05
Max Relative Error: 2.345e-04
Signal with Max Error: scope_voltage[1523]

Differences Found:
  - Signal scope_voltage[1523] differs: expected=12.345, actual=12.346,
    absError=1.234e-05, relError=2.345e-04
```

**Common failure scenarios:**
1. **Non-deterministic behavior** - Circuit uses random numbers or timing-dependent code
2. **Algorithm changed** - Numerical method was modified (intentionally or not)
3. **Floating-point rounding** - Different JVM or compiler optimization changed FP behavior
4. **Bug introduced** - New code has a defect

## Tolerance Levels

Three tolerance levels are available:

| Level | Value | Use Case |
|-------|-------|----------|
| `STRICT` | 1e-12 | Verify bit-identical results (default for capturing) |
| `NORMAL` | 1e-10 | Standard floating-point tolerance (default for testing) |
| `RELAXED` | 1e-6 | Performance optimizations that slightly affect precision |

Set tolerance via system property:
```bash
mvn test -Dtest=GoldenReferenceTest -Dtolerance=STRICT
```

## Data Format

### Metadata File (_metadata.txt)

```
circuit=Topologies/BuckBoost_thermal.ipes
simulationTime=0.01
timestep=1.0E-6
checksum=a3f5c8d9e1b2f4a6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0
signalCount=5
```

### Signal CSV Files

```csv
time,value
0.0,0.0
1.0E-6,0.012345
2.0E-6,0.023456
...
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Golden Reference Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'

      - name: Run golden reference tests
        run: mvn test -Dtest=GoldenReferenceTest

      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: target/surefire-reports/
```

## Best Practices

### Capturing Baselines

1. **Clean environment**: Ensure no local modifications before capturing
2. **Document commit**: Note the exact commit hash used for baselines
3. **Version control**: Commit baseline data to git (or use Git LFS for large datasets)
4. **Verify reproducibility**: Run `-Dbaseline.verify=true` to ensure deterministic results
5. **Start small**: Capture a subset first (e.g., Topologies/) before full capture

### Running Tests

1. **Run frequently**: Execute tests after every significant code change
2. **Check tolerance**: Start with STRICT, relax only if necessary
3. **Investigate failures**: Don't ignore failures - understand the root cause
4. **Update baselines**: Only after confirming changes are intentional and correct

### Handling Failures

1. **Review diff**: Examine which signals and time points differ
2. **Check magnitude**: Is error within acceptable numerical precision?
3. **Verify logic**: Did algorithm change? If not, investigate why results differ
4. **Test edge cases**: Some circuits may be more sensitive to changes
5. **Re-baseline**: If changes are intentional and verified correct, recapture baseline

## Troubleshooting

### No signals captured

**Problem**: `No signals captured - circuit may not have scopes`

**Solution**: Circuit doesn't contain scope elements. This is normal for some circuits. They will be skipped.

### Simulation timeout

**Problem**: `Simulation did not complete successfully or timed out after 300 seconds`

**Solution**:
- Increase timeout in `GoldenReferenceTestHarness.java`
- Circuit may have infinite loop or very long simulation time
- Check circuit parameters (dt, tend)

### Non-reproducible results

**Problem**: Running same circuit twice gives different results

**Solution**:
- Circuit uses random number generation - may need to seed RNG
- Circuit has timing-dependent behavior
- Floating-point operations are non-deterministic on this platform
- Use `strictfp` keyword to enforce IEEE 754 compliance

### Memory issues

**Problem**: `OutOfMemoryError` during baseline capture

**Solution**:
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx4G"
mvn test-compile exec:java ...
```

### Baseline not found

**Problem**: `Baseline not found for circuit: XYZ`

**Solution**: Baseline hasn't been captured yet. Run `BaselineCapture` first.

## API Reference

### GoldenReferenceTestHarness

```java
// Initialize the harness (must be called first)
GoldenReferenceTestHarness.initialize();

// Run a simulation
SimulationResult result = GoldenReferenceTestHarness.runCircuitSimulation(circuitFile);

// Cleanup
GoldenReferenceTestHarness.shutdown();
```

### ResultComparator

```java
// Compare with tolerance level
ComparisonResult result = ResultComparator.compare(
    expectedResult,
    actualResult,
    ToleranceLevel.NORMAL
);

// Compare with custom tolerance
ComparisonResult result = ResultComparator.compare(
    expectedResult,
    actualResult,
    1e-10
);

// Quick checksum comparison
boolean matches = ResultComparator.quickCompare(expected, actual);

// Get detailed report
String report = result.getReport();
```

### ResultSerializer

```java
// Save baseline
ResultSerializer.saveAsCSV(simulationResult);

// Load baseline
SimulationResult baseline = ResultSerializer.loadFromCSV(circuitName);

// Check if baseline exists
boolean exists = ResultSerializer.baselineExists(circuitName);
```

## Extending the Infrastructure

### Adding New Comparison Metrics

Edit `ResultComparator.java` to add custom comparison logic:

```java
// Example: Energy conservation check
public static boolean checkEnergyConservation(SimulationResult result) {
    // Custom validation logic
}
```

### Custom Signal Extraction

Edit `GoldenReferenceTestHarness.captureResults()` to extract additional signals:

```java
// Example: Capture thermal data
String[] thermalElements = GeckoExternal.getThermalElements();
for (String element : thermalElements) {
    // Extract thermal signal data
}
```

### Alternative Serialization Formats

Implement `ResultSerializerBinary.java` for more compact storage:

```java
public class ResultSerializerBinary {
    public static void saveAsBinary(SimulationResult result) {
        // Use DataOutputStream for compact binary format
    }
}
```

## Performance Considerations

### Baseline Capture Time

Approximate times (single-threaded):
- Small circuit (< 10ms simulation): ~1-2 seconds
- Medium circuit (10-100ms): ~5-10 seconds
- Large circuit (> 100ms): ~30-60 seconds

For 100+ circuits: **2-3 hours total**

**Optimization strategies:**
- Parallel execution (modify `BaselineCapture` to use thread pool)
- Capture subset for quick validation
- Use faster machine for baseline generation

### Storage Requirements

Approximate sizes:
- Small circuit baseline: ~100 KB
- Medium circuit baseline: ~1 MB
- Large circuit baseline: ~10 MB

For 100 circuits: **~500 MB total**

**Storage optimization:**
- Use Git LFS for large binary files
- Compress CSV files (gzip)
- Downsample time series (increase skipPoints)
- Store only key signals (not all scopes)

## Maintenance

### Regular Tasks

1. **Update baselines** after validated algorithm changes
2. **Prune unused baselines** if circuits are removed
3. **Verify reproducibility** periodically (quarterly)
4. **Archive old baselines** when major refactoring completes

### Version Control

Recommended `.gitattributes`:
```
*.csv filter=lfs diff=lfs merge=lfs -text
src/test/resources/golden/baselines/** filter=lfs diff=lfs merge=lfs -text
```

## Future Enhancements

Potential improvements:
- [ ] Parallel baseline capture
- [ ] Binary serialization format
- [ ] Web dashboard for test results
- [ ] Automatic failure bisection (git bisect integration)
- [ ] Performance regression detection
- [ ] Visual waveform comparison tool
- [ ] Integration with JaCoCo coverage reports

## References

- JUnit 4 Parameterized Tests: https://junit.org/junit4/javadoc/4.12/org/junit/runners/Parameterized.html
- Maven Surefire Plugin: https://maven.apache.org/surefire/maven-surefire-plugin/
- IEEE 754 Floating Point: https://en.wikipedia.org/wiki/IEEE_754

## Support

For issues or questions:
1. Check this documentation
2. Review `REFACTORING_PLAN.md`
3. Examine test logs in `target/surefire-reports/`
4. Open GitHub issue with details

---

*Last updated: 2025-10-12*
*Part of GeckoCIRCUITS Refactoring Project - Phase 1*
