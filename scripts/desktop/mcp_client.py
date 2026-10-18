#!/usr/bin/env python3
"""Reusable MCP Client for GeckoCIRCUITS.

Communicates with gecko-mcp.jar over stdio using JSON-RPC 2.0.
"""

import json
import subprocess
import threading
from pathlib import Path
from typing import Any, Dict, Optional

REPO_ROOT = Path(__file__).resolve().parents[2]


class GeckoMcpClient:
    def __init__(self, jar_path: Optional[Path] = None, java_path: Optional[Path] = None):
        self.java_path = java_path or (REPO_ROOT / "desktop" / "app" / "engine" / "runtime" / "bin" / "java.exe")
        self.jar_path = jar_path or (REPO_ROOT / "desktop" / "app" / "engine" / "gecko-mcp.jar")
        if not self.jar_path.exists():
            target_jar = REPO_ROOT / "backend" / "gecko-mcp" / "target" / "gecko-mcp-1.0-jar-with-dependencies.jar"
            if target_jar.exists():
                self.jar_path = target_jar

        self.proc: Optional[subprocess.Popen] = None
        self._next_id = 1
        self._lock = threading.Lock()
        self.stderr_lines = []

    def start(self):
        cmd = [str(self.java_path), "-jar", str(self.jar_path)]
        self.proc = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=0,
            cwd=str(REPO_ROOT),
        )

        def read_stderr():
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
                "clientInfo": {"name": "gecko-agent-client", "version": "1.0"},
            },
        }
        resp = self._send_and_receive(init_req)
        if "error" in resp:
            raise RuntimeError(f"Initialization failed: {resp['error']}")

        notif = {"jsonrpc": "2.0", "method": "notifications/initialized"}
        self._send_notification(notif)
        return resp.get("result", {})

    def _alloc_id(self) -> int:
        with self._lock:
            i = self._next_id
            self._next_id += 1
            return i

    def _send_notification(self, notif: Dict[str, Any]):
        data = json.dumps(notif).encode("utf-8") + b"\n"
        self.proc.stdin.write(data)
        self.proc.stdin.flush()

    def _send_and_receive(self, req: Dict[str, Any]) -> Dict[str, Any]:
        data = json.dumps(req).encode("utf-8") + b"\n"
        with self._lock:
            self.proc.stdin.write(data)
            self.proc.stdin.flush()
            while True:
                line = self.proc.stdout.readline()
                if not line:
                    raise RuntimeError("EOF while reading MCP response")
                text = line.decode("utf-8", errors="replace").strip()
                if not text:
                    continue
                if text.startswith("{"):
                    try:
                        return json.loads(text)
                    except Exception:
                        pass
                self.stderr_lines.append(f"[STDOUT_SKIP] {text}")

    def call_tool(self, name: str, arguments: Optional[Dict[str, Any]] = None) -> Any:
        req = {
            "jsonrpc": "2.0",
            "id": self._alloc_id(),
            "method": "tools/call",
            "params": {
                "name": name,
                "arguments": arguments or {},
            },
        }
        resp = self._send_and_receive(req)
        if "error" in resp:
            raise RuntimeError(f"Tool {name} error: {resp['error']}")
        res = resp.get("result", {})
        if res.get("isError"):
            msg = res.get("content", [{}])[0].get("text", "Unknown tool error")
            raise RuntimeError(f"Tool {name} failed: {msg}")
        contents = res.get("content", [])
        if contents and contents[0].get("type") == "text":
            text = contents[0].get("text", "")
            try:
                return json.loads(text)
            except Exception:
                return text
        return res

    def close(self):
        if self.proc:
            try:
                self.proc.terminate()
                self.proc.wait(timeout=2)
            except Exception:
                self.proc.kill()
            self.proc = None

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()


if __name__ == "__main__":
    with GeckoMcpClient() as client:
        status = client.call_tool("gecko_server_status")
        print("Gecko Server Status:", json.dumps(status, indent=2))
