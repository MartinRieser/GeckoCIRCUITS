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
    expect(ControlComponentType.ASIN).toBe(1038);
    expect(ControlComponentType.ACOS).toBe(1039);
    expect(ControlComponentType.TAN).toBe(1040);
    expect(ControlComponentType.ATAN).toBe(1041);
    expect(ControlComponentType.SQR).toBe(1042);
    expect(ControlComponentType.POW).toBe(1043);
    expect(ControlComponentType.ROUND).toBe(1044);
    expect(ControlComponentType.SIGN).toBe(1045);
    expect(ControlComponentType.EQ).toBe(1046);
    expect(ControlComponentType.NE).toBe(1047);
    expect(ControlComponentType.COUNTER).toBe(1048);
    expect(ControlComponentType.ABCDQ).toBe(1049);
    expect(ControlComponentType.DQABC).toBe(1050);
    expect(ControlComponentType.THYR_CTRL).toBe(1051);
    expect(ControlComponentType.PMSM_CONTROL).toBe(1052);
    expect(ControlComponentType.PMSM_MODULATOR).toBe(1053);
    expect(ControlComponentType.DEMUX).toBe(1054);
    expect(ControlComponentType.SPACE_VECTOR).toBe(1055);
    expect(ControlComponentType.SDFT).toBe(1056);
    expect(ControlComponentType.SPARSEMATRIX).toBe(1058);

    expect(ControlComponentType.LEGACY_ASIN).toBe(33);
    expect(ControlComponentType.LEGACY_ACOS).toBe(35);
    expect(ControlComponentType.LEGACY_TAN).toBe(37);
    expect(ControlComponentType.LEGACY_ATAN).toBe(38);
    expect(ControlComponentType.LEGACY_SQR).toBe(39);
    expect(ControlComponentType.LEGACY_POW).toBe(42);
    expect(ControlComponentType.LEGACY_ROUND).toBe(44);
    expect(ControlComponentType.LEGACY_SIGN).toBe(47);
    expect(ControlComponentType.LEGACY_EQ).toBe(48);
    expect(ControlComponentType.LEGACY_NE).toBe(51);
    expect(ControlComponentType.LEGACY_COUNTER).toBe(53);
    expect(ControlComponentType.LEGACY_ABCDQ).toBe(59);
    expect(ControlComponentType.LEGACY_DQABC).toBe(63);
    expect(ControlComponentType.LEGACY_THYR_CTRL).toBe(65);
    expect(ControlComponentType.LEGACY_PMSM_CONTROL).toBe(66);
    expect(ControlComponentType.LEGACY_PMSM_MODULATOR).toBe(72);
    expect(ControlComponentType.LEGACY_DEMUX).toBe(76);
    expect(ControlComponentType.LEGACY_SPACE_VECTOR).toBe(77);
    expect(ControlComponentType.LEGACY_SDFT).toBe(82);
    expect(ControlComponentType.LEGACY_SPARSEMATRIX).toBe(85);
  });

  it('provides backwards-compatible CTRL_TYPE mapping object', () => {
    expect(CTRL_TYPE.VOLTMETER).toBe(1001);
    expect(CTRL_TYPE.AMMETER).toBe(1002);
    expect(CTRL_TYPE.SCOPE).toBe(1003);
    expect(CTRL_TYPE.ASIN).toBe(1038);
    expect(CTRL_TYPE.ABCDQ).toBe(1049);
    expect(CTRL_TYPE.DQABC).toBe(1050);
    expect(CTRL_TYPE.DEMUX).toBe(1054);
    expect(CTRL_TYPE.SDFT).toBe(1056);
  });

  it('defines valid canvas metric dimensions and thresholds', () => {
    expect(CANVAS_METRICS.TWO_PORT_DIST).toBe(2);
    expect(CANVAS_METRICS.MULTI_PIN_STEP).toBe(2);
    expect(CANVAS_METRICS.DEFAULT_SNAP_DISTANCE).toBeGreaterThan(0);
    expect(CANVAS_METRICS.TERMINAL_TOUCH_TOLERANCE).toBe(0.25);
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
