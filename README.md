# Dayline — v0.11.1

Compile repair for v0.11.0.

Fixed:
- `DayGlyph.kt`: moved `MaterialTheme.colorScheme.background` outside the
  Canvas draw lambda. Compose theme reads are composable calls and cannot be
  made from the non-composable DrawScope.
- `DayTimeline.kt`: removed the explicit
  `androidx.compose.foundation.layout.weight` import. `Modifier.weight()` is
  used from RowScope directly.

No v0.11.0 features were removed:
- Android Calendar sync
- optional per-event 25/5 focus cycle
- persistent FOCUS / REST Now activity
- current-time line on Today
- redesigned 24-hour Today ribbon
- live widget refresh
