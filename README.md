# Dayline — v0.9.3

Nothing-style emoji picker + long-title slide.

## Emoji picker
Settings → Widgets → Widget emoji now has:
- a live Pulse-style preview
- category strip with Faces / Fun / Other
- cleaner 4-column icon grid
- stronger selected-state outline
- monochrome transparent emoji icons throughout

## Long event titles
When **Slide long event titles** is enabled (default), a long highlighted event title in the widget is split into readable chunks and automatically slides from right to left.

The animation uses Android RemoteViews `ViewFlipper`, so it runs in the widget host instead of trying to continuously re-render the Glance widget.

- frame changes every ~2.8 seconds
- right-to-left slide transition
- up to four chunks
- short titles remain static
- applies to highlighted/next event pills in Pulse 3×1 and Orbit 2×2
