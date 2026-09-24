import { describe, it, expect } from 'vitest';
import {
  Orientation,
  ORIENTATION_CYCLE,
  LkComponentType,
  ControlComponentType,
  CTRL_TYPE,
  CANVAS_METRICS,
  SENTINEL_UNSET,
  SIMULATION_DEFAULTS,
} from '../src/model/constants';

describe('Model Constants & Enums', () => {
  it('defines correct numerical orientation codes matching legacy .ipes format', () => {
    expect(Orientation.SOUTH_NORTH).toBe(501);
    expect(Orientation.WEST_EAST).toBe(502);
    expect(Orientation.NORTH_SOUTH).toBe(503);
    expect(Orientation.EAST_WEST).toBe(504);
  });

  it('defines the standard 4-phase rotation cycle', () => {
    expect(ORIENTATION_CYCLE).toEqual([
      Orientation.NORTH_SOUTH,
      Orientation.EAST_WEST,
      Orientation.SOUTH_NORTH,
      Orientation.WEST_EAST,
    ]);
  });

  it('defines classic LK component type IDs accurately', () => {
    expect(LkComponentType.RESISTOR).toBe(1);
    expect(LkComponentType.INDUCTOR).toBe(2);
    expect(LkComponentType.CAPACITOR).toBe(3);
    expect(LkComponentType.VOLTAGE_SOURCE).toBe(4);
    expect(LkComponentType.CURRENT_SOURCE).toBe(5);
    expect(LkComponentType.DIODE).toBe(6);
    expect(LkComponentType.IDEAL_SWITCH).toBe(7);
    expect(LkComponentType.THYRISTOR).toBe(8);
    expect(LkComponentType.IGBT).toBe(10);
    expect(LkComponentType.TRANSFORMER).toBe(23);
    expect(LkComponentType.MOSFET).toBe(28);
    expect(LkComponentType.BJT).toBe(33);
  });

  it('defines Control component type IDs accurately across legacy and modern ranges', () => {
    expect(ControlComponentType.LEGACY_VOLTMETER).toBe(1);
    expect(ControlComponentType.LEGACY_AMMETER).toBe(2);
    expect(ControlComponentType.VOLTMETER).toBe(1001);
    expect(ControlComponentType.AMMETER).toBe(1002);
    expect(ControlComponentType.SCOPE).toBe(1003);
    expect(ControlComponentType.SIGNAL_SOURCE).toBe(1004);
    expect(ControlComponentType.CONSTANT).toBe(1005);
  });

  it('provides backwards-compatible CTRL_TYPE mapping object', () => {
    expect(CTRL_TYPE.VOLTMETER).toBe(1001);
    expect(CTRL_TYPE.AMMETER).toBe(1002);
    expect(CTRL_TYPE.SCOPE).toBe(1003);
  });

  it('defines valid canvas metric dimensions and thresholds', () => {
    expect(CANVAS_METRICS.TWO_PORT_DIST).toBe(2);
    expect(CANVAS_METRICS.MULTI_PIN_STEP).toBe(2);
    expect(CANVAS_METRICS.DEFAULT_SNAP_DISTANCE).toBeGreaterThan(0);
    expect(CANVAS_METRICS.MIN_ZOOM).toBeLessThan(CANVAS_METRICS.MAX_ZOOM);
    expect(CANVAS_METRICS.ZOOM_STEP_FACTOR).toBeGreaterThan(1.0);
  });

  it('defines the standard .ipes unassigned sentinel string', () => {
    expect(SENTINEL_UNSET).toBe('NIX_NIX_NIX');
  });

  it('defines sensible simulation runner defaults', () => {
    expect(SIMULATION_DEFAULTS.DURATION_SEC).toBeGreaterThan(0);
    expect(SIMULATION_DEFAULTS.TIME_STEP_SEC).toBeGreaterThan(0);
    expect(SIMULATION_DEFAULTS.POLL_INTERVAL_MS).toBeGreaterThan(50);
  });
});
