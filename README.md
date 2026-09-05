# Dayline — v0.10.1

Live widget refresh + Today visual refinement.

## Today
The large 24-segment circle is replaced by a much smaller **Day signal**:
- 12 dots represent two-hour blocks
- scheduled blocks become solid
- the current block gets a small Material You ring
- one faint rail connects the day
- substantially less visual weight above the greeting

## Widget light-mode task contrast
Tasks now use the Material You secondary container with its matching
`onSecondaryContainer` foreground. Long task titles stay in the native Glance
path instead of the bitmap animation path so their light/dark contrast stays exact.

## Live widget changes
The Glance widget data is now read *inside* `provideContent`, rather than captured
before the composition starts.

Dayline also refreshes each placed Glance id directly through
`GlanceAppWidgetManager`.

As a result:
- changing the widget emoji refreshes placed widgets immediately
- adding/editing/deleting an event refreshes placed widgets immediately
- changing widget font refreshes immediately
- lock-screen widget refreshes with the same path

## Emoji picker
Choosing an icon no longer closes the emoji sheet.
The live preview remains visible while trying different icons.
Swipe the sheet down or use Back when finished.
