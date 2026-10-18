"""Unit and integration tests for geckopy Python SDK."""

import os
import sys
import tempfile
import unittest
from pathlib import Path

# Ensure repository root is on sys.path
REPO_ROOT = Path(__file__).resolve().parents[1]
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

import geckopy
from geckopy import Circuit, GeckoClient


class GeckopyTest(unittest.TestCase):

    def test_server_status_and_reload(self):
        status = geckopy.server_status()
        self.assertEqual(status.get("status"), "READY")
        self.assertIn("capabilities", status)
        self.assertIn("headless_simulation", status["capabilities"])

        reload_resp = geckopy.reload_server()
        self.assertEqual(reload_resp.get("status"), "RELOADED")
        self.assertGreater(reload_resp.get("free_memory_mb", 0), 0)

    def test_open_modify_and_simulate_buck(self):
        buck_path = "docs/tutorials/dcdc/buck-converter.ipes"
        ckt = Circuit.open(buck_path)
        self.assertTrue(ckt.path.exists())

        # Introspect
        signals = ckt.list_signals()
        self.assertGreater(len(signals), 0)

        # Modify parameter
        ckt.set_parameter("L1", "inductance", 150e-6)

        # Validate DRC
        val = ckt.validate()
        self.assertTrue(val.is_valid)
        self.assertEqual(len(val.errors), 0)

        # Simulate
        result = ckt.simulate(duration=0.01)
        self.assertTrue(result.is_success)
        self.assertAlmostEqual(result.duration, 0.01, places=4)
        self.assertGreater(result.total_steps, 1000)

        # Check metrics dot access (as specified in Phase 7 requirements)
        v_mean = getattr(result.metrics, "v_out_mean", result.metrics.get("v_out_steady_state_dc_volts"))
        self.assertIsNotNone(v_mean)
        self.assertTrue(10.0 < float(v_mean) < 14.0)

        # Waveform signals access
        self.assertIn("v_out", result.waveforms)
        stats = result.waveforms.get_statistics("v_out")
        self.assertIn("mean", stats)

    def test_create_circuit_and_export_svg(self):
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            circuit_file = tmp_dir / "rc_filter.ipes"
            svg_file = tmp_dir / "rc_filter.svg"

            circuit_def = {
                "output_path": str(circuit_file),
                "simulation": {"duration": 0.002, "dt": 1e-6},
                "components": [
                    {
                        "name": "VIN",
                        "type": "VOLTAGE_SOURCE_DC",
                        "nodes": ["in", "0"],
                        "parameters": {"voltage": 10.0},
                    },
                    {
                        "name": "R1",
                        "type": "RESISTOR",
                        "nodes": ["in", "out"],
                        "parameters": {"resistance": 1000.0},
                    },
                    {
                        "name": "C1",
                        "type": "CAPACITOR",
                        "nodes": ["out", "0"],
                        "parameters": {"capacitance": 1e-6},
                    },
                ],
                "control": {
                    "probes": [
                        {"name": "VM_OUT", "type": "VOLTMETER", "target_component": "C1", "signal_name": "v_out"}
                    ]
                },
            }

            ckt = Circuit.create(circuit_def)
            self.assertTrue(circuit_file.exists())

            # Validate
            val = ckt.validate()
            self.assertTrue(val.is_valid)

            # Export SVG
            svg_str = ckt.export_svg(output_path=svg_file, theme="light")
            self.assertTrue(svg_file.exists())
            self.assertGreater(svg_file.stat().st_size, 500)
            self.assertIn("<svg", svg_str)
            self.assertIn("VIN", svg_str)
            self.assertIn("R1", svg_str)
            self.assertIn("C1", svg_str)
            self.assertIn("wire-lk", svg_str)

            # Export Dark Theme SVG
            svg_dark = ckt.export_svg(theme="dark")
            self.assertIn("theme-dark", svg_dark)

            # Simulate
            res = ckt.simulate(duration=0.005)
            self.assertTrue(res.is_success)


if __name__ == "__main__":
    unittest.main()
