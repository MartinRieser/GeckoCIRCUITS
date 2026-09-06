#!/usr/bin/env python3
"""Standard MCP JSON-RPC Stdio Client for GeckoCIRCUITS.

Speaks the official Model Context Protocol (JSON-RPC 2.0) over stdin/stdout
to launch-mcp.py, demonstrating autonomous tool and resource invocation.
"""

import json
import subprocess
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
LAUNCHER = REPO_ROOT / "scripts" / "desktop" / "launch-mcp.py"


class McpSession:
    def __init__(self):
        self.proc = subprocess.Popen(
            [sys.executable, str(LAUNCHER)],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
            cwd=str(REPO_ROOT),
        )
        self.msg_id = 0

    def send_notification(self, method: str, params: dict = None):
        req = {
            "jsonrpc": "2.0",
            "method": method,
        }
        if params is not None:
            req["params"] = params
        line = json.dumps(req)
        self.proc.stdin.write(line + "\n")
        self.proc.stdin.flush()

    def send_request(self, method: str, params: dict = None) -> dict:
        self.msg_id += 1
        req = {
            "jsonrpc": "2.0",
            "id": self.msg_id,
            "method": method,
        }
        if params is not None:
            req["params"] = params
        
        line = json.dumps(req)
        self.proc.stdin.write(line + "\n")
        self.proc.stdin.flush()

        # Read JSON response (server writes one JSON line per message)
        while True:
            resp_line = self.proc.stdout.readline()
            if not resp_line:
                stderr = self.proc.stderr.read()
                raise RuntimeError(f"MCP server exited unexpectedly: {stderr}")
            resp_line = resp_line.strip()
            if not resp_line:
                continue
            try:
                resp = json.loads(resp_line)
                if "id" in resp and resp["id"] == self.msg_id:
                    if "error" in resp:
                        raise RuntimeError(f"MCP error {resp['error']}")
                    return resp.get("result", {})
            except json.JSONDecodeError:
                continue

    def initialize(self):
        result = self.send_request("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "mcp-test-client", "version": "1.0.0"}
        })
        self.send_notification("notifications/initialized")
        return result

    def list_tools(self):
        return self.send_request("tools/list")

    def call_tool(self, name: str, arguments: dict):
        result = self.send_request("tools/call", {
            "name": name,
            "arguments": arguments
        })
        # Unpack text content if present
        if "content" in result:
            for item in result["content"]:
                if item.get("type") == "text":
                    try:
                        return json.loads(item["text"])
                    except json.JSONDecodeError:
                        return item["text"]
        return result

    def list_resources(self):
        return self.send_request("resources/list")

    def read_resource(self, uri: str):
        return self.send_request("resources/read", {"uri": uri})

    def close(self):
        try:
            self.proc.terminate()
            self.proc.wait(timeout=3)
        except Exception:
            self.proc.kill()


if __name__ == "__main__":
    session = McpSession()
    try:
        init = session.initialize()
        print(f"Connected to MCP server: {init.get('serverInfo')}")
        tools = session.list_tools()
        tool_names = [t["name"] for t in tools.get("tools", [])]
        print(f"Available tools ({len(tool_names)}): {tool_names}")
        resources = session.list_resources()
        resource_uris = [r["uri"] for r in resources.get("resources", [])]
        print(f"Available resources ({len(resource_uris)}): {resource_uris}")
    finally:
        session.close()
