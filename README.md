# Dayline — v0.8.2

Widget visual correction.

The previous widgets inherited Material You colors, which is why a brown/orange wallpaper produced brown widgets. That was technically system-matched, but it did not match the approved Dayline concept.

v0.8.2 deliberately separates the widget visual identity from the app's System theme.

## Frost widget palette
- cool blue-gray → lavender gradient background
- white / pale-gray typography
- near-white rounded event/date pills
- blue-gray text inside the strong pills
- monospaced widget typography
- custom Dayline orbit mark instead of the clock-like unicode symbol

## Pulse · 3×1
The compact widget now follows the approved composition closely:
- orbit mark + DAYLINE
- vertical separator
- weekday / date / month pills
- useful status pill (`NOW`, `24M`, `2H`, `OPEN`, etc.)
- highlighted next-event capsule
- stacked `NEXT / UP`
- AM → PM dot track with a ring for the current segment

## Other widgets
Orbit 2×2 and Board 3×2 now use the same Frost visual system for consistency.

The Android app's **System** appearance still uses Material You. Only the Dayline widgets use the dedicated Frost palette.
