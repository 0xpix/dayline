# Google Play Data Safety Notes

Use this as a release checklist, not as a substitute for reviewing the final Play Console form.

Current Play-flavor v0.12.1 architecture:
- no Dayline account
- no Dayline backend
- no analytics SDK
- no advertising SDK
- no Play-flavor INTERNET permission
- no Play-flavor REQUEST_INSTALL_PACKAGES permission
- local SharedPreferences/JSON state
- optional Android Calendar Provider read/write
- explicit user-selected JSON backup/restore and ICS import/export
- Android automatic app-data backup disabled
- local notifications and alarms

Before every Play submission, audit the final dependency tree and behavior again. If a future dependency transmits data off-device, update both the Play Data safety declaration and privacy policy before publishing.

The separate GitHub beta flavor is a different package (`com.pix.dayline.beta`). It can contact GitHub Releases for beta update metadata/downloads and can request Android's per-app “Install unknown apps” permission. Those beta-only permissions are not merged into the Play flavor.
