# Dayline — v0.8.4

Dot-matrix widget typography + system pills.

## Typography
Glance app widgets do not support app-bundled custom fonts. To get much closer to the Nothing dot-matrix reference without shipping a font file, Dayline now renders widget labels with its own 5×7 dot-matrix bitmap renderer.

This applies to the visible typography in both widgets:
- weekday / date / month
- DAYLINE label
- next event
- `YOUR DAY IS CLEAR`
- AM / PM
- 2×2 date, free-hours badge and agenda text

## Pulse · 3×1
The widget body remains transparent.

The rounded pills now use the Android / Nothing Material You system containers rather than the old Frost fill. This includes `YOUR DAY IS CLEAR`, event pills, date pills, and status pills.

## Orbit · 2×2
Keeps the Android / Nothing system background and now uses the same custom dot-matrix typography for a consistent widget family.
