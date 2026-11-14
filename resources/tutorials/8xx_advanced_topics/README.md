# 8xx - Advanced Topics

Complex topologies and optimization techniques.

| Tutorial | Title | Difficulty |
|----------|-------|------------|
| [801](801_matrix_converters/) | Matrix Converters | 3/3 |
| [802](802_motor_drives_pmsm/) | Motor Drives (PMSM) | 3/3 |
| [803](803_optimization/) | Optimization | 3/3 |
| [804](804_thyristor_control/) | Thyristor Control | 3/3 |

## Learning Objectives

- Understand direct AC-AC conversion with matrix converters
- Implement field-oriented control for PMSM
- Optimize converter designs using the [REST API](../../../docs/api/rest-api.md), [`geckopy`](../../../docs/tutorials/scripting/python.md), or Java/[`SCRIPT_BLOCK`](../../../docs/tutorials/scripting/java-blocks.md) control blocks (see archived MATLAB/GeckoSCRIPT materials in `803_optimization/`)
- Control thyristor-based rectifiers

## Contents

### 801 - Matrix Converters
- `UltraSparseMatrixConverter.ipes` - Ultra-sparse topology
- `three-phase_ACAC_sparsematrixConverter_junction_temperature.ipes` - Thermal analysis

### 802 - Motor Drives (PMSM)
- `indirect_matrix_java_with_PMSM_control.ipes` - Indirect matrix with PMSM
- `sparseMatrix_java_with_PMSM_control.ipes` - Sparse matrix with PMSM

### 803 - Optimization
- `GeckoSwissRectifierOptimizer/` - Swiss rectifier optimization toolkit
- MATLAB scripts for automated design sweeps

### 804 - Thyristor Control
- Comprehensive thyristor circuit examples
- Single-phase, two-phase, and three-phase bridges
- Commutation and loss-of-commutation analysis
- Transformer-coupled configurations
