#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
from html.parser import HTMLParser
import re
import sys
import xml.etree.ElementTree as ET

try:
    import yaml
except ImportError:
    yaml = None

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
JAVA = APP / "src/main/java"
RES = APP / "src/main/res"
ALL_JAVA_ROOTS = [path for path in (APP / "src").glob("*/java") if path.is_dir()]
ERRORS: list[str] = []
WARNINGS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def warn(message: str) -> None:
    WARNINGS.append(message)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


required = [
    "LICENSE",
    "README.md",
    "CHANGELOG.md",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "docs/privacy-policy.md",
    "docs/privacy-policy.html",
    "docs/data-safety.md",
    "docs/play-store-listing.md",
    "docs/play-store-checklist.md",
    "docs/github-beta-updates.md",
    "docs/github-beta-signing.md",
    "docs/glyph-matrix.md",
    ".github/workflows/build-apk.yml",
    ".github/workflows/play-release.yml",
]
for rel in required:
    if not (ROOT / rel).is_file():
        fail(f"Missing required release file: {rel}")

# XML.
for path in sorted((APP / "src").rglob("*.xml")):
    try:
        ET.parse(path)
    except Exception as exc:
        fail(f"Invalid XML {path.relative_to(ROOT)}: {exc}")

# HTML.
class StrictEnoughHtmlParser(HTMLParser):
    pass

for path in sorted((ROOT / "docs").glob("*.html")):
    try:
        parser = StrictEnoughHtmlParser()
        parser.feed(read(path))
        parser.close()
    except Exception as exc:
        fail(f"Invalid HTML {path.relative_to(ROOT)}: {exc}")

# GitHub Actions YAML.
for path in sorted((ROOT / ".github/workflows").glob("*.yml")):
    if yaml is None:
        warn("PyYAML unavailable; skipped workflow YAML parse")
        break
    try:
        parsed = yaml.safe_load(read(path))
        if not isinstance(parsed, dict):
            fail(f"Workflow does not parse to an object: {path.relative_to(ROOT)}")
    except Exception as exc:
        fail(f"Invalid YAML {path.relative_to(ROOT)}: {exc}")

# Kotlin parser-level sanity.
kotlin_files = sorted(path for root in ALL_JAVA_ROOTS for path in root.rglob("*.kt"))
if not kotlin_files:
    fail("No Kotlin source files found")

for path in kotlin_files:
    text = read(path)
    imports = [line for line in text.splitlines() if line.startswith("import ")]
    dupes = sorted({line for line in imports if imports.count(line) > 1})
    if dupes:
        fail(f"Duplicate imports in {path.relative_to(ROOT)}: {dupes}")
    if "import androidx.compose.foundation.layout.weight" in text:
        fail(f"Forbidden internal weight import in {path.relative_to(ROOT)}")
    if any(marker in text for marker in ("<<<<<<<", ">>>>>>>", "=======")):
        fail(f"Merge marker left in {path.relative_to(ROOT)}")

# Resource index across all source sets.
resources: dict[str, set[str]] = {}
for res_root in [path for path in (APP / "src").glob("*/res") if path.is_dir()]:
    for directory in res_root.iterdir():
        if not directory.is_dir():
            continue
        kind = directory.name.split("-")[0]
        for path in directory.iterdir():
            if path.is_file():
                resources.setdefault(kind, set()).add(path.stem)

strings_path = RES / "values/strings.xml"
if strings_path.exists():
    try:
        tree = ET.parse(strings_path)
        resources["string"] = {
            el.attrib["name"]
            for el in tree.getroot()
            if el.tag == "string" and "name" in el.attrib
        }
    except Exception:
        pass

patterns = {
    "drawable": r"R\.drawable\.([A-Za-z0-9_]+)",
    "layout": r"R\.layout\.([A-Za-z0-9_]+)",
    "xml": r"R\.xml\.([A-Za-z0-9_]+)",
    "string": r"R\.string\.([A-Za-z0-9_]+)",
    "mipmap": r"R\.mipmap\.([A-Za-z0-9_]+)",
}
for path in kotlin_files:
    text = read(path)
    for kind, pattern in patterns.items():
        for name in re.findall(pattern, text):
            if name not in resources.get(kind, set()):
                fail(f"Missing R.{kind}.{name} referenced by {path.relative_to(ROOT)}")

manifest_path = APP / "src/main/AndroidManifest.xml"
manifest_text = read(manifest_path)
manifest_root = ET.parse(manifest_path).getroot()
android_ns = "{http://schemas.android.com/apk/res/android}"
all_kotlin = "\n".join(read(p) for p in kotlin_files)
for tag in ("activity", "receiver", "service", "provider"):
    for element in manifest_root.findall(f".//{tag}"):
        name = element.get(android_ns + "name")
        if not name:
            continue
        simple = name.rsplit(".", 1)[-1]
        if not re.search(rf"\b(class|object)\s+{re.escape(simple)}\b", all_kotlin):
            fail(f"Manifest {tag} class not found in Kotlin source: {name}")

for kind, name in re.findall(r"@([A-Za-z0-9_]+)/([A-Za-z0-9_]+)", manifest_text):
    if kind in resources and name not in resources[kind]:
        fail(f"Manifest references missing @{kind}/{name}")

# Build/release config.
gradle = read(APP / "build.gradle.kts")
for token, label in (
    ('versionName = "0.14.0"', "app versionName must be 0.14.0 before beta suffix"),
    ('versionCode = 1400', "app versionCode must be 1400"),
    ('targetSdk = 36', "targetSdk 36 expected"),
    ('compileSdk = 37', "compileSdk 37 expected"),
    ('create("beta")', "beta product flavor missing"),
    ('applicationIdSuffix = ".beta"', "beta package suffix missing"),
    ('versionNameSuffix = ".beta"', "beta version suffix missing"),
    ('GITHUB_BETA_UPDATES', "beta update build flag missing"),
    ('UPDATE_CHANNEL', "update-channel build field missing"),
    ('GIT_COMMIT', "Git commit build identity missing"),
):
    if token not in gradle:
        fail(label)

for token in (
    'file("libs/glyph-matrix-sdk-2.0.aar")',
    'add("betaImplementation", files(glyphMatrixSdk))',
):
    if token not in gradle:
        fail(f"Glyph beta dependency guard missing: {token}")

if (APP / "libs/glyph-matrix-sdk-2.0.aar").exists():
    fail("Proprietary Nothing Glyph Matrix AAR must not be committed or packaged in the source ZIP")

play = read(ROOT / ".github/workflows/play-release.yml")
for token in (
    ":app:assemblePlayRelease",
    ":app:bundlePlayRelease",
    "DAYLINE_KEYSTORE_BASE64",
    "DAYLINE_KEYSTORE_PASSWORD",
    "DAYLINE_KEY_ALIAS",
    "DAYLINE_KEY_PASSWORD",
):
    if token not in play:
        fail(f"Play workflow missing {token}")

# Permission separation.
beta_manifest_path = APP / "src/beta/AndroidManifest.xml"
beta_manifest_text = read(beta_manifest_path) if beta_manifest_path.exists() else ""
for permission in ("android.permission.INTERNET", "android.permission.REQUEST_INSTALL_PACKAGES"):
    if permission in manifest_text:
        fail(f"Main/Play manifest must not declare {permission}")
    if permission not in beta_manifest_text:
        fail(f"Beta updater manifest missing {permission}")
if 'android:allowBackup="false"' not in manifest_text:
    fail("Android automatic app backup should remain disabled")
if "POST_PROMOTED_NOTIFICATIONS" in manifest_text:
    fail("Promoted notification permission should not be declared")

for token in (
    "com.nothing.ketchum.permission.ENABLE",
    "com.pix.dayline.glyph.DaylineGlyphToyService",
    "com.nothing.glyph.TOY",
    "com.nothing.glyph.toy.aod_support",
    'android:value="1"',
):
    if token not in beta_manifest_text:
        fail(f"Beta Glyph Toy manifest missing {token}")
if "com.nothing.ketchum.permission.ENABLE" in manifest_text:
    fail("Main/Play manifest must not request Nothing Glyph permission")
if "NothingKey" in beta_manifest_text:
    warn("Legacy NothingKey metadata is present even though the Matrix kit does not document it")

# Notification presentation.
notification_sources = "\n".join(
    read(path) for path in (JAVA / "com/pix/dayline/notifications").glob("*.kt")
)
for forbidden in (
    "setUsesChronometer",
    "setChronometerCountDown",
    "setRequestPromotedOngoing",
    "HH:mm:ss",
):
    if forbidden in notification_sources:
        fail(f"Notification presentation regression: {forbidden} found")
for expected in ("setShowWhen(false)", "setProgress(progressMax, progressValue, false)", "ACTION_REFRESH"):
    if expected not in notification_sources:
        fail(f"Clean Now activity notification is missing {expected}")

# GitHub beta workflow and updater.
build_workflow = read(ROOT / ".github/workflows/build-apk.yml")
for token in (
    ":app:assembleBetaDebug",
    ":app:assemblePlayDebug",
    ":app:assembleBetaRelease",
    "DAYLINE_BETA_KEYSTORE_BASE64",
    "DAYLINE_BETA_KEY_ALIAS",
    "keytool -list",
    "apksigner",
    "--prerelease",
    "sha256sum",
):
    if token not in build_workflow:
        fail(f"GitHub beta workflow missing {token}")

for token in (
    "Verify Nothing SDK binary is not committed",
    "GlyphMatrix-Developer-Kit/main/glyph-matrix-sdk-2.0.aar",
    "app/libs/glyph-matrix-sdk-2.0.aar",
):
    if token not in build_workflow:
        fail(f"GitHub beta Glyph SDK workflow missing {token}")
if "glyph-matrix-sdk-2.0.aar" in play:
    fail("Play workflow must not fetch/package the Nothing Glyph Matrix SDK")

updater = read(APP / "src/beta/java/com/pix/dayline/updates/GithubBetaUpdater.kt")
for token in (
    "api.github.com/repos/0xpix/dayline/releases",
    "DaylineVersion.compare",
    "checksumUrl",
    "verifyApk",
    "canRequestPackageInstalls",
    "GitHub releases are not publicly reachable yet",
):
    if token not in updater:
        fail(f"GitHub updater missing {token}")

play_updater = read(APP / "src/play/java/com/pix/dayline/updates/GithubBetaUpdater.kt")
for forbidden in ("HttpURLConnection", "api.github.com", "REQUEST_INSTALL_PACKAGES"):
    if forbidden in play_updater:
        fail(f"Play updater stub unexpectedly contains {forbidden}")

# Node 24-native GitHub action majors used by this repository baseline.
all_workflows = "\n".join(read(path) for path in sorted((ROOT / ".github/workflows").glob("*.yml")))
for forbidden_action in (
    "actions/checkout@v4",
    "actions/setup-java@v4",
    "actions/upload-artifact@v4",
    "gradle/actions/setup-gradle@v4",
    "actions/configure-pages@v5",
    "actions/upload-pages-artifact@v3",
    "actions/deploy-pages@v4",
):
    if forbidden_action in all_workflows:
        fail(f"Deprecated/older action still referenced: {forbidden_action}")

# v0.12 depth features + v0.13 polish anchors.
feature_checks = {
    "per-calendar controls": "CalendarPreferences" in all_kotlin and "CalendarControlsSheet" in all_kotlin,
    "recurrence edit scope": "RecurrenceEditScope" in all_kotlin and "THIS_AND_FOLLOWING" in all_kotlin,
    "drag resize": "onResize" in all_kotlin and "shiftEnd" in all_kotlin and "detectDragGestures" in all_kotlin,
    "tap-to-create": "onCreateAt" in all_kotlin and "detectTapGestures" in all_kotlin,
    "focus modes": "FOCUS_50_10" in all_kotlin and "CUSTOM" in all_kotlin,
    "focus actions": "ACTION_SKIP_REST" in all_kotlin and "ACTION_PLUS_FIVE" in all_kotlin,
    "event progress": "compactRemaining" in all_kotlin and " LEFT" in all_kotlin,
    "today summary": "todaySummary" in all_kotlin,
    "conflict details": "ConflictSheet" in all_kotlin and "onConflict" in all_kotlin,
    "buffers": "bufferBeforeMinutes" in all_kotlin and "bufferAfterMinutes" in all_kotlin,
    "templates": "EventTemplate" in all_kotlin and "Save template" in all_kotlin,
    "spaces-calendar link": "calendarId" in read(JAVA / "com/pix/dayline/model/DaylineSpace.kt"),
    "task polish": "TaskPriority" in all_kotlin and "Convert to event" in all_kotlin,
    "undo": '"UNDO"' in all_kotlin,
    "search commands": "unfinished" in all_kotlin and "tomorrow" in all_kotlin and "focus" in all_kotlin,
    "backup/export": "exportState" in all_kotlin and "exportIcs" in all_kotlin,
    "per-widget config": "WidgetInstancePrefs" in all_kotlin and "WidgetConfigActivity" in all_kotlin,
    "activity widget states": "liveWidgetLabel" in all_kotlin,
    "haptics": "HapticFeedbackType.SegmentFrequentTick" in all_kotlin,
    "onboarding": "OnboardingScreen" in all_kotlin and "onboardingComplete" in all_kotlin,
    "update state": "UpdateStatus" in all_kotlin and "UpdateSheet" in all_kotlin,
    "automatic beta checks": "BetaUpdateScheduler" in all_kotlin and "24L * 60L * 60L" in all_kotlin,
    "build identity": "BuildConfig.GIT_COMMIT" in all_kotlin and "BuildConfig.VERSION_CODE" in all_kotlin,
    "calendar sync health": "saveCalendarSyncHealth" in all_kotlin and "Sync health" in all_kotlin,
    "return to now": "scrollToNow" in all_kotlin,
    "glyph preferences": "GlyphPreferences" in all_kotlin and "GlyphMode" in all_kotlin,
    "glyph 13x13 patterns": "GlyphMatrixPatterns" in all_kotlin and "const val SIZE = 13" in all_kotlin,
    "glyph eyes": "LOOK_LEFT" in all_kotlin and "LOOK_RIGHT" in all_kotlin and "BLINK" in all_kotlin,
    "glyph app states": "REMINDER_SOON" in all_kotlin and "CONFLICT" in all_kotlin and "DAY_OPEN" in all_kotlin,
    "glyph runtime queue": "GlyphRuntimeStore" in all_kotlin and "priority" in all_kotlin,
    "glyph state resolver": "GlyphStateResolver" in all_kotlin and "inQuietHours" in all_kotlin,
    "glyph hardware bridge": "NothingGlyphBridge" in all_kotlin and "setAppMatrixFrame" in all_kotlin,
    "glyph AOD toy": "DaylineGlyphToyService" in all_kotlin and "Always-on Glyph Toy" in all_kotlin,
    "glyph live preview": "GlyphMatrixPreview" in all_kotlin and "TEST GLYPH" in all_kotlin,
}
for label, ok in feature_checks.items():
    if not ok:
        fail(f"Feature anchor missing: {label}")

settings = read(JAVA / "com/pix/dayline/ui/settings/SettingsScreen.kt")
policy_url = "https://0xpix.github.io/dayline/privacy-policy.html"
if policy_url not in settings:
    fail("In-app privacy policy URL missing or changed")
if policy_url not in read(ROOT / "docs/play-store-checklist.md"):
    fail("Play checklist privacy policy URL mismatch")

for path in [*kotlin_files, *ROOT.glob("*.md"), *ROOT.glob("docs/*.md")]:
    text = read(path)
    if "FIXME" in text:
        fail(f"FIXME left in {path.relative_to(ROOT)}")
    if any(marker in text for marker in ("<<<<<<<", ">>>>>>>")):
        fail(f"Merge marker left in {path.relative_to(ROOT)}")

print("Dayline v0.14.0.beta release validation")
print(f"  Kotlin files: {len(kotlin_files)}")
print(f"  XML files: {len(list((APP / 'src').rglob('*.xml')))}")
print(f"  Errors: {len(ERRORS)}")
print(f"  Warnings: {len(WARNINGS)}")
for message in ERRORS:
    print(f"ERROR: {message}")
for message in WARNINGS:
    print(f"WARN: {message}")

if ERRORS:
    sys.exit(1)
print("PASS: static release validation completed successfully.")
print("NOTE: GitHub Actions remains the authoritative Android/Gradle compile test.")
