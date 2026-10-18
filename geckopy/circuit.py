"""High-level Circuit abstraction for GeckoCIRCUITS Python SDK."""

from __future__ import annotations

from pathlib import Path
from typing import Any, Dict, List, Optional, Union

from .client import GeckoClient, get_default_client
from .result import Metrics, SimulationResult, Waveforms


class ValidationReport:
    """Structured report returned by ``ckt.validate()``."""

    def __init__(self, raw: dict[str, Any]):
        self.raw = dict(raw)
        self.is_valid: bool = bool(self.raw.get("valid", False))
        self.diagnostics: list[dict[str, Any]] = list(self.raw.get("diagnostics", []))

    @property
    def errors(self) -> list[dict[str, Any]]:
        return [d for d in self.diagnostics if str(d.get("level")).upper() == "ERROR"]

    @property
    def warnings(self) -> list[dict[str, Any]]:
        return [d for d in self.diagnostics if str(d.get("level")).upper() == "WARNING"]

    def __bool__(self) -> bool:
        return self.is_valid

    def __repr__(self) -> str:
        return f"<ValidationReport valid={self.is_valid} errors={len(self.errors)} warnings={len(self.warnings)}>"


class Circuit:
    """Represents a GeckoCIRCUITS simulation circuit project (.ipes)."""

    def __init__(self, path: Union[str, Path], client: Optional[GeckoClient] = None):
        self.path: Path = Path(path).resolve()
        self.client: GeckoClient = client or get_default_client()
        self._inspection_cache: Optional[dict[str, Any]] = None

    @classmethod
    def open(cls, path: Union[str, Path], client: Optional[GeckoClient] = None) -> Circuit:
        """Opens an existing GeckoCIRCUITS circuit file (.ipes or .ipes.gz)."""
        p = Path(path)
        if not p.is_absolute():
            # Resolve against client repo root
            c = client or get_default_client()
            p = (c.repo_root / p).resolve()

        if not p.exists():
            raise FileNotFoundError(f"Circuit file not found: {p}")
        return cls(p, client)

    @classmethod
    def create(
        cls,
        definition: dict[str, Any],
        output_path: Optional[Union[str, Path]] = None,
        client: Optional[GeckoClient] = None,
    ) -> Circuit:
        """Creates a new circuit file programmatically from a definition dictionary."""
        c = client or get_default_client()
        d = dict(definition)
        if output_path is not None:
            d["output_path"] = str(output_path)

        if "output_path" not in d:
            raise ValueError("output_path must be specified either in definition or as argument")

        res = c.call_tool("gecko_create_circuit", d)
        out = Path(res.get("output_path", d["output_path"])).resolve()
        return cls(out, client=c)

    def set_parameter(self, component_name: str, parameter: str, value: Any) -> Circuit:
        """Patches a component's parameter value in the circuit."""
        self.client.call_tool(
            "gecko_patch_component",
            {
                "circuit_path": str(self.path),
                "component_name": component_name,
                "parameters": {parameter: value},
            },
        )
        self._inspection_cache = None
        return self

    def set_script_code(
        self,
        block_name: str,
        source_code: str,
        static_code: str = "",
        static_vars: str = "",
    ) -> Circuit:
        """Updates the step code, static functions, or static variables of a control script block."""
        self.client.call_tool(
            "gecko_set_script_code",
            {
                "circuit_path": str(self.path),
                "component_name": block_name,
                "source_code": source_code,
                "static_code": static_code,
                "static_variables": static_vars,
            },
        )
        self._inspection_cache = None
        return self

    def validate(self, level: str = "compile") -> ValidationReport:
        """Runs pre-flight Design Rule Checks (DRC) and script compilation checks."""
        res = self.client.call_tool(
            "gecko_validate_circuit",
            {
                "circuit_path": str(self.path),
                "level": level,
            },
        )
        return ValidationReport(res)

    def simulate(
        self,
        duration: Optional[float] = None,
        dt: Optional[float] = None,
        solver: Optional[str] = None,
        signals: Optional[list[str]] = None,
        **kwargs: Any,
    ) -> SimulationResult:
        """Runs a headless simulation and returns the parsed results, metrics, and waveforms."""
        args: dict[str, Any] = {"circuit_path": str(self.path)}
        if duration is not None:
            args["duration"] = duration
        if dt is not None:
            args["dt"] = dt
        if solver is not None:
            args["matrix_solver"] = solver
        if signals is not None:
            args["signals"] = signals
        args.update(kwargs)

        res = self.client.call_tool("gecko_get_waveforms", args)
        return SimulationResult(res)

    def get_waveforms(
        self,
        duration: Optional[float] = None,
        dt: Optional[float] = None,
        max_points: int = 2000,
        signals: Optional[list[str]] = None,
        **kwargs: Any,
    ) -> Waveforms:
        """Retrieves sampled time-series waveforms and per-channel statistics."""
        res = self.simulate(duration=duration, dt=dt, max_points=max_points, signals=signals, **kwargs)
        return res.waveforms

    def measure_metrics(self, **kwargs: Any) -> Metrics:
        """Computes comprehensive multi-domain metrics (electrical, thermal, magnetic, mechanical)."""
        args: dict[str, Any] = {"circuit_path": str(self.path)}
        args.update(kwargs)
        res = self.client.call_tool("gecko_measure_metrics", args)
        return Metrics(res)

    def export_svg(
        self,
        output_path: Optional[Union[str, Path]] = None,
        theme: str = "light",
        scale: float = 1.0,
    ) -> str:
        """Exports a clean vector SVG schematic of the circuit."""
        args: dict[str, Any] = {
            "circuit_path": str(self.path),
            "theme": theme,
            "scale": scale,
        }
        if output_path is not None:
            out_p = Path(output_path).resolve()
            args["output_path"] = str(out_p)

        res = self.client.call_tool("gecko_export_schematic_svg", args)
        return str(res.get("svg", ""))

    def inspect(self) -> dict[str, Any]:
        """Returns structured circuit introspection (components, nets, settings, couplings)."""
        res = self.client.call_tool("gecko_inspect_circuit", {"circuit_path": str(self.path)})
        self._inspection_cache = res
        return res

    def list_signals(self) -> list[str]:
        """Discovers all observable signals (voltmeters, ammeters, scopes, switches, thermal, magnetic)."""
        res = self.client.call_tool("gecko_list_signals", {"circuit_path": str(self.path)})
        if isinstance(res, list):
            return list(res)
        if isinstance(res, dict):
            return list(res.get("signals", []))
        return []

    @property
    def components(self) -> list[dict[str, Any]]:
        """List of all circuit components from inspection."""
        if self._inspection_cache is None:
            self.inspect()
        assert self._inspection_cache is not None
        comps = []
        for key in ["lk_components", "control_components", "thermal_components", "special_components"]:
            comps.extend(self._inspection_cache.get(key, []))
        return comps

    def __repr__(self) -> str:
        return f"<Circuit '{self.path.name}'>"
