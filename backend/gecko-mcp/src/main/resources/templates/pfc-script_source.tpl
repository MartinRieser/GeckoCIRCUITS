// === Microcontroller Emulation: Active Interleaved PFC ===
double v_ref = §0§;
double v_out = xIN[0];

// 1. Dynamic Load Step: switch in parallel load at t = §1§s
double gate_load = (t >= §2§) ? 1.0 : 0.0;
yOUT[2] = gate_load;

// 2. Voltage Error & Anti-Windup PI Controller
double e_v = v_ref - v_out;
double Kp_v = 0.018;
double Ki_v = 4.5;

v_int = v_int + Ki_v * e_v * dt;
if (v_int < -0.35) v_int = -0.35;
if (v_int > 0.35) v_int = 0.35;

double duty = 0.41 + Kp_v * e_v + v_int;

// 3. Grid-Synchronized Duty Shaping (European §3§ Hz Mains)
double omega = 2.0 * PI * §4§;
double grid_phase = abs(sin(omega * t));
double duty_mod = duty * (0.90 + 0.10 * (1.0 - grid_phase));
if (duty_mod < 0.05) duty_mod = 0.05;
if (duty_mod > 0.78) duty_mod = 0.78;

// 4. Dual Interleaved PWM Carriers (§5§ Hz, 180 deg phase shift)
double f_sw = §6§;
double T_sw = 1.0 / f_sw;

// Carrier 1: ramp [0, 1)
double t1 = t % T_sw;
double c1 = t1 / T_sw;

// Carrier 2: shifted by T_sw / 2 (180 degrees)
double t2 = (t + 0.5 * T_sw) % T_sw;
double c2 = t2 / T_sw;

// Gate outputs
yOUT[0] = (c1 < duty_mod) ? 1.0 : 0.0;
yOUT[1] = (c2 < duty_mod) ? 1.0 : 0.0;

// Telemetry
yOUT[3] = v_out;
yOUT[4] = duty_mod;
return yOUT;