"""Simulation results, metrics, and waveforms representation for GeckoCIRCUITS Python SDK."""

from __future__ import annotations

import re
from typing import Any, Dict, Iterator, List, Optional, Union


def _normalize_key(key: str) -> str:
    """Normalizes key names to standard pythonic snake_case identifiers."""
    k = re.sub(r"[^0-9a-zA-Z_]+", "_", key).strip("_").lower()
    return k


class Metrics:
    """Container for multi-domain electrical, thermal, magnetic, and mechanical metrics.

    Allows both dictionary key access (``metrics['v_out_mean']``) and dot attribute access
    (``metrics.v_out_mean``, ``metrics.efficiency``).
    """

    def __init__(self, raw: dict[str, Any]):
        self._raw = dict(raw)
        self._normalized: dict[str, Any] = {}
        self._flatten("", self._raw)

    def _flatten(self, prefix: str, data: Any):
        if isinstance(data, dict):
            for k, v in data.items():
                p = f"{prefix}_{k}" if prefix else k
                norm_p = _normalize_key(p)
                self._normalized[norm_p] = v
                norm_k = _normalize_key(k)
                if norm_k not in self._normalized:
                    self._normalized[norm_k] = v

                # Common aliases
                if "steady_state_dc" in norm_k or "mean" in norm_k:
                    if "v_out" not in self._normalized:
                        self._normalized["v_out_mean"] = v
                if "efficiency" in norm_k:
                    self._normalized["efficiency"] = v
                if "power_factor" in norm_k:
                    self._normalized["power_factor"] = v
                if "ripple" in norm_k:
                    self._normalized["ripple"] = v

                if isinstance(v, (dict, list)):
                    self._flatten(p, v)

    def __getattr__(self, name: str) -> Any:
        norm = _normalize_key(name)
        if norm in self._normalized:
            val = self._normalized[norm]
            return Metrics(val) if isinstance(val, dict) else val
        raise AttributeError(f"'Metrics' object has no metric '{name}'. Available: {list(self._normalized.keys())[:15]}")

    def __getitem__(self, name: str) -> Any:
        norm = _normalize_key(name)
        if norm in self._normalized:
            return self._normalized[norm]
        if name in self._raw:
            return self._raw[name]
        raise KeyError(name)

    def __contains__(self, name: str) -> bool:
        return _normalize_key(name) in self._normalized or name in self._raw

    def get(self, name: str, default: Any = None) -> Any:
        norm = _normalize_key(name)
        if norm in self._normalized:
            return self._normalized[norm]
        return self._raw.get(name, default)

    def to_dict(self) -> dict[str, Any]:
        return dict(self._raw)

    def __repr__(self) -> str:
        keys = list(self._normalized.keys())[:8]
        return f"<Metrics keys={keys}...>"


class Waveforms:
    """Waveform data container supporting dictionary indexing, stats, and dataframe export."""

    def __init__(self, raw: dict[str, Any]):
        self._raw = dict(raw)
        waves = self._raw.get("waveforms", self._raw.get("signals", {}))
        if isinstance(waves, dict):
            self.signals: dict[str, list[float]] = {k: v for k, v in waves.items() if k != "time"}
            self.time: list[float] = list(waves.get("time", self._raw.get("time", [])))
        else:
            self.signals = {}
            self.time = []
        self.statistics: dict[str, dict[str, float]] = self._raw.get(
            "signal_statistics", self._raw.get("statistics", {})
        )

    def __getitem__(self, signal_name: str) -> Any:
        if signal_name in self.signals:
            return self.signals[signal_name]
        if signal_name in self.statistics:
            return self.statistics[signal_name]
        raise KeyError(f"Signal '{signal_name}' not found. Available signals: {list(self.signals.keys())}")

    def __contains__(self, signal_name: str) -> bool:
        return signal_name in self.signals or signal_name in self.statistics

    def keys(self) -> list[str]:
        return list(self.signals.keys())

    def get_statistics(self, signal_name: str) -> dict[str, float]:
        """Returns statistical metrics (min, max, mean, rms, peak_to_peak) for a signal."""
        return self.statistics.get(signal_name, {})

    def to_dataframe(self):
        """Converts waveforms to a pandas DataFrame if pandas is installed."""
        try:
            import pandas as pd
        except ImportError:
            raise ImportError("pandas is required for to_dataframe(). Install it with: pip install pandas")

        data = {}
        if self.time:
            data["time"] = self.time
        for sig, vals in self.signals.items():
            data[sig] = vals
        return pd.DataFrame(data)

    def __repr__(self) -> str:
        return f"<Waveforms signals={list(self.signals.keys())} steps={len(self.time)}>"


class SimulationResult:
    """High-level result of a circuit simulation run."""

    def __init__(self, raw: dict[str, Any]):
        self.raw = dict(raw)
        self.status: str = self.raw.get("status", "UNKNOWN")

        # Time & duration resolution
        raw_dur = self.raw.get("duration", self.raw.get("simulation_time"))
        waves_dict = self.raw.get("waveforms", self.raw.get("signals", {}))
        time_list = waves_dict.get("time", self.raw.get("time", [])) if isinstance(waves_dict, dict) else []

        if raw_dur is not None:
            self.duration: float = float(raw_dur)
        elif time_list:
            self.duration = float(time_list[-1])
        else:
            self.duration = 0.0

        if "dt" in self.raw:
            self.dt: float = float(self.raw["dt"])
        elif len(time_list) > 1:
            self.dt = float(time_list[1] - time_list[0])
        else:
            self.dt = 0.0

        self.total_steps: int = int(self.raw.get("total_time_steps", self.raw.get("total_steps", len(time_list))))
        self.signal_names: list[str] = [k for k in waves_dict.keys() if k != "time"] if isinstance(waves_dict, dict) else []

        # Metrics
        raw_metrics = self.raw.get("metrics", {})
        if not raw_metrics and "signal_statistics" in self.raw:
            raw_metrics = dict(self.raw.get("signal_statistics", {}))
        elif not raw_metrics and "statistics" in self.raw:
            raw_metrics = dict(self.raw.get("statistics", {}))
        self.metrics: Metrics = Metrics(raw_metrics)

        # Waveforms
        self.waveforms: Waveforms = Waveforms(self.raw)

    @property
    def is_success(self) -> bool:
        return self.status == "COMPLETED" or self.status == "SUCCESS"

    def to_dataframe(self):
        return self.waveforms.to_dataframe()

    def __repr__(self) -> str:
        return f"<SimulationResult status='{self.status}' duration={self.duration}s steps={self.total_steps}>"
