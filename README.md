# Dayline — v0.11.0

Calendar integration, optional Pomodoro event cycles, live Now marker,
and a redesigned Today ribbon.

## Android Calendar sync
Settings → Calendar → Android Calendar sync.

When enabled:
- Dayline requests READ_CALENDAR + WRITE_CALENDAR.
- Events from visible Android calendars are overlaid in Today, Calendar,
  Upcoming and widgets.
- Existing/new Dayline events are published to the first visible writable
  calendar on the phone.
- Dayline-created events retain their Calendar Provider event id so edits
  update the same external event instead of creating duplicates.
- External calendar occurrences are shown read-only inside Dayline.
- Calendar Provider changes are observed while Dayline is running.

Turning sync off only hides the external overlay; it does not delete calendar
events from the phone.

## Optional Pomodoro 25 / 5
Timed events now have a Focus cycle option:

- Off
- 25 / 5

When 25 / 5 is enabled:
- 25 minutes focus
- 5 minutes rest
- repeats until the event's end time
- the persistent Now activity counts down to the current focus/rest boundary
  rather than only to the whole event end
- the notification automatically switches between FOCUS and REST

This is per event, so Gym / CS2 can stay Off while a research/deep-work block
uses 25 / 5.

## Current-time line
Today now gets a live current-time marker in the agenda:
- HH:mm label
- small Material You dot
- thin line
- updates every ~30 seconds

## Today ribbon
The previous dotted Day signal is replaced with a very quiet 24-hour ribbon:
- faint line = the day
- solid segments = actual scheduled event ranges
- small ring = the current point in the day

Every mark now represents real time rather than decoration.

## Calendar Provider implementation note
Recurring events are written with RRULE + DURATION, while non-recurring events
use DTEND. All-day events use UTC midnight boundaries as required by Android's
Calendar Provider.
