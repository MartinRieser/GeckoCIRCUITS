#!/bin/bash

# Quick test of golden reference infrastructure with one circuit
echo "Testing golden reference infrastructure with single circuit..."

# Test with Education_ETHZ/ex_1.ipes (simple educational circuit)
CIRCUIT="Education_ETHZ/ex_1.ipes"
echo "Testing circuit: $CIRCUIT"

# Run baseline capture for just this circuit
mvn test-compile exec:java \
    -Dexec.mainClass="ch.technokrat.regression.BaselineCapture" \
    -Dexec.classpathScope="test" \
    -Dcircuits.filter="Education_ETHZ" \
    -Dbaseline.overwrite=true \
    2>&1 | tee test-single-result.log

# Check if baseline was created
if [ -d "src/test/resources/golden/baselines/Education_ETHZ" ]; then
    echo "✅ SUCCESS: Baseline directory created!"

    # List files created
    echo "Files created:"
    find src/test/resources/golden/baselines/Education_ETHZ -type f -exec ls -la {} \;

    # Test loading the baseline
    echo ""
    echo "Testing baseline loading..."
    mvn test -Dtest=GoldenReferenceTest -Dcircuit="$CIRCUIT"

else
    echo "❌ FAILED: No baseline directory created"
    echo "Check test-single-result.log for errors"
fi