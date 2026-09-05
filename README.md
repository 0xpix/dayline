# Dayline — v0.8.6

Widget readability + widget typography settings.

## Fixed light/dark readability
The dot-matrix text bitmap is now rendered as a neutral white mask and tinted by Glance with the actual Material You `ColorProvider`.

That means the same widget text automatically receives:
- dark text on light system surfaces
- light text on dark system surfaces
- matching text colors inside primary/secondary system pills

This fixes the stale white-on-light problem visible after switching Nothing OS between light and dark mode.

## Widget font setting
Settings → Typography now includes a separate **Widget typography** section:

- **Nothing dots · Bold** — new default; thicker square-ish dots for home-screen readability.
- **Nothing dots · Fine** — the thinner style used previously.
- **Monospace · Bold** — native Glance monospace text for maximum legibility.

Changing the widget font refreshes all Dayline widgets immediately.

## Widget colors
The 3×1 remains transparent.
Its date/status/event/`YOUR DAY IS CLEAR` pills continue to use the Android / Nothing Material You system container colors.
The 2×2 continues to use the system widget background.
