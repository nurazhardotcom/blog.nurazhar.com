#!/usr/bin/env python3
"""Local timing harness for the homepage build pipeline.

Measures wall-clock seconds for `bb build` and `bb validate-links`,
once COLD (no public/ output, so d2 must re-render every SVG) and then
N_WARM cycles back-to-back (pandoc + d2 inputs cached on disk by the
runtime).

Outputs a markdown-friendly table at the end so the post can quote
real numbers. Pure file-I/O + subprocess — no project code touched.
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "public"
N_WARM = int(os.environ.get("N_WARM", "6"))


def run(label: str, *args: str) -> float:
    """Run a command silently, return elapsed seconds."""
    t0 = time.perf_counter()
    r = subprocess.run(
        list(args),
        cwd=str(ROOT),
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    t1 = time.perf_counter()
    if r.returncode != 0:
        print(f"!! {label} failed with exitcode {r.returncode}", file=sys.stderr)
        sys.exit(r.returncode)
    return t1 - t0


def fmt(s: float) -> str:
    return f"{s:6.2f}s"


def main() -> int:
    if not (ROOT / "bb.edn").exists():
        print(f"!! {ROOT} does not look like the homepage repo", file=sys.stderr)
        return 2

    # === COLD: wipe public/ so d2 has no pre-rendered SVG cache
    print(f"# COLD (rm -rf public/, no d2 svg cache, no stale output)")
    if PUBLIC.exists():
        shutil.rmtree(PUBLIC)
    cold_build = run("cold bb build        ", "bb", "build")
    cold_val = run("cold bb validate-links", "bb", "validate-links")
    print(f"  bb build          {fmt(cold_build)}")
    print(f"  bb validate-links {fmt(cold_val)}")
    print()

    # === WARM: back-to-back cycles, each starting from a populated public/
    build_samples: list[float] = []
    val_samples: list[float] = []
    print(f"# WARM (public/ populated; N_WARM={N_WARM} back-to-back cycles)")
    header = (
        f"  {'cycle':<6} {'bb build':>10} {'bb validate-links':>20} "
        f"{'cycle total':>14}"
    )
    print(header)
    for i in range(1, N_WARM + 1):
        b = run(f"warm{i} bb build         ", "bb", "build")
        v = run(f"warm{i} bb validate-links", "bb", "validate-links")
        build_samples.append(b)
        val_samples.append(v)
        print(f"  {i:<6} {fmt(b):>10} {fmt(v):>20} {fmt(b + v):>14}")
    print()

    def stats(samples: list[float]) -> tuple[float, float, float, float]:
        return min(samples), max(samples), sum(samples) / len(samples), samples[len(samples) // 2]

    bm, bM, bmavg, bmed = stats(build_samples)
    vm, vM, vmavg, vmed = stats(val_samples)
    cycle = [b + v for b, v in zip(build_samples, val_samples)]
    cm, cM, cavg, cmed = stats(cycle)

    print("# Summary (warm cycles, N={})".format(N_WARM))
    summary = (
        f"  {'metric':<22} {'min':>8} {'median':>8} {'mean':>8} {'max':>8}\n"
        f"  {'bb build':<22} {fmt(bm):>8} {fmt(bmed):>8} {fmt(bmavg):>8} {fmt(bM):>8}\n"
        f"  {'bb validate-links':<22} {fmt(vm):>8} {fmt(vmed):>8} {fmt(vmavg):>8} {fmt(vM):>8}\n"
        f"  {'full pipeline equiv':<22} {fmt(cm):>8} {fmt(cmed):>8} {fmt(cavg):>8} {fmt(cM):>8}"
    )
    print(summary)

    # === Markdown table ready to paste into the post
    md = (
        "| Cycle | `bb build` | `bb validate-links` | Full pipeline |\n"
        "|---|---:|---:|---:|\n"
        f"| **Cold** (no `public/` cache) | {cold_build:.2f}s | {cold_val:.2f}s | {cold_build + cold_val:.2f}s |\n"
    )
    md += (
        f"| **Warm min** | {bm:.2f}s | {vm:.2f}s | {cm:.2f}s |\n"
        f"| **Warm median** | {bmed:.2f}s | {vmed:.2f}s | {cmed:.2f}s |\n"
        f"| **Warm mean** | {bmavg:.2f}s | {vmavg:.2f}s | {cavg:.2f}s |\n"
        f"| **Warm max** | {bM:.2f}s | {vM:.2f}s | {cM:.2f}s |\n"
    )
    print()
    print("# Markdown table (for the blog post)")
    print(md)

    # Persist for the orchestrator
    Path("/tmp/bench_results.md").write_text(md)
    Path("/tmp/bench_results.txt").write_text(
        f"cold_build={cold_build:.3f}\n"
        f"cold_validate={cold_val:.3f}\n"
        f"warm_build_min={bm:.3f} median={bmed:.3f} mean={bmavg:.3f} max={bM:.3f}\n"
        f"warm_validate_min={vm:.3f} median={vmed:.3f} mean={vmavg:.3f} max={vM:.3f}\n"
        f"warm_pipeline_min={cm:.3f} median={cmed:.3f} mean={cavg:.3f} max={cM:.3f}\n"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
