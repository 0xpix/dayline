# Dayline — v0.10.0

Three interaction/system features.

## Drag to reschedule
On Today, long-press a timed event/task and drag vertically.

- 15-minute snapping
- duration is preserved
- start/end times preview while dragging
- release saves immediately
- recurring items move the whole series with the current data model

## Now activity
Timed items with a valid start and end time can show a persistent notification while active.

- countdown chronometer to the event end
- auto-clears at the end
- survives rescheduling and reboot
- requests Android 16 promoted ongoing / Live Update treatment where supported
- toggle in Settings → Reminders → Now activity

## Lock-screen widget
Adds **Dayline · Lock 2×1** with keyguard widget metadata.

It shows:
- selected monochrome widget emoji
- day/date
- `NOW` for an active timed event
- otherwise the next item and its time

Whether it appears in the lock-screen picker depends on the device/OEM widget host.
