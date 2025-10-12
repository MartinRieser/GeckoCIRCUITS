#!/bin/bash

# Script to test the golden reference infrastructure with a single circuit
# This verifies that the infrastructure works before running full baseline capture

echo "=========================================="
echo "Testing Golden Reference Infrastructure"
echo "=========================================="
echo ""

# Step 1: Check that we have circuit files
echo "Step 1: Checking for circuit files..."
CIRCUIT_COUNT=$(find resources -name "*.ipes" 2>/dev/null | wc -l)
echo "Found $CIRCUIT_COUNT circuit files"

if [ "$CIRCUIT_COUNT" -eq 0 ]; then
    echo "ERROR: No circuit files found in resources/ directory"
    exit 1
fi

# Step 2: Try to capture baseline for one circuit in Topologies
echo ""
echo "Step 2: Capturing baseline for sample circuit..."
echo "(This may take 1-2 minutes)"
echo ""

mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dcircuits.filter="Topologies" \
    -Dbaseline.overwrite=true \
    2>&1 | tee test-baseline-capture.log

# Check if baseline was created
if [ -d "src/test/resources/golden/baselines" ]; then
    BASELINE_COUNT=$(find src/test/resources/golden/baselines -name "_metadata.txt" 2>/dev/null | wc -l)
    echo ""
    echo "Baseline files created: $BASELINE_COUNT"

    if [ "$BASELINE_COUNT" -gt 0 ]; then
        echo "✓ Baseline capture successful!"
    else
        echo "✗ No baselines were created"
        exit 1
    fi
else
    echo "✗ Baseline directory not created"
    exit 1
fi

# Step 3: Run golden reference test
echo ""
echo "Step 3: Running golden reference test..."
echo ""

mvn test -Dtest=GoldenReferenceTest 2>&1 | tee test-golden-reference.log

# Check test results
if grep -q "BUILD SUCCESS" test-golden-reference.log; then
    echo ""
    echo "=========================================="
    echo "✓ All tests PASSED!"
    echo "Infrastructure is working correctly"
    echo "=========================================="
    exit 0
else
    echo ""
    echo "=========================================="
    echo "✗ Tests FAILED"
    echo "Check test-golden-reference.log for details"
    echo "=========================================="
    exit 1
fi
