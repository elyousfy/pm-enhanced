# PM (Enhanced)

<p align="center">
<img src="docs/design/logo/logo.png" alt="PM (Enhanced) logo" width="160"/>
</p>

<p align="center">
<strong>Silences your phone at prayer times, on time, for the prayers and days you choose.</strong>
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="GPLv3 License"/></a>
</p>

PM (Enhanced) is a fork of [Prayer Mode](https://github.com/prayermode/prayermode) by Yahyaoui.
It keeps the original idea: turn on Do Not Disturb during each prayer and turn it off afterwards.
It replaces the interface with Jetpack Compose and rebuilds the scheduling and time sources so the times match the official ones.

Package id: `com.elyousfy.pmenhanced`. It installs next to the original app.

## Features

**Choose what is silenced**
- Turn each of the five prayers on or off.
- Pick the active weekdays. Days that are off are skipped.
- Per prayer: start silence at the Adhan, before it for ablution, or at the Iqama; set the length; shift the Adhan by ±30 minutes.
- Friday Jumuʿah at a fixed time or relative to Dhuhr. UAE locations default to the national fixed time of 12:45.
- Ramadan Taraweeh and Tahajjud windows, and Eid prayer.

**Accurate times**
- Pick a country and city by hand. No GPS, no location permission.
- Two time sources:
  - **Aladhan** (any country). UAE uses the Dubai method (16), which matches the official Awqaf timetable within one minute.
  - **UAE Awqaf** (official, UAE cities only). This needs your own API key, entered in the app. If Awqaf fails, the app falls back to Aladhan.
- Times are computed in the city's time zone, so a wrong device time zone does not shift them.
- Exact alarms switch DND directly at the minute, also in Doze. Alarms are restored after reboot, time change, time-zone change and app update.

**Design**
- Themes: follow the phone, Light, Dark (pure black, borderless) and Forest (the original green look).
- Accent colours, round black-and-white buttons.
- 12-hour or 24-hour clock, or follow the phone.
- Arabic with right-to-left layout, plus the upstream translations.
- Quick Settings tile.

## Permissions

| Permission | Why |
| :--- | :--- |
| Do Not Disturb access | Turn silence on and off at prayer times. |
| Alarms & reminders | Exact alarms, so silence starts on the minute. |
| Notifications (optional) | Tell you when silence starts and ends. |
| Ignore battery optimisation (optional) | Keep the daily reschedule reliable on aggressive devices. |
| Internet | Download prayer times from Aladhan or Awqaf. |

The app has no location permission, no ads and no analytics.

## Building

Requirements: JDK 17 or newer and the Android SDK (compileSdk 37). minSdk is 24; `java.time` works there through core library desugaring.

```bash
git clone https://github.com/elyousfy/pm-enhanced.git
cd pm-enhanced
./gradlew :app:assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:lintDebug            # lint
```

Release signing is optional. To sign, create `keystore.properties` in the project root with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. The file is in `.gitignore`; never commit it.

No API keys are bundled in the source or the APK. Each user enters their own Awqaf key in the app, which keeps it in the app's settings.

## Credits

- [Prayer Mode](https://github.com/prayermode/prayermode) by Yahyaoui, the upstream project.
- Prayer times by the [Aladhan API](https://aladhan.com/prayer-times-api) and [UAE Awqaf](https://www.awqaf.gov.ae/).

## License

GNU General Public License v3.0, the same as upstream. See [LICENSE](LICENSE).
