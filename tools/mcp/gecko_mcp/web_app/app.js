/**
 * GeckoCIRCUITS LLC Resonant Converter Web Interface Logic.
 * Manages interactive SVG schematic, Chart.js waveform renderers,
 * and live REST/MCP API communication with GeckoHeadless simulation engine.
 */

// Global Chart instances
let chartZVS = null;
let chartTank = null;
let chartVout = null;

// State
let isSimulating = false;
let animationEnabled = true;

// DOM Element References
const btnSimulate = document.getElementById('btn-simulate');
const simSpinner = document.getElementById('sim-spinner');
const btnSimulateText = document.getElementById('btn-simulate-text');

const sliderPout = document.getElementById('slider-pout');
const valPout = document.getElementById('val-pout');
const sliderFsw = document.getElementById('slider-fsw');
const valFsw = document.getElementById('val-fsw');
const sliderDeadtime = document.getElementById('slider-deadtime');
const valDeadtime = document.getElementById('val-deadtime');
const sliderVin = document.getElementById('slider-vin');
const valVin = document.getElementById('val-vin');

const kpiZVS = document.getElementById('kpi-zvs-status');
const kpiZVSSub = document.getElementById('kpi-zvs-sub');
const kpiF0 = document.getElementById('kpi-f0');
const kpiKFactor = document.getElementById('kpi-kfactor');
const kpiVout = document.getElementById('kpi-vout');
const kpiVoutSub = document.getElementById('kpi-vout-sub');
const kpiPout = document.getElementById('kpi-pout');
const kpiPoutSub = document.getElementById('kpi-pout-sub');
const kpiIlr = document.getElementById('kpi-ilr');

const inspName = document.getElementById('insp-name');
const inspType = document.getElementById('insp-type');
const inspVal = document.getElementById('insp-val');

const btnToggleAnim = document.getElementById('btn-toggle-animation');
const resonantFlowWire = document.getElementById('wire-resonant-flow');

// --- 1. Chart Configuration and Initialization ---
function initCharts() {
  const commonChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 400 },
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: {
        grid: { color: 'rgba(148, 163, 184, 0.08)' },
        ticks: {
          color: '#64748b',
          font: { family: 'JetBrains Mono', size: 10 },
          maxTicksLimit: 6,
          callback: function(val, index, ticks) {
            const label = this.getLabelForValue(val);
            const num = parseFloat(label);
            return isNaN(num) ? label : (num * 1e6).toFixed(1) + ' μs';
          }
        }
      },
      y: {
        grid: { color: 'rgba(148, 163, 184, 0.08)' },
        ticks: {
          color: '#64748b',
          font: { family: 'JetBrains Mono', size: 10 }
        }
      }
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        titleFont: { family: 'JetBrains Mono', size: 11 },
        bodyFont: { family: 'JetBrains Mono', size: 11 },
        borderColor: 'rgba(148, 163, 184, 0.2)',
        borderWidth: 1,
        padding: 8
      }
    }
  };

  // 1. ZVS Chart: Vsw & Gate Drives
  const ctxZVS = document.getElementById('chart-zvs').getContext('2d');
  chartZVS = new Chart(ctxZVS, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'V_sw (V)',
          borderColor: '#06b6d4',
          backgroundColor: 'rgba(6, 182, 212, 0.08)',
          borderWidth: 2,
          pointRadius: 0,
          fill: true,
          tension: 0.1,
          yAxisID: 'y'
        },
        {
          label: 'Gate 1',
          borderColor: '#10b981',
          borderWidth: 1.5,
          pointRadius: 0,
          tension: 0,
          yAxisID: 'yGate',
          borderDash: [3, 2]
        },
        {
          label: 'Gate 2',
          borderColor: '#8b5cf6',
          borderWidth: 1.5,
          pointRadius: 0,
          tension: 0,
          yAxisID: 'yGate',
          borderDash: [3, 2]
        }
      ]
    },
    options: {
      ...commonChartOptions,
      scales: {
        ...commonChartOptions.scales,
        y: {
          ...commonChartOptions.scales.y,
          title: { display: true, text: 'V_sw (V)', color: '#06b6d4', font: { size: 10 } },
          min: -20,
          max: 450
        },
        yGate: {
          position: 'right',
          grid: { drawOnChartArea: false },
          ticks: { color: '#94a3b8', font: { family: 'JetBrains Mono', size: 9 }, stepSize: 1 },
          min: -0.2,
          max: 3.5,
          display: false
        }
      }
    }
  });

  // 2. Tank Dynamics: i_Lr & V_pri
  const ctxTank = document.getElementById('chart-tank').getContext('2d');
  chartTank = new Chart(ctxTank, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'i_Lr (A)',
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.08)',
          borderWidth: 2,
          pointRadius: 0,
          fill: true,
          tension: 0.2,
          yAxisID: 'y'
        },
        {
          label: 'V_pri (V)',
          borderColor: '#f59e0b',
          borderWidth: 1.5,
          pointRadius: 0,
          tension: 0.1,
          yAxisID: 'yVolt',
          borderDash: [4, 3]
        }
      ]
    },
    options: {
      ...commonChartOptions,
      scales: {
        ...commonChartOptions.scales,
        y: {
          ...commonChartOptions.scales.y,
          title: { display: true, text: 'i_Lr (A)', color: '#3b82f6', font: { size: 10 } }
        },
        yVolt: {
          position: 'right',
          grid: { drawOnChartArea: false },
          ticks: { color: '#f59e0b', font: { family: 'JetBrains Mono', size: 9 } },
          title: { display: true, text: 'V_pri (V)', color: '#f59e0b', font: { size: 10 } }
        }
      }
    }
  });

  // 3. Vout & Ripple
  const ctxVout = document.getElementById('chart-vout').getContext('2d');
  chartVout = new Chart(ctxVout, {
    type: 'line',
    data: {
      labels: [],
      datasets: [
        {
          label: 'V_out (V)',
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.08)',
          borderWidth: 2.2,
          pointRadius: 0,
          fill: true,
          tension: 0.1
        }
      ]
    },
    options: {
      ...commonChartOptions,
      scales: {
        ...commonChartOptions.scales,
        y: {
          ...commonChartOptions.scales.y,
          title: { display: true, text: 'V_out (V)', color: '#10b981', font: { size: 10 } }
        }
      }
    }
  });
}

// --- 2. Update Charts with Simulation Waveforms ---
function updateWaveforms(waveforms, metrics) {
  if (!waveforms || !waveforms.time) return;

  const times = waveforms.time;
  
  // ZVS Chart
  if (chartZVS && waveforms.sw) {
    chartZVS.data.labels = times;
    chartZVS.data.datasets[0].data = waveforms.sw;
    chartZVS.data.datasets[1].data = waveforms.gate1 || [];
    chartZVS.data.datasets[2].data = waveforms.gate2 || [];
    chartZVS.update();
  }

  // Tank Dynamics Chart
  if (chartTank && waveforms.i_lr) {
    chartTank.data.labels = times;
    chartTank.data.datasets[0].data = waveforms.i_lr;
    chartTank.data.datasets[1].data = waveforms.pri_p || [];
    chartTank.update();
  }

  // Vout Chart
  if (chartVout && waveforms.vout) {
    chartVout.data.labels = times;
    chartVout.data.datasets[0].data = waveforms.vout;
    chartVout.update();
  }

  // Update KPI Dashboard
  if (metrics) {
    const llc = metrics.llc_resonant;
    if (llc) {
      if (llc.zvs_soft_switching_achieved) {
        kpiZVS.innerText = 'VERIFIED (ZVS)';
        kpiZVS.className = 'kpi-value text-emerald';
        kpiZVSSub.innerText = `Switch node turn-on: ${llc.switch_node_min_volts}V`;
      } else {
        kpiZVS.innerText = 'HARD SWITCHING';
        kpiZVS.className = 'kpi-value text-amber';
        kpiZVSSub.innerText = `Residual: ${llc.switch_node_min_volts}V`;
      }
      kpiIlr.innerText = `${llc.resonant_current_peak_amps} A`;
    }

    const voutM = metrics.output_voltage;
    if (voutM) {
      kpiVout.innerText = `${voutM.steady_state_dc_volts} V`;
      kpiVoutSub.innerText = `Ripple: ${voutM.ripple_peak_to_peak_volts} V (${voutM.ripple_percentage}%)`;

      if (kpiPout && voutM.output_power_watts !== undefined) {
        const pKw = (voutM.output_power_watts / 1000).toFixed(1);
        kpiPout.innerText = `${pKw} kW`;
        kpiPoutSub.innerText = `Load: ${voutM.load_resistance_ohms} Ω · ${voutM.output_current_amps} A DC`;
      }
      const schRloadSub = document.getElementById('schematic-rload-sub');
      if (schRloadSub && voutM.load_resistance_ohms !== undefined) {
        schRloadSub.innerText = `${voutM.load_resistance_ohms} Ω (${(voutM.output_power_watts/1000).toFixed(1)} kW)`;
      }
    }
  }
}

// --- 3. Run Simulation via MCP Backend API ---
async function runSimulation() {
  if (isSimulating) return;

  isSimulating = true;
  simSpinner.style.display = 'inline-block';
  btnSimulateText.innerText = 'Simulating with Gecko Engine...';
  btnSimulate.disabled = true;

  const p_out = parseFloat(sliderPout.value);
  const f_sw = parseFloat(sliderFsw.value);
  const t_dead = parseFloat(sliderDeadtime.value) * 1e-9;
  const v_in = parseFloat(sliderVin.value);

  try {
    const res = await fetch(`/api/llc/simulate?p_out=${p_out}&f_sw=${f_sw}&t_dead=${t_dead}&v_in=${v_in}&duration=0.0003&dt=2e-8`);
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);

    const data = await res.json();
    if (data.status === 'SUCCESS') {
      updateWaveforms(data.waveforms, data.metrics);

      if (data.setup) {
        kpiF0.innerText = `${(data.setup.resonant_frequency_hz / 1000).toFixed(1)} kHz`;
        kpiKFactor.innerText = `Inductance Ratio k = ${data.setup.inductance_ratio_k}`;
      }
    } else {
      alert(`Simulation Error: ${data.error || 'Unknown error'}`);
    }
  } catch (err) {
    console.error('Simulation request failed:', err);
    alert(`Failed to connect to simulation engine: ${err.message}`);
  } finally {
    isSimulating = false;
    simSpinner.style.display = 'none';
    btnSimulateText.innerText = 'Simulate with Gecko Engine (MCP)';
    btnSimulate.disabled = false;
  }
}

// --- 4. Interactive Schematic Component Inspector ---
function setupSchematicInteractions() {
  const components = document.querySelectorAll('.schematic-component');

  components.forEach(comp => {
    comp.addEventListener('mouseenter', () => {
      const name = comp.dataset.component || 'Component';
      const type = comp.dataset.type || '';
      const val = comp.dataset.value || '';

      inspName.innerText = name;
      inspType.innerText = type;
      inspVal.innerText = val;
    });

    comp.addEventListener('click', () => {
      const name = comp.dataset.component || 'Component';
      const type = comp.dataset.type || '';
      const val = comp.dataset.value || '';

      inspName.innerText = `[Active] ${name}`;
      inspType.innerText = type;
      inspVal.innerText = val;
    });
  });

  // Toggle Current Flow Animation
  btnToggleAnim.addEventListener('click', () => {
    animationEnabled = !animationEnabled;
    if (animationEnabled) {
      resonantFlowWire.style.animation = 'dashFlow 1.2s linear infinite';
      btnToggleAnim.innerHTML = '<span class="btn-icon">⚡</span> Animation: Active';
    } else {
      resonantFlowWire.style.animation = 'none';
      btnToggleAnim.innerHTML = '<span class="btn-icon">⏸</span> Animation: Paused';
    }
  });

  // Reset View
  document.getElementById('btn-reset-zoom').addEventListener('click', () => {
    const svg = document.getElementById('schematic-svg');
    svg.setAttribute('viewBox', '0 0 920 420');
  });
}

// --- 5. Controls and Presets ---
function setupControls() {
  // Slider Value Listeners
  sliderPout.addEventListener('input', (e) => {
    const kw = (parseFloat(e.target.value) / 1000).toFixed(1);
    valPout.innerText = `${kw} kW`;
  });

  sliderFsw.addEventListener('input', (e) => {
    const khz = (parseFloat(e.target.value) / 1000).toFixed(1);
    valFsw.innerText = `${khz} kHz`;
  });

  sliderDeadtime.addEventListener('input', (e) => {
    valDeadtime.innerText = `${e.target.value} ns`;
  });

  sliderVin.addEventListener('input', (e) => {
    valVin.innerText = `${e.target.value} V`;
  });

  // Presets
  const presetButtons = document.querySelectorAll('.btn-preset');
  presetButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      presetButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      if (btn.dataset.fsw) {
        const fsw = btn.dataset.fsw;
        sliderFsw.value = fsw;
        valFsw.innerText = `${(parseFloat(fsw) / 1000).toFixed(1)} kHz`;
      }
      if (btn.dataset.pout) {
        const pout = btn.dataset.pout;
        sliderPout.value = pout;
        valPout.innerText = `${(parseFloat(pout) / 1000).toFixed(1)} kW`;
      }

      // Auto-trigger simulation when a preset is selected
      runSimulation();
    });
  });

  // Manual Simulate Button
  btnSimulate.addEventListener('click', runSimulation);
}

// --- 6. AI Design Copilot & Discussion ---
function setupAiCopilot() {
  const chips = document.querySelectorAll('.ai-chip');
  const contentBox = document.getElementById('ai-response-content');

  chips.forEach(chip => {
    chip.addEventListener('click', async () => {
      chips.forEach(c => c.classList.remove('active'));
      chip.classList.add('active');

      const topic = chip.dataset.topic;
      const p_out = sliderPout.value;
      const f_sw = sliderFsw.value;

      contentBox.innerHTML = '<span class="pulse-dot"></span> <em>Consulting AI Model & Power Electronics Engine...</em>';

      try {
        const res = await fetch(`/api/llc/ai_consult?topic=${topic}&p_out=${p_out}&f_sw=${f_sw}`);
        const data = await res.json();
        if (data.status === 'SUCCESS') {
          const op = data.operating_point;
          contentBox.innerHTML = `
            <div style="margin-bottom:6px; font-weight:700; color:#38bdf8;">⚡ ${data.topic.toUpperCase().replace('_', ' ')}:</div>
            <div style="margin-bottom:8px; line-height:1.4;">${data.expert_advice}</div>
            <div style="display:flex; flex-wrap:wrap; gap:8px; font-size:11px; background:rgba(255,255,255,0.04); padding:6px 10px; border-radius:6px; font-family:'JetBrains Mono';">
              <span>R_load: <strong style="color:#10b981;">${op.load_resistance_ohms} Ω</strong></span>
              <span>Z0: <strong style="color:#38bdf8;">${op.characteristic_impedance_ohms} Ω</strong></span>
              <span>Q: <strong style="color:#fbbf24;">${op.quality_factor_q}</strong></span>
              <span>k: <strong style="color:#a855f7;">${op.inductance_ratio_k}</strong></span>
              <span>I_mag(pk): <strong style="color:#f43f5e;">${op.magnetizing_peak_current_amps} A</strong></span>
              <span>t_dead(min): <strong style="color:#34d399;">${op.min_dead_time_ns} ns</strong></span>
            </div>
          `;
        }
      } catch (err) {
        contentBox.innerText = `AI Consult error: ${err.message}`;
      }
    });
  });
}

// --- 7. Initialization ---
document.addEventListener('DOMContentLoaded', () => {
  initCharts();
  setupSchematicInteractions();
  setupControls();
  setupAiCopilot();

  // Run initial simulation on load
  runSimulation();
});
