import { describe, it, expect } from 'vitest';
import { COMPONENT_METAS } from '../src/model/componentSchema';
import { terminalPositions, flowVector, controlFlowVector, TWO_PORT_DIST, ORIENTATION_CYCLE } from '../src/model/geometry';

/**
 * Strategy 1: Cross-Layer Contract & Terminal Coordinate Parity Tests.
 * Ensures COMPONENT_METAS definitions and geometry raster coordinate calculations
 * strictly align with simulation engine parameter and topology expectations.
 */
describe('Strategy 1: Cross-Layer Contract & Coordinate Parity', () => {

  it('all non-disabled components declare well-formed parameter definitions and non-empty terminals', () => {
    for (const [typeStr, meta] of Object.entries(COMPONENT_METAS)) {
      if (meta.disabled) continue;
      const type = Number(typeStr);

      // Verify basic metadata integrity
      expect(meta.type).toBe(type);
      expect(meta.displayName.trim().length).toBeGreaterThan(0);
      expect(meta.defaultPrefix.trim().length).toBeGreaterThan(0);
      expect(['LK', 'THERM', 'CONTROL', 'SPECIAL'].includes(meta.family)).toBe(true);

      // Verify parameters
      const seenIndices = new Set<number>();
      for (const param of meta.parameters) {
        if (!['sourceCode', 'staticCode', 'staticVariables', 'anzXIN', 'anzYOUT', 'text', 'width', 'height', 'libraryPath'].includes(param.key)) {
          expect(param.key).toMatch(/^param\d+$/);
          expect(param.index).toBeGreaterThanOrEqual(0);
          expect(seenIndices.has(param.index)).toBe(false);
          seenIndices.add(param.index);
        }

        if (param.options) {
          expect(param.options.length).toBeGreaterThan(0);
          for (const opt of param.options) {
            expect(typeof opt.label).toBe('string');
            expect(typeof opt.value).toBe('number');
          }
        } else if (typeof param.defaultValue === 'number') {
          expect(Number.isFinite(param.defaultValue)).toBe(true);
        } else {
          expect(typeof param.defaultValue).toBe('string');
        }
      }

      // Verify terminals
      expect(Array.isArray(meta.terminals.input)).toBe(true);
      expect(Array.isArray(meta.terminals.output)).toBe(true);
    }
  });

  it('terminalPositions produces valid raster points matching flowVector for all orientations', () => {
    const testCenter = [20, 30];

    for (const [typeStr, meta] of Object.entries(COMPONENT_METAS)) {
      if (meta.disabled || meta.type === 2001 || meta.type === 9) {
        // Textbox (2001) has no terminals; LK_M (9) is pinless by design (SPICE K coupler)
        continue;
      }
      const type = Number(typeStr);

      for (const orientation of ORIENTATION_CYCLE) {
        const terms = terminalPositions({
          type,
          family: meta.family,
          position: testCenter,
          orientation,
        });

        const allPoints = [...terms.input, ...terms.output];
        // Must have at least 1 terminal (except notes/couplers)
        expect(allPoints.length).toBeGreaterThan(0);

        for (const pt of allPoints) {
          // Terminal coordinates must strictly be integer grid raster coordinates
          expect(Number.isInteger(pt.x)).toBe(true);
          expect(Number.isInteger(pt.y)).toBe(true);

          // Terminals must be within reasonable distance of component center (<= 15 grid units)
          const dist = Math.hypot(pt.x - testCenter[0], pt.y - testCenter[1]);
          expect(dist).toBeGreaterThanOrEqual(1.0);
          expect(dist).toBeLessThanOrEqual(15.0);
        }

        // Standard 2-port electrical components must have input at -2 and output at +2 along flow
        if (meta.family === 'LK' && meta.terminals.input.length === 1 && meta.terminals.output.length === 1
            && type !== 31 && type !== 29 && type !== 23 && type !== 33) {
          const dir = flowVector(orientation);
          expect(terms.input[0]).toEqual({
            x: testCenter[0] - dir.x * TWO_PORT_DIST,
            y: testCenter[1] - dir.y * TWO_PORT_DIST,
          });
          expect(terms.output[0]).toEqual({
            x: testCenter[0] + dir.x * TWO_PORT_DIST,
            y: testCenter[1] + dir.y * TWO_PORT_DIST,
          });
        }

        // Single-output control sources (SIGNAL, CONSTANT) must have output at +2 along controlFlowVector
        if (meta.family === 'CONTROL' && meta.terminals.input.length === 0 && meta.terminals.output.length === 1) {
          const dir = controlFlowVector(orientation);
          expect(terms.output[0]).toEqual({
            x: testCenter[0] + dir.x * TWO_PORT_DIST,
            y: testCenter[1] + dir.y * TWO_PORT_DIST,
          });
        }

        // Single-input control gates must have input at -2 along controlFlowVector
        if (meta.family === 'CONTROL' && meta.terminals.input.length === 1 && meta.terminals.output.length === 0) {
          const dir = controlFlowVector(orientation);
          expect(terms.input[0]).toEqual({
            x: testCenter[0] - dir.x * TWO_PORT_DIST,
            y: testCenter[1] - dir.y * TWO_PORT_DIST,
          });
        }
      }
    }
  });

  it('standard component defaults in componentSchema match backend CircuitEditService defaults', () => {
    // Cross-check canonical default parameters between frontend schema and backend
    const expectedDefaults: Record<number, Record<string, number>> = {
      1: { param0: 1000 },                 // Resistor
      2: { param0: 3e-4 },                 // Inductor
      3: { param0: 100e-9 },               // Capacitor
      4: { param0: 401, param1: 10 },      // Voltage source
      5: { param0: 401, param1: 1 },       // Current source
      6: { param1: 0.6, param2: 1e-2, param3: 1e7 }, // Diode
      7: { param1: 1e-2, param2: 1e7 },    // Switch
      8: { param1: 0.8, param2: 1e-2, param3: 1e7 }, // Thyristor
      10: { param1: 0.8, param2: 1e-2, param3: 1e7 }, // IGBT
      12: { param0: 1e-3 },                // Coupled Inductor
      23: { param0: 10, param1: 2, param2: -1 }, // Ideal transformer (10:2 inverted)
      24: { param0: 1.0 },                 // Reluctance
      26: { param0: 401, param1: 100 },    // MMF source
      28: { param1: 0.8, param2: 1e-2, param3: 1e7 }, // MOSFET
      1004: { param0: 404, param1: 1.0, param2: 100000 }, // Signal generator (rect, 1V, 100kHz)
      1005: { param0: 1.0 },               // Constant 1.0
      1006: { param0: 1.0 },               // Gain 1.0
      1007: { param0: 1.0, param1: 0.01 }, // PI (Kp=1, Ti=0.01s)
      1008: { param0: 0.001 },             // PT1 (tau=1ms)
      1015: { param0: 0.001 },             // Delay 1ms
      1021: { param0: -1.0, param1: 1.0 }, // Limit [-1, 1]
      1030: { param0: 0.1 },               // Hysteresis 0.1
      1031: { param0: 0.001, param1: 1.0 }, // PT2 (tau=1ms, gain=1)
      1032: { param0: 1.0 },               // PD (gain=1)
      1037: { param0: 200e-9 },            // Dead-time 200ns
    };

    for (const [typeStr, expected] of Object.entries(expectedDefaults)) {
      const type = Number(typeStr);
      const meta = COMPONENT_METAS[type];
      expect(meta, `Component type ${type} should exist in COMPONENT_METAS`).toBeDefined();

      for (const [key, expectedVal] of Object.entries(expected)) {
        const paramDef = meta.parameters.find(p => p.key === key);
        expect(paramDef, `Component ${meta.name} (${type}) should define ${key}`).toBeDefined();
        expect(paramDef!.defaultValue, `Component ${meta.name} (${type}) ${key} mismatch`).toBeCloseTo(expectedVal, 6);
      }
    }
  });

});
