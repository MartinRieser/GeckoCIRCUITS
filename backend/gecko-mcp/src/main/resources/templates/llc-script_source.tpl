// === Microcontroller Emulation: Half-Bridge LLC Resonant Controller ===
double f_sw = §0§;
double t_dead = §1§;
double T_sw = 1.0 / f_sw;
double t_phase = t - Math.floor(t / T_sw) * T_sw;

// Complementary PWM outputs with dead-time for Zero-Voltage Switching (ZVS)
boolean s1 = (t_phase >= t_dead) && (t_phase < (0.5 * T_sw));
boolean s2 = (t_phase >= (0.5 * T_sw + t_dead)) && (t_phase < T_sw);

yOUT[0] = s1 ? 1.0 : 0.0;
yOUT[1] = s2 ? 1.0 : 0.0;
return yOUT;