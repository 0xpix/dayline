# Dayline — v0.10.2

Compile repair for v0.10.1.

Fixed:
- `DayGlyph.kt` now imports `androidx.compose.foundation.layout.size`.
- This resolves the Kotlin compile error at the new minimal Day signal:
  `Unresolved reference 'size'`.

No features were changed or removed:
- minimal Today day signal
- live widget refresh
- emoji picker stays open while changing icons
- Material You task contrast fix
- drag-to-reschedule
- Now activity
- lock-screen widget
