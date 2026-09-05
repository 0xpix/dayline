# Dayline — v0.7.0

System-theme + playful widgets update.

## System theme
When Appearance is set to **System**, Dayline now uses Android Material You dynamic colors derived from the phone wallpaper/system palette on Android 12+. Light and OLED dark remain explicit Dayline themes.

## Widgets
The widget set is now:

- **Pulse 2×1** — date bubble + 12-dot two-hour day map + next item.
- **Day Map 2×3** — full 24-hour dot matrix + up to three items.
- **Day Board 3×3** — full visual day map, free-hour badge and up to four items.

The widgets use GlanceTheme so their colors follow the Android system/widget dynamic palette instead of staying black/white.

Dot semantics:
- primary-colored dot = scheduled hour
- tertiary-colored dot = current hour
- muted dot = open hour

The widgets request hourly framework refreshes and are also refreshed when Dayline data changes.
