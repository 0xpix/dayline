# Dayline — v0.6.0

Dark-mode + launcher icon + widgets release.

## Fixed
- Dark mode now provides the correct foreground content color globally, so labels and settings remain readable.
- The launcher/adaptive/themed icon no longer depends on stroked arc paths. It uses solid geometry so Nothing Launcher should render the complete mark instead of only the center `!`-like part.
- “Show day glyph” is renamed to **24-hour day dial** and explained in Settings.

## Widgets
Three separate Glance widgets are included:
- **Dayline · Next 1×2** — date + next event/task.
- **Dayline · Today 2×2** — date + first three items today.
- **Dayline · Agenda 2×3** — taller Today list with up to six items.

Widgets open Dayline when tapped, refresh when Dayline data changes, and request a periodic refresh every 30 minutes.

Uses stable AndroidX Glance 1.2.0.
