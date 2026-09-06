# Google Play Data Safety Notes

Use this as a release checklist, not as a substitute for reviewing the final Play Console form.

Current v0.13.0 beta/play architecture:

- no Dayline account
- no Dayline backend
- no analytics SDK
- no advertising SDK
- no Play-flavor INTERNET permission
- no Play-flavor REQUEST_INSTALL_PACKAGES permission
- optional Android Calendar Provider access when the user enables calendar sync
- explicit user-selected JSON backup/restore and ICS import/export
- Android automatic app-data backup disabled
- local notifications and alarms
- GitHub beta builds may connect to the public GitHub Releases API for update checks and APK downloads
- the Play flavor hides the GitHub updater and Google Play owns stable app updates

The beta update request contains ordinary network metadata such as an IP address and User-Agent processed by GitHub as the network service provider. Dayline does not include calendar, task, focus, widget or backup contents in the update request.

The separate GitHub beta flavor uses package `com.pix.dayline.beta`. It can request Android's per-app “Install unknown apps” permission only when the user chooses to install a downloaded beta. Installation is still performed and verified by Android; Dayline never silently installs an APK.

Before every Play submission, audit the final merged Play manifest, dependency tree and runtime behavior again. If a future dependency transmits user data off-device, update both the Play Data safety declaration and privacy policy before publishing.
