#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ROOT / "app" / "build.gradle.kts"
README = ROOT / "README.md"
CHANGELOG = ROOT / "CHANGELOG.md"
RELEASES = ROOT / "docs" / "releases"

VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")


def beta_code(version: str) -> int:
    match = VERSION_RE.fullmatch(version)
    if not match:
        raise SystemExit("Version must look like MAJOR.MINOR.PATCH, e.g. 0.17.1")
    major, minor, patch = map(int, match.groups())
    return major * 1_000_000 + minor * 100 + patch


def replace_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.MULTILINE)
    if count != 1:
        raise SystemExit(f"Could not update {label}")
    return updated


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 scripts/bump_beta.py 0.17.1")

    version = sys.argv[1].removeprefix("v").removesuffix(".beta")
    code = beta_code(version)
    tag = f"v{version}.beta"

    gradle = GRADLE.read_text(encoding="utf-8")
    gradle = replace_once(
        gradle,
        r"(create\(\"beta\"\)\s*\{.*?versionCode\s*=\s*)\d+",
        rf"\g<1>{code}",
        "beta versionCode",
    )
    gradle = replace_once(
        gradle,
        r"(create\(\"beta\"\)\s*\{.*?versionName\s*=\s*\")[^\"]+(\")",
        rf"\g<1>{version}\g<2>",
        "beta versionName",
    )
    GRADLE.write_text(gradle, encoding="utf-8")

    readme = README.read_text(encoding="utf-8")
    readme = re.sub(r"v\d+\.\d+\.\d+\.beta", tag, readme)
    readme = re.sub(r"\| \*\*Android versionCode\*\* \| `\d+` \|", f"| **Android versionCode** | `{code}` |", readme)
    README.write_text(readme, encoding="utf-8")

    changelog = CHANGELOG.read_text(encoding="utf-8")
    heading = f"## {version}.beta — Reliability\n"
    if heading not in changelog:
        changelog = changelog.replace(
            "# Changelog\n",
            "# Changelog\n\n"
            + heading
            + "\n### Added\n- Reliability work in progress.\n"
            + "\n### Changed\n- Beta release preparation.\n"
            + "\n### Fixed\n- Bumped beta versionCode to **"
            + str(code)
            + f"** and beta versionName to `{version}.beta`.\n",
            1,
        )
        CHANGELOG.write_text(changelog, encoding="utf-8")

    RELEASES.mkdir(parents=True, exist_ok=True)
    release_file = RELEASES / f"{tag}.md"
    if not release_file.exists():
        release_file.write_text(
            "## Added\n- Reliability improvements and regression coverage.\n\n"
            "## Changed\n- Internal release hardening.\n\n"
            "## Fixed\n- Stability fixes for this beta.\n",
            encoding="utf-8",
        )

    print(f"Prepared {tag} / versionCode {code}")


if __name__ == "__main__":
    main()
