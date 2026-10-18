"""GeckoCIRCUITS MCP Client for Python.

Manages process lifecycle and JSON-RPC 2.0 communication with gecko-mcp.jar over stdio.
"""

from __future__ import annotations

import atexit
import json
import os
import shutil
import subprocess
import threading
from pathlib import Path
from typing import Any, Dict, Optional


def find_repo_root() -> Path:
    """Finds the root of the GeckoCIRCUITS repository."""
    # Check environment variable first
    if "GECKO_HOME" in os.environ:
        p = Path(os.environ["GECKO_HOME"]).resolve()
        if p.exists():
            return p

    # Traverse parents from this file
    cur = Path(__file__).resolve().parent
    for parent in [cur] + list(cur.parents):
        if (parent / "backend" / "gecko-mcp").exists() or (parent / "pom.xml").exists():
            return parent
    return Path.cwd().resolve()


class GeckoClient:
    """Headless GeckoCIRCUITS client communicating with the backend over JSON-RPC 2.0."""

    def __init__(self, jar_path: Optional[Path | str] = None, java_path: Optional[Path | str] = None):
        self.repo_root = find_repo_root()
        
        # Determine Java binary
        if java_path:
            self.java_path = Path(java_path)
        else:
            bundled_win = self.repo_root / "desktop" / "app" / "engine" / "runtime" / "bin" / "java.exe"
            bundled_nix = self.repo_root / "desktop" / "app" / "engine" / "runtime" / "bin" / "java"
            if bundled_win.exists():
                self.java_path = bundled_win
            elif bundled_nix.exists():
                self.java_path = bundled_nix
            else:
                sys_java = shutil.which("java")
                self.java_path = Path(sys_java) if sys_java else Path("java")

        # Determine JAR binary
        if jar_path:
            self.jar_path = Path(jar_path)
        else:
            desktop_jar = self.repo_root / "desktop" / "app" / "engine" / "gecko-mcp.jar"
            target_jar = self.repo_root / "backend" / "gecko-mcp" / "target" / "gecko-mcp.jar"
            if desktop_jar.exists():
                self.jar_path = desktop_jar
            elif target_jar.exists():
                self.jar_path = target_jar
            else:
                self.jar_path = desktop_jar

        self.proc: Optional[subprocess.Popen] = None
        self._next_id = 1
        self._lock = threading.Lock()
        self.stderr_lines: list[str] = []

    def start(self) -> dict:
        """Starts the backend subprocess and completes the MCP handshake."""
        with self._lock:
            if self.proc is not None and self.proc.poll() is None:
                return {}

            cmd = [str(self.java_path), "-jar", str(self.jar_path)]
            self.proc = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                bufsize=0,
                cwd=str(self.repo_root),
            )

            def read_stderr():
                assert self.proc is not None
                assert self.proc.stderr is not None
                for line in iter(self.proc.stderr.readline, b""):
                    text = line.decode("utf-8", errors="replace").rstrip()
                    self.stderr_lines.append(text)

            threading.Thread(target=read_stderr, daemon=True).start()

            # MCP handshake
            init_req = {
                "jsonrpc": "2.0",
                "id": self._alloc_id(),
                "method": "initialize",
                "params": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {},
                    "clientInfo": {"name": "geckopy-sdk", "version": "1.0"},
                },
            }
            resp = self._raw_send_receive(init_req)
            if "error" in resp:
                raise RuntimeError(f"Backend initialization failed: {resp['error']}")

            notif = {"jsonrpc": "2.0", "method": "notifications/initialized"}
            data = json.dumps(notif).encode("utf-8") + b"\n"
            assert self.proc.stdin is not None
            self.proc.stdin.write(data)
            self.proc.stdin.flush()
            return resp.get("result", {})

    def _alloc_id(self) -> int:
        i = self._next_id
        self._next_id += 1
        return i

    def _raw_send_receive(self, req: dict[str, Any]) -> dict[str, Any]:
        assert self.proc is not None
        assert self.proc.stdin is not None
        assert self.proc.stdout is not None
        data = json.dumps(req).encode("utf-8") + b"\n"
        self.proc.stdin.write(data)
        self.proc.stdin.flush()

        while True:
            line = self.proc.stdout.readline()
            if not line:
                raise RuntimeError("EOF while communicating with gecko-mcp backend")
            text = line.decode("utf-8", errors="replace").strip()
            if not text:
                continue
            if text.startswith("{"):
                try:
                    return json.loads(text)
                except Exception:
                    pass
            self.stderr_lines.append(f"[STDOUT_SKIP] {text}")

    def call_tool(self, name: str, arguments: Optional[dict[str, Any]] = None) -> Any:
        """Invokes an MCP tool by name and parses the JSON response."""
        if self.proc is None or self.proc.poll() is not None:
            self.start()

        with self._lock:
            req = {
                "jsonrpc": "2.0",
                "id": self._alloc_id(),
                "method": "tools/call",
                "params": {
                    "name": name,
                    "arguments": arguments or {},
                },
            }
            resp = self._raw_send_receive(req)

        if "error" in resp:
            raise RuntimeError(f"Tool '{name}' RPC error: {resp['error']}")

        res = resp.get("result", {})
        if res.get("isError"):
            contents = res.get("content", [{}])
            msg = contents[0].get("text", "Unknown tool error") if contents else "Unknown tool error"
            raise RuntimeError(f"Tool '{name}' failed: {msg}")

        contents = res.get("content", [])
        if contents and contents[0].get("type") == "text":
            text = contents[0].get("text", "")
            try:
                return json.loads(text)
            except Exception:
                return text
        return res

    def close(self):
        """Terminates the backend subprocess cleanly."""
        with self._lock:
            if self.proc is not None:
                try:
                    self.proc.terminate()
                    self.proc.wait(timeout=1.5)
                except Exception:
                    try:
                        self.proc.kill()
                    except Exception:
                        pass
                self.proc = None

    def __enter__(self) -> GeckoClient:
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


# Shared singleton client
_DEFAULT_CLIENT: Optional[GeckoClient] = None
_DEFAULT_LOCK = threading.Lock()


def get_default_client() -> GeckoClient:
    """Returns the process-wide shared GeckoClient instance."""
    global _DEFAULT_CLIENT
    with _DEFAULT_LOCK:
        if _DEFAULT_CLIENT is None:
            _DEFAULT_CLIENT = GeckoClient()
            atexit.register(_cleanup_default_client)
        return _DEFAULT_CLIENT


def _cleanup_default_client():
    global _DEFAULT_CLIENT
    if _DEFAULT_CLIENT is not None:
        _DEFAULT_CLIENT.close()
        _DEFAULT_CLIENT = None
