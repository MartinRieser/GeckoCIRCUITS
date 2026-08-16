import { describe, it, expect } from 'vitest';
import { EXAMPLES, RLC_CIRCUIT_IPES, BUCK_CONVERTER_IPES } from '../src/model/examples';

describe('Simulation & Examples', () => {
  it('provides built-in examples with valid .ipes structure', () => {
    expect(EXAMPLES.length).toBeGreaterThanOrEqual(3);

    const rlc = EXAMPLES.find((e) => e.id === 'rlc');
    expect(rlc).toBeDefined();
    expect(rlc!.content).toContain('tDURATION');
    expect(rlc!.content).toContain('ElementLK');
    expect(rlc!.content).toContain('verbindungLK');

    const buck = EXAMPLES.find((e) => e.id === 'buck');
    expect(buck).toBeDefined();
    expect(buck!.content).toContain('tDURATION');
    expect(buck!.content).toContain('ElementLK');
  });

  it('example circuits contain electrical components and data container signals', () => {
    expect(RLC_CIRCUIT_IPES).toContain('dataContainerSignals[]');
    expect(BUCK_CONVERTER_IPES).toContain('dataContainerSignals[]');
  });
});
