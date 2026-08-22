/**
 * Pure mapping from a simulation results payload (signal name -> samples)
 * to chart inputs: trace order, the time vector and per-signal statistics.
 * Extracted from SimulationDrawer so it is unit-testable.
 */

export interface SignalStats {
  min: number;
  max: number;
  pkpk: number;
  mean: number;
  rms: number;
}

export interface ChartData {
  signalNames: string[];
  timeArray: number[];
  signalStats: Record<string, SignalStats>;
}

export function computeSignalStats(arr: number[]): SignalStats | null {
  if (arr.length === 0) return null;
  let min = arr[0];
  let max = arr[0];
  let sum = 0;
  let sumSq = 0;
  for (let i = 0; i < arr.length; i++) {
    const v = arr[i];
    if (v < min) min = v;
    if (v > max) max = v;
    sum += v;
    sumSq += v * v;
  }
  const mean = sum / arr.length;
  return { min, max, pkpk: max - min, mean, rms: Math.sqrt(sumSq / arr.length) };
}

export function mapSimulationResults(results: Record<string, number[]> | null): ChartData {
  if (!results || Object.keys(results).length === 0) {
    return { signalNames: [], timeArray: [], signalStats: {} };
  }
  const timeArray = results['time'] || [];
  const signalNames = Object.keys(results).filter((k) => k !== 'time');
  const signalStats: Record<string, SignalStats> = {};
  for (const name of signalNames) {
    const stats = computeSignalStats(results[name] || []);
    if (stats) signalStats[name] = stats;
  }
  return { signalNames, timeArray, signalStats };
}
