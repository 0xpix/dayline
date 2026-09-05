# Dayline — v0.7.1

Widget compile repair for the v0.7 system-theme/widget redesign.

Fixed:
- uses `androidx.glance.unit.ColorProvider`, the ColorProvider interface used by Glance theme colors
- uses the core `androidx.glance.background` extension for ColorProvider backgrounds
- removes incorrect `androidx.glance.appwidget.background` usage, which only accepts explicit day/night Compose Colors
- removes the invalid `defaultWeight` import/usages from this widget implementation

Keeps:
- Material You system theme
- Pulse 2×1
- Day Map 2×3
- Day Board 3×3
