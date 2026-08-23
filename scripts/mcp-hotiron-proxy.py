#!/usr/bin/env python3
"""stdio MCP proxy to a running analyzer (--mcp on 127.0.0.1:8765)."""
from __future__ import annotations

import os
import socket
import sys

HOST = os.environ.get("HOTIRON_MCP_HOST", "127.0.0.1")
PORT = int(os.environ.get("HOTIRON_MCP_PORT", "8765"))


def main() -> int:
    try:
        sock = socket.create_connection((HOST, PORT), timeout=3)
    except OSError as exc:
        sys.stderr.write(
            f"No copy -- HotIron MCP is QRT on {HOST}:{PORT} ({exc}).\n"
            "QRV with: make mcp   (or the launcher --mcp)\n"
        )
        return 1
    sock.settimeout(None)
    stdin = sys.stdin.buffer
    stdout = sys.stdout.buffer

    def pump(src, dst):
        try:
            while True:
                chunk = src.read(4096) if hasattr(src, "read") else src.recv(4096)
                if not chunk:
                    break
                if hasattr(dst, "write"):
                    dst.write(chunk)
                    dst.flush()
                else:
                    dst.sendall(chunk)
        except OSError:
            pass

    import threading

    t = threading.Thread(target=pump, args=(sock, stdout), daemon=True)
    t.start()
    try:
        while True:
            data = stdin.read(4096)
            if not data:
                break
            sock.sendall(data)
    except BrokenPipeError:
        pass
    finally:
        try:
            sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        sock.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
