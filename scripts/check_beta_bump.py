#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app" / "build.gradle.kts"
VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")

FUNCTIONAL_PREFIXES = (
    "app/src/",
    "app/build.gradle.kts",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
    "gradle/",
)


def run(*args: str) -> str:
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def tuple_version(value: str) -> tuple[int, int, int]:
    match = VERSION_RE.fullmatch(value)
    if not match:
        raise ValueError(value)
    return tuple(map(int, match.groups()))


def current_beta_version() -> str:
    text = GRADLE.read_text(encoding="utf-8")
    block = re.search(r'create\("beta"\)\s*\{(?P<body>.*?)\n\s*\}', text, re.DOTALL)
    if not block:
        raise SystemExit("Could not find beta flavor in app/build.gradle.kts")
    match = re.search(r'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"', block.group("body"))
    if not match:
        raise SystemExit("Could not find beta versionName")
    return match.group(1)


def beta_tags() -> list[tuple[tuple[int, int, int], str]]:
    raw = run("git", "tag", "--list", "v*.beta*")
    result: list[tuple[tuple[int, int, int], str]] = []
    for tag in raw.splitlines():
        core = tag.removeprefix("v").split(".beta", 1)[0]
        try:
            result.append((tuple_version(core), tag))
        except ValueError:
            continue
    return sorted(result)


def main() -> None:
    tags = beta_tags()
    if not tags:
        print("No previous beta tag found; version-bump guard skipped.")
        return

    latest_version, latest_tag = tags[-1]
    current = tuple_version(current_beta_version())

    changed = run("git", "diff", "--name-only", f"{latest_tag}...HEAD").splitlines()
    functional = [
        path for path in changed
        if any(path == prefix or path.startswith(prefix) for prefix in FUNCTIONAL_PREFIXES)
    ]

    if not functional:
        print(f"No functional changes since {latest_tag}; beta bump not required.")
        return

    if current <= latest_version:
        print(f"Functional changes exist after {latest_tag}, but beta is still {'.'.join(map(str, current))}.", file=sys.stderr)
        print("Bump the beta version before merging/releasing.", file=sys.stderr)
        for path in functional:
            print(f"  - {path}", file=sys.stderr)
        raise SystemExit(1)

    print(
        f"Beta bump OK: {latest_tag} -> v{'.'.join(map(str, current))}.beta "
        f"({len(functional)} functional file(s) changed)."
    )


if __name__ == "__main__":
    main()
