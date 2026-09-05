# Dayline — v0.9.1

Compile repair for v0.9.0.

The current Glance `ColorProvider` API in this project accepts either:
- a single Compose `Color`, or
- a color resource id.

v0.9.0 incorrectly used named `day` / `night` arguments for the free-standing Pulse text colors.

Fixed:
- `PulseFreeText = ColorProvider(Color(...))`
- `PulseFreeMuted = ColorProvider(Color(...))`

No visual features were removed:
- emoji-only Pulse identity remains
- compact bottom-sheet Settings remains
- Pulse 3×1 remains transparent
- free-standing SAT / AM / PM / NEXT / UP text remains bright
- Material You pill colors remain
- Orbit 2×2 remains system-themed
