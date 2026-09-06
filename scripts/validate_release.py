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
ERRORS: list[str] = []
WARNINGS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def warn(message: str) -> None:
    WARNINGS.append(message)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


# Required public/release files.
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
    ".github/workflows/build-apk.yml",
    ".github/workflows/play-release.yml",
]
for rel in required:
    if not (ROOT / rel).is_file():
        fail(f"Missing required release file: {rel}")

# XML.
for path in sorted((APP / "src/main").rglob("*.xml")):
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

# Kotlin source is at least parser-clean. A full Android compile is still the CI gate.
kotlin_files = sorted(JAVA.rglob("*.kt"))
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

# Build resource index.
resources: dict[str, set[str]] = {}
for directory in RES.iterdir():
    if not directory.is_dir():
        continue
    kind = directory.name.split("-")[0]
    for path in directory.iterdir():
        if path.is_file() and path.suffix != ".xml" or path.is_file():
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

# Kotlin R refs.
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

# Manifest components must be declared in source somewhere, not necessarily in a same-named file.
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

# Manifest resource refs.
for kind, name in re.findall(r"@([A-Za-z0-9_]+)/([A-Za-z0-9_]+)", manifest_text):
    if kind in resources and name not in resources[kind]:
        fail(f"Manifest references missing @{kind}/{name}")

# Release and Play configuration.
gradle = read(APP / "build.gradle.kts")
if 'versionName = "0.12.6"' not in gradle:
    fail("app versionName must be 0.12.6")
if 'versionCode = 36' not in gradle:
    fail("app versionCode must be 36")
if "targetSdk = 36" not in gradle:
    fail("targetSdk 36 expected")
if "compileSdk = 37" not in gradle:
    fail("compileSdk 37 expected")

play = read(ROOT / ".github/workflows/play-release.yml")
for required_token in (
    ":app:assembleRelease",
    ":app:bundleRelease",
    "DAYLINE_KEYSTORE_BASE64",
    "DAYLINE_KEYSTORE_PASSWORD",
    "DAYLINE_KEY_ALIAS",
    "DAYLINE_KEY_PASSWORD",
):
    if required_token not in play:
        fail(f"Play workflow missing {required_token}")

# Privacy / permission posture for the no-cloud build.
if "android.permission.INTERNET" in manifest_text:
    fail("Unexpected INTERNET permission")
if 'android:allowBackup="false"' not in manifest_text:
    fail("Android automatic app backup should remain disabled")
if "POST_PROMOTED_NOTIFICATIONS" in manifest_text:
    fail("Promoted notification permission should not be declared in the clean notification build")

# Notification presentation must not regress to the seconds chronometer/live chip.
notification_sources = "\n".join(
    read(path) for path in (JAVA / "com/pix/dayline/notifications").glob("*.kt")
)
for forbidden in ("setUsesChronometer", "setChronometerCountDown", "setRequestPromotedOngoing"):
    if forbidden in notification_sources:
        fail(f"Notification presentation regression: {forbidden} found")
for expected in ("setShowWhen(false)", "setProgress(progressMax, progressValue, false)", "ACTION_REFRESH"):
    if expected not in notification_sources:
        fail(f"Clean Now activity notification is missing {expected}")

# Feature anchors for the 0.12 Play beta.
feature_checks = {
    "per-calendar controls": "CalendarPreferences" in all_kotlin and "CalendarControlsSheet" in all_kotlin,
    "recurrence edit scope": "RecurrenceEditScope" in all_kotlin and "THIS_AND_FOLLOWING" in all_kotlin,
    "drag resize": "onResize" in all_kotlin and "shiftEnd" in all_kotlin,
    "tap-to-create": "onCreateAt" in all_kotlin and "detectTapGestures" in all_kotlin,
    "focus modes": "FOCUS_50_10" in all_kotlin and "CUSTOM" in all_kotlin,
    "focus actions": "ACTION_SKIP_REST" in all_kotlin and "ACTION_PLUS_FIVE" in all_kotlin,
    "event progress": "m left" in all_kotlin or "M LEFT" in all_kotlin,
    "today summary": "todaySummary" in all_kotlin,
    "conflict detection": "overlaps" in all_kotlin and 'Text("!"' in all_kotlin,
    "buffers": "bufferBeforeMinutes" in all_kotlin and "bufferAfterMinutes" in all_kotlin,
    "templates": "EventTemplate" in all_kotlin and "Save template" in all_kotlin,
    "spaces-calendar link": "calendarId" in read(JAVA / "com/pix/dayline/model/DaylineSpace.kt"),
    "task polish": "TaskPriority" in all_kotlin and "Convert to event" in all_kotlin,
    "undo": 'actionLabel = "UNDO"' in all_kotlin or '"UNDO"' in all_kotlin,
    "search": "SearchScreen" in all_kotlin,
    "backup/export": "exportState" in all_kotlin and "exportIcs" in all_kotlin,
    "per-widget config": "WidgetInstancePrefs" in all_kotlin and "WidgetConfigActivity" in all_kotlin,
    "activity widget states": "liveWidgetLabel" in all_kotlin,
    "haptics": "HapticFeedbackType" in all_kotlin,
    "onboarding": "OnboardingScreen" in all_kotlin and "onboardingComplete" in all_kotlin,
}
for label, ok in feature_checks.items():
    if not ok:
        fail(f"Feature anchor missing: {label}")







# v0.12.6 compile-regression checks.
renderer_source = read(
    JAVA / "com/pix/dayline/widgets/DotMatrixRenderer.kt"
)
store_source = read(
    JAVA / "com/pix/dayline/data/DaylineStore.kt"
)

if "repeatDays" not in store_source:
    fail("Custom repeat-day persistence is missing")

# v0.12.5 Unicode / recurrence / widget layout checks.
renderer_source = read(
    JAVA / "com/pix/dayline/widgets/DotMatrixRenderer.kt"
)
widgets_source = read(
    JAVA / "com/pix/dayline/widgets/DaylineWidgets.kt"
)
item_source = read(
    JAVA / "com/pix/dayline/model/DaylineItem.kt"
)
quick_source = read(
    JAVA / "com/pix/dayline/ui/components/QuickAddSheet.kt"
)
calendar_source = read(
    JAVA / "com/pix/dayline/data/AndroidCalendarSync.kt"
)
transfer_source = read(
    JAVA / "com/pix/dayline/data/DaylineTransfer.kt"
)

if "fun canRender(rawText: String)" not in renderer_source:
    fail("DotMatrix Unicode safety check is missing")

if "Native Glance Text preserves" not in widgets_source:
    fail("Unicode-safe widget title fallback is missing")

if "widget_event_surface" not in widgets_source:
    fail("Widget event-name contrast surface is missing")

if "compactTitleUnicode" not in widgets_source:
    fail("Unicode-safe title truncation is missing")

for token in (
    "Recurrence.WEEKENDS",
    "Recurrence.CUSTOM"
):
    if token not in item_source:
        fail(f"Occurrence logic missing {token}")
    if token not in quick_source:
        fail(f"Quick Add recurrence option missing {token}")
    if token not in calendar_source:
        fail(f"Calendar RRULE missing {token}")
    if token not in transfer_source:
        fail(f"ICS recurrence support missing {token}")

if "RepeatDaysDialog(" not in quick_source:
    fail("Seven-day repeat chooser dialog is missing")

for label in (
    '"Once"',
    '"Daily"',
    '"Weekdays"',
    '"Weekend"',
    '"Choose days"'
):
    if label not in quick_source:
        fail(f"Visible repeat option missing: {label}")

if '"Sunday only"' in quick_source:
    fail("Old Sunday-only repeat option is still visible")

if '"Every day except Sunday"' in quick_source:
    fail("Old except-Sunday repeat option is still visible")

if "SectionHeader(" not in quick_source:
    fail("Quick Add section spacing hierarchy is missing")

# v0.12.4 settings/sync/widget/upcoming checks.
settings_source = read(
    JAVA / "com/pix/dayline/ui/settings/SettingsScreen.kt"
)
widget_config_source = read(
    JAVA / "com/pix/dayline/widgets/WidgetConfigActivity.kt"
)
widgets_source = read(
    JAVA / "com/pix/dayline/widgets/DaylineWidgets.kt"
)
calendar_source = read(
    JAVA / "com/pix/dayline/data/AndroidCalendarSync.kt"
)
quick_source = read(
    JAVA / "com/pix/dayline/ui/components/QuickAddSheet.kt"
)
upcoming_source = read(
    JAVA / "com/pix/dayline/ui/upcoming/UpcomingScreen.kt"
)

if 'SettingsGroup("Widgets")' in settings_source:
    fail("Widget settings still appear in app Settings")

if "EmojiPickerView" not in widget_config_source:
    fail("Complete AndroidX emoji picker is missing")

if "WidgetSelectionSheet" not in widget_config_source:
    fail("Widget selector popups are missing")

if "autoSlideLongTitles" not in widget_config_source:
    fail("Long-title sliding is not per-widget")

if "widget_system_surface" not in widgets_source:
    fail("System-neutral widget surface is missing")

if "reconcileDeletedMappedItems" not in calendar_source:
    fail("Calendar deletion reconciliation is missing")

for font_token in (
    "FontChoice.INTER",
    "FontChoice.SPACE_GROTESK",
    "FontChoice.IBM_PLEX_MONO"
):
    if font_token not in settings_source:
        fail(f"New font option is missing: {font_token}")

if "FlowRow" not in quick_source:
    fail("Quick Add still uses cramped fixed rows")

for template in (
    "Meeting",
    "Focus block",
    "Workout",
    "Appointment",
    "Errand"
):
    if template not in read(
        JAVA / "com/pix/dayline/data/DaylineStore.kt"
    ):
        fail(f"Common template missing: {template}")

for filter_token in (
    "Meetings",
    "Holidays",
    "UpcomingFilter.Calendar",
    "UpcomingFilter.Space"
):
    if filter_token not in upcoming_source:
        fail(f"Upcoming filter missing: {filter_token}")

gradle_text = read(ROOT / "app/build.gradle.kts")
if "androidx.emoji2:emoji2-emojipicker:1.6.0" not in gradle_text:
    fail("AndroidX Emoji Picker dependency is missing")

# v0.12.3 interaction/polish checks.
timeline_source = read(
    JAVA / "com/pix/dayline/ui/components/DayTimeline.kt"
)
app_source = read(
    JAVA / "com/pix/dayline/ui/DaylineApp.kt"
)
widget_config_source = read(
    JAVA / "com/pix/dayline/widgets/WidgetConfigActivity.kt"
)
now_source = read(
    JAVA / "com/pix/dayline/notifications/NowActivityScheduler.kt"
)
floating_source = read(
    JAVA / "com/pix/dayline/ui/components/FloatingControls.kt"
)

for token in (
    "var gestureOffsetPx = 0f",
    "var gestureStep = 0",
    "commitStep * 15"
):
    if token not in timeline_source:
        fail(f"Drag commit repair is missing {token}")

if "duration = SnackbarDuration.Short" not in app_source:
    fail("Undo snackbar is not configured to auto-dismiss")

if "WidgetPreview(" not in widget_config_source:
    fail("Widget settings live preview is missing")

if ".setSubText(eventRange)" not in now_source:
    fail("Now notification START → END range is missing")

if 'append("END ")' not in now_source:
    fail("Now notification does not lead with event end time")

if "MenuLinesIcon" not in floating_source:
    fail("Refined floating menu icon is missing")

# Compose Dp sanity: Kotlin/Compose provides .dp for Int/Float/Double, not Long.
# Catch the exact regression that broke v0.12.1.
for kt in kotlin_files:
    text = read(kt)
    suspicious_long_dp = re.findall(
        r'(?:\d+L|toMinutes\(\)|coerceIn\(\s*\d+L\s*,\s*\d+L\s*\))\.dp\b',
        text
    )
    if suspicious_long_dp:
        fail(
            f"{kt.relative_to(ROOT)} contains a likely Long.dp expression: "
            f"{suspicious_long_dp}"
        )

# v0.12.1 widget/Noto Emoji checks.
widget_emoji = read(JAVA / "com/pix/dayline/data/WidgetEmoji.kt")
widgets_source = read(JAVA / "com/pix/dayline/widgets/DaylineWidgets.kt")
settings_source = read(JAVA / "com/pix/dayline/ui/settings/SettingsScreen.kt")
noto_renderer = JAVA / "com/pix/dayline/widgets/NotoEmojiRenderer.kt"

if not (RES / "font/noto_emoji.xml").is_file():
    fail("Missing downloadable @font/noto_emoji resource")
else:
    noto_font_xml = read(RES / "font/noto_emoji.xml")
    for token in ("com.google.android.gms.fonts", "Noto Emoji", "com_google_android_gms_fonts_certs"):
        if token not in noto_font_xml:
            fail(f"Noto Emoji downloadable font is missing {token}")

if not noto_renderer.is_file():
    fail("Missing NotoEmojiRenderer.kt")
else:
    renderer_text = read(noto_renderer)
    if "ResourcesCompat.getFont(context, R.font.noto_emoji)" not in renderer_text:
        fail("NotoEmojiRenderer is not loading @font/noto_emoji")

if "iconRes" in widget_emoji or "R.drawable.emoji_" in all_kotlin:
    fail("Legacy PNG widget emoji references remain")

legacy_emoji_pngs = list((RES / "drawable-nodpi").glob("emoji_*.png")) if (RES / "drawable-nodpi").exists() else []
if legacy_emoji_pngs:
    fail(f"Legacy widget emoji PNGs remain: {[p.name for p in legacy_emoji_pngs]}")

if '"DAYLINE"' in widgets_source:
    fail("Pulse widget still renders DAYLINE under the emoji")

if "ColorProvider(R.color.widget_system_surface)" not in widgets_source:
    fail("System-neutral widget background is missing")

widget_config_for_noto = read(
    JAVA / "com/pix/dayline/widgets/WidgetConfigActivity.kt"
)
if "EmojiPickerView" not in widget_config_for_noto:
    fail("Widget emoji picker is not using AndroidX EmojiPickerView")
if "NotoEmojiRenderer.render" not in widget_config_for_noto:
    fail("Widget preview is not rendering the selected emoji through Noto Emoji")

if 'android:name="preloaded_fonts"' not in manifest_text:
    fail("Manifest does not preload downloadable fonts")

# Public policy URL should match GitHub Pages workflow/listing.
settings = read(JAVA / "com/pix/dayline/ui/settings/SettingsScreen.kt")
policy_url = "https://0xpix.github.io/dayline/privacy-policy.html"
if policy_url not in settings:
    fail("In-app privacy policy URL missing or changed")
if policy_url not in read(ROOT / "docs/play-store-checklist.md"):
    fail("Play checklist privacy policy URL mismatch")

# No obvious release blockers left in code/docs.
for path in [*kotlin_files, *ROOT.glob("*.md"), *ROOT.glob("docs/*.md")]:
    text = read(path)
    if "FIXME" in text:
        fail(f"FIXME left in {path.relative_to(ROOT)}")

print("Dayline v0.12.6 release validation")
print(f"  Kotlin files: {len(kotlin_files)}")
print(f"  XML files: {len(list((APP / 'src/main').rglob('*.xml')))}")
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
