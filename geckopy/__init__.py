"""geckopy - Python SDK for GeckoCIRCUITS simulation engine."""

from .circuit import Circuit, ValidationReport
from .client import GeckoClient, get_default_client
from .result import Metrics, SimulationResult, Waveforms

__version__ = "1.0.0"

# Convenient top-level functions
open = Circuit.open
create = Circuit.create


def server_status() -> dict:
    """Checks the GeckoCIRCUITS simulation engine and Java environment status."""
    return get_default_client().call_tool("gecko_server_status")


def reload_server() -> dict:
    """Performs a graceful reload/reset of the simulation engine."""
    return get_default_client().call_tool("gecko_server_reload")


__all__ = [
    "Circuit",
    "SimulationResult",
    "Metrics",
    "Waveforms",
    "ValidationReport",
    "GeckoClient",
    "open",
    "create",
    "server_status",
    "reload_server",
]
