#!/usr/bin/env python3
"""Summarize gcov data from native self-tests (no extra Python deps).

Invoked by `make test` after the C/C++ self-tests run. Does not fail the
build if gcov is missing; test binaries already passed or failed on their own.
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


def parse_gcov(path: Path) -> tuple[int, int]:
    """Return (covered, instrumented) executable lines."""
    covered = 0
    instrumented = 0
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        if ":" not in raw:
            continue
        count, rest = raw.split(":", 1)
        count = count.strip()
        if not rest.strip() or rest.lstrip().startswith("0:"):
            continue
        if count in ("-", ""):
            continue
        if count.startswith("-"):
            continue
        instrumented += 1
        if count != "#####" and not count.startswith("====="):
            covered += 1
    return covered, instrumented


def source_from_gcda(gcda: Path, objects: Path) -> Path | None:
    rel = gcda.relative_to(objects)
    stem = rel.with_suffix("")
    for ext in (".c", ".cc", ".cpp"):
        cand = Path(str(stem) + ext)
        if cand.is_file():
            return cand
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--objects", required=True, help="directory of .gcno/.gcda (e.g. obj/gcov)")
    ap.add_argument("--source-root", default="src-c", help="first-party native root")
    ap.add_argument("--out", required=True, help="text report path")
    ap.add_argument("--module-root", default=".", help="directory that contains src-c/")
    args = ap.parse_args()

    objects = Path(args.objects).resolve()
    src_root = Path(args.source_root)
    module_root = Path(args.module_root).resolve()
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)

    if not shutil_which("gcov"):
        msg = "gcov not found; native tests ran but line coverage was not collected.\n"
        out.write_text(msg, encoding="utf-8")
        sys.stderr.write(msg)
        return 0

    gcda_files = sorted(objects.rglob("*.gcda"))
    if not gcda_files:
        msg = f"no .gcda under {objects}; compile self-tests with --coverage.\n"
        out.write_text(msg, encoding="utf-8")
        sys.stderr.write(msg)
        return 0

    report_dir = objects / "gcov-out"
    if report_dir.exists():
        for old in report_dir.glob("*.gcov"):
            old.unlink()
    else:
        report_dir.mkdir(parents=True)

    for gcda in gcda_files:
        src = source_from_gcda(gcda, objects)
        if src is None:
            continue
        src_path = src if src.is_file() else Path(src.as_posix())
        if not src_path.is_file():
            # object dir mirrors source paths: src-c/atsc/foo.cpp
            continue
        if "selftest" in src_path.name:
            continue
        subprocess.run(
            ["gcov", "-p", "-o", str(gcda.parent), str(src_path)],
            cwd=module_root,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )

    for leftover in module_root.glob("*.gcov"):
        dest = report_dir / leftover.name
        leftover.replace(dest)

    src_marker = src_root.as_posix().rstrip("/") + "/"
    rows: list[tuple[str, int, int, float]] = []
    tot_c = tot_i = 0
    for gcov_file in sorted(report_dir.glob("*.gcov")):
        text = gcov_file.read_text(encoding="utf-8", errors="replace")
        src_name = ""
        m = re.search(r"^ *-: *0:Source:(.*)$", text, re.M)
        if m:
            src_name = m.group(1).strip()
        if "selftest" in src_name or "selftest" in gcov_file.name:
            continue
        norm = src_name.replace("\\", "/")
        if src_marker not in norm and not norm.startswith(src_root.as_posix()):
            continue
        if "/usr/" in norm:
            continue
        covered, instrumented = parse_gcov(gcov_file)
        if instrumented == 0:
            continue
        pct = 100.0 * covered / instrumented
        rows.append((src_name, covered, instrumented, pct))
        tot_c += covered
        tot_i += instrumented

    rows.sort(key=lambda r: r[0])
    tot_pct = (100.0 * tot_c / tot_i) if tot_i else 0.0
    lines = [
        "Native gcov (make test self-tests; excludes *selftest* sources)",
        f"Objects: {objects}",
        "",
        f"{'lines':>10}  {'hit':>8}  {'cov':>7}  file",
    ]
    for label, covered, instrumented, pct in rows:
        lines.append(f"{instrumented:10d}  {covered:8d}  {pct:6.1f}%  {label}")
    lines.append("")
    lines.append(f"{tot_i:10d}  {tot_c:8d}  {tot_pct:6.1f}%  TOTAL")
    lines.append("")
    report = "\n".join(lines) + "\n"
    out.write_text(report, encoding="utf-8")
    sys.stdout.write(report)
    return 0


def shutil_which(cmd: str) -> str | None:
    path = os.environ.get("PATH", "")
    for d in path.split(os.pathsep):
        p = Path(d) / cmd
        if p.is_file() and os.access(p, os.X_OK):
            return str(p)
    return None


if __name__ == "__main__":
    raise SystemExit(main())
