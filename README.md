# Telegram Chat Exporter (Android)

A minimalistic Android app that signs in to your Telegram account, lets you multi-select messages from any chat or channel, and exports them as a Markdown file with **per-message timestamps** preserved — so you can paste them into ChatGPT / Claude / Gemini for analysis without losing the time context.

The default Telegram client's "copy" loses timestamps. This app fixes that.

## Features

- Native user-account access via TDLib (works on channels you're subscribed to, including read-only ones).
- Long-press to start multi-select; tap to add/remove messages.
- Markdown export with `## [yyyy-MM-dd HH:mm:ss] Sender` headers, replies as blockquotes, forwards italicised.
- Media-only messages are skipped automatically (text-only output, ideal for AI analysis).
- Save to any folder via Android's Storage Access Framework.
- Material 3 design with dynamic color on Android 12+.

## Build setup (one-time)

The app and its CI use no manual build steps once the following secrets are configured.

### 1. Create a Telegram application

Go to <https://my.telegram.org/apps> and create an application. You'll get an `api_id` (integer) and `api_hash` (string).

### 2. Add GitHub repository secrets

Settings → Secrets and variables → Actions → New repository secret:

| Secret name        | Value |
|--------------------|-------|
| `API_ID`           | Your Telegram api_id (integer) |
| `API_HASH`         | Your Telegram api_hash (string) |
| `KEYSTORE_BASE64`  | Output of `base64 -w0 release.jks` (only required for signed release builds) |
| `KEYSTORE_PASSWORD`| Keystore password |
| `KEY_ALIAS`        | Key alias inside the keystore |
| `KEY_PASSWORD`     | Key password |

Generate a release keystore with:
```
keytool -genkeypair -v -keystore release.jks -alias chat-exporter -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks > release.jks.b64
```

### 3. Build TDLib once

TDLib is a heavy native dependency; we build it once and reuse the AAR.

1. Go to **Actions → Build TDLib AAR → Run workflow** (defaults to TDLib `master`).
2. Wait ~45 minutes. A GitHub Release tagged `tdlib-master-<sha>` is published with `tdlib.aar` attached.
3. In `.github/workflows/android-build.yml`, replace `TDLIB_RELEASE_TAG: tdlib-master-PLACEHOLDER` with the new tag.
4. Commit and push.

### 4. Pushes now build the app automatically

- **Any push or PR**: a debug APK is built and uploaded as the workflow artifact `app-debug-apks` (download from the Actions run page).
- **Push a tag like `v0.1.0`**: a signed release APK is built and attached to the GitHub Release for that tag.

## Install

1. Download the per-ABI APK that matches your phone (most modern phones are `arm64-v8a`).
2. `adb install app-arm64-v8a-debug.apk` — or transfer to the phone and tap to install (allow installs from unknown sources).
3. Open the app, enter your Telegram phone number, then the SMS/Telegram code, then your 2FA password if you have one.

## Usage

1. Open a chat or channel.
2. Long-press any message to start selection mode.
3. Tap more messages to add them.
4. Tap the share icon in the top bar — opens an export preview.
5. Tap **Save .md**, choose a folder, done. Open the file or paste its contents into your AI assistant.

## Local development

You need JDK 17, Android SDK 34, and the GitHub CLI (for fetching the TDLib AAR).

```
scripts/fetch-tdlib.sh                # downloads app/libs/tdlib.aar from the pinned release
./gradlew :app:assembleDebug -PAPI_ID=12345 -PAPI_HASH=abcdef...
```

`app/libs/tdlib.aar` is gitignored; CI fetches it on every build.

## Project layout

```
app/                                     Android app module (Kotlin + Compose)
  src/main/java/com/example/tgexporter/
    telegram/    TDLib client wrapper, repositories, auth state
    ui/          Compose screens: auth, chats, messages, export
    export/      Markdown formatter + Storage Access Framework saver
    di/          AppContainer (manual DI)
.github/workflows/
  build-tdlib.yml      one-shot TDLib build → publishes tdlib.aar to a Release
  android-build.yml    push: debug APK artifact; tag v*: signed release APK
scripts/fetch-tdlib.sh   helper to pull the AAR for local builds
```

## Security notes

- `api_id` / `api_hash` are baked into `BuildConfig` from CI secrets at build time. They aren't perfect secrets per Telegram's docs, but they identify your account; **don't share signed APKs publicly without rotating the credentials**.
- The TDLib database lives in `app filesDir/tdlib`. Logging out wipes it.
- This app does **not** send anything anywhere — exports are written to your device only.

## Known limitations (v0)

- Forum chats with topic threads are flat in the message list.
- Media files aren't saved alongside the .md (media-only messages are skipped per design).
- No background fetching: chats and history load on demand.
