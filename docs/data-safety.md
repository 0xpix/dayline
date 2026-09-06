# Google Play Data Safety Notes

Use this as a release checklist, not as a substitute for reviewing the final Play Console form.

Current v0.12.0 architecture:
- no Dayline account
- no Dayline backend
- no analytics SDK
- no advertising SDK
- local SharedPreferences/JSON state
- optional Android Calendar Provider read/write
- explicit user-selected JSON backup/restore and ICS import/export
- Android automatic app-data backup disabled
- local notifications and alarms

Before every Play submission, audit the final dependency tree and behavior again. If a future dependency transmits data off-device, update both the Play Data safety declaration and privacy policy before publishing.
