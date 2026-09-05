# Dayline — v0.8.8

Compile repair for the widget-font + widget-cover update.

Fixed:
- restores the missing Glance `Text`, `TextStyle`, `FontWeight`, `FontFamily` imports
- restores the missing Compose `sp` import
- renames the `DotMatrixRenderer.render()` parameter from `style` to `fontChoice`
- explicitly uses `this.style = Paint.Style.FILL`, avoiding the Kotlin name collision with the widget font choice

No visual features were removed:
- Widget typography selector remains
- Widget cover selector remains
- Pulse 3×1 remains transparent
- Pulse pills remain Material You / Nothing system colors
- Orbit 2×2 remains system-themed
