# Dayline — v0.10.3

Live event refresh + neutral system widget surfaces.

## Live event updates
Event changes now use a Glance state refresh token.

For every placed widget instance Dayline now:
1. commits the updated event JSON first;
2. changes the widget's Glance Preferences state;
3. calls `update()` for that exact widget id.

Each widget reads the refresh token inside `provideContent`, so an already-running
Glance composition is forced to recompose and then reload the current events.

This applies to:
- adding events/tasks
- editing events/tasks
- deleting events/tasks
- drag rescheduling
- task completion
- emoji/font/widget-setting changes

## Widget background
Removed Material You accent containers from the widget UI.

- Orbit 2×2 uses the system Material 3 `background`
- pills use neutral `surfaceVariant`
- emoji circle uses neutral `surfaceVariant`
- event/task text uses `onSurface`
- day-track dots use neutral system foregrounds
- Pulse 3×1 body remains transparent

This avoids the blue Material You look while still following system light/dark
surface colors.
