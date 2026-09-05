# Dayline — v0.8.5

Compile repair for the v0.8.4 dot-matrix widget update.

Fixed:
- `GlanceTheme.colors.outlineVariant` was not available in the current Glance API; the transparent 3×1 divider now uses `onSurfaceVariant`.
- `SystemPill` had its composable `content` parameter first, which made all trailing-lambda calls invalid Kotlin. `content` is now the final parameter.

No widget design was changed:
- Pulse 3×1 remains transparent.
- Its pills continue to use Android / Nothing Material You system containers.
- Dot-matrix widget typography remains enabled.
- Orbit 2×2 keeps the system-colored background.
