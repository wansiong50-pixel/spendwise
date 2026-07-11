# How to Build a Release APK (instead of a Debug APK)

This guide explains what a release APK is, why it's different from the debug
APK Android Studio makes by default, and three ways to build one — from
easiest to most manual. It's written for SpendWise, but the ideas apply to
any Android project.

---

## 1. Debug vs Release — what's actually different?

When you press the green ▶ Run button in Android Studio, you get a **debug**
build. That's fine for development, but you should never ship it:

| | Debug APK | Release APK |
|---|---|---|
| **Signing key** | A throwaway key Android Studio auto-generates on your machine | **Your** release key (`spendwise-release.jks`) |
| **Code shrinking (R8)** | Off — full, slow code | On — smaller and faster (Compose especially benefits) |
| **Debuggable** | Yes — anyone can attach a debugger and inspect it | No |
| **Performance** | Slower | Optimized |
| **Can update an installed release app?** | ❌ No — different key | ✅ Yes |

The signing key is the important one. Android only allows an app to be
**updated** by an APK signed with the **same key** as the version already
installed. Debug and release use different keys, so:

- A release APK **cannot install over** a debug build (and vice versa).
  You must uninstall one first — which deletes that install's data.
- If you ever lose the release key, you can never update the app for
  anyone who installed it. They'd have to uninstall (losing their data)
  and reinstall.

> **⚠️ Back up your keystore.** For this project that means two things:
> the keystore file (`spendwise-release.jks`, stored **outside** the repo
> in `C:\Users\<you>\.android-keys\`) and the passwords in
> `keystore.properties` (in the project root, deliberately **not** committed
> to git). Put a copy of both somewhere safe — a password manager entry
> with the file attached is ideal.

---

## 2. How signing is wired up in this project

You don't need to type passwords into Android Studio every time. The build
script ([app/build.gradle.kts](../app/build.gradle.kts)) reads a file called
`keystore.properties` from the project root:

```properties
storeFile=C:/Users/<you>/.android-keys/spendwise-release.jks
storePassword=<the keystore password>
keyAlias=spendwise
keyPassword=<the key password>
```

If that file exists, **every release build is signed automatically**.
If it doesn't exist (e.g. someone else clones the repo), release builds
still compile — they just come out unsigned.

This file is listed in `.gitignore` on purpose. Never commit it.

---

## 3. Method A — Build Variants panel (easiest, recommended)

Because signing is already configured, you can build a release APK with two
clicks:

1. In Android Studio, open **Build → Select Build Variant…**
   (or the **Build Variants** tab in the bottom-left sidebar).
2. In the panel, find the `:app` module and change **Active Build Variant**
   from `debug` to **`release`**.
3. Go to **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Wait for the build (a minute or two — R8 shrinking takes time).
5. When the "APK(s) generated successfully" bubble appears, click
   **locate** — or find the file yourself at:

   ```
   app/build/outputs/apk/release/app-release.apk
   ```

That file is your signed, shrunk, installable release APK.

> **Tip:** switch the Build Variant back to `debug` afterwards, otherwise
> the ▶ Run button will keep building (slower) release versions.

---

## 4. Method B — the command line (what CI and scripts use)

You don't need Android Studio open at all. From the project folder in
PowerShell:

```powershell
.\gradlew.bat :app:assembleRelease
```

The APK lands in the same place:
`app\build\outputs\apk\release\app-release.apk`.

Useful variations:

```powershell
# Run the unit tests first, then build
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease

# Install the release build straight onto a connected device/emulator
.\gradlew.bat :app:installRelease
```

This is exactly what was used to build the APKs attached to the GitHub
releases.

---

## 5. Method C — the "Generate Signed App Bundle / APK" wizard

This is the classic way most tutorials teach. It works even in projects
with no signing config, because you enter the keystore details by hand:

1. **Build → Generate Signed App Bundle / APK…**
2. Choose **APK**, click **Next**.
3. Fill in the keystore form:
   - **Key store path:** browse to `C:\Users\<you>\.android-keys\spendwise-release.jks`
   - **Key store password:** the `storePassword` from `keystore.properties`
   - **Key alias:** `spendwise`
   - **Key password:** the `keyPassword` from `keystore.properties`
   - Optionally tick *Remember passwords*.
4. Click **Next**, choose the **release** variant, click **Create**.
5. The APK appears in `app/release/` (note: a *different* folder than
   Methods A and B use).

For this project Methods A/B are simpler because the form is already
answered by `keystore.properties` — but it's good to know the wizard
exists, and it's what you'd use on a machine without that file.

> **APK vs App Bundle (.aab):** the wizard also offers "App Bundle". That
> format is only for uploading to Google Play — it can't be installed
> directly on a phone. For GitHub releases and sideloading, always pick
> **APK**.

---

## 6. Check the APK before sharing it

Two quick sanity checks:

**Is it actually signed with the release key?**

```powershell
# Use your installed build-tools version
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

You should see `CN=SpendWise` in the certificate line. If you see an error
about the APK not being signed, `keystore.properties` wasn't found — check
it exists in the project root.

**Does it install and run?**

```powershell
# NOTE: uninstall any debug build first — different key, update will fail
adb uninstall com.spendwise.app
adb install app\build\outputs\apk\release\app-release.apk
```

(On your own phone: copy the APK over, tap it, and allow "install unknown
apps" when prompted.)

---

## 7. Releasing a new version (the full checklist)

When you want to publish, say, version 1.5:

1. **Bump the version** in [app/build.gradle.kts](../app/build.gradle.kts):
   ```kotlin
   versionCode = 10      // always +1 — Android uses this to detect updates
   versionName = "1.5"   // the human-readable version
   ```
2. **Update [CHANGELOG.md](../CHANGELOG.md)** — rename the `[Unreleased]`
   section to `[1.5] — <date>`.
3. **Test and build:**
   ```powershell
   .\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
   ```
4. **Commit, tag, push:**
   ```powershell
   git add -A
   git commit -m "Release 1.5"
   git tag v1.5
   git push
   git push origin v1.5
   ```
5. **Publish on GitHub** (rename the APK so each release's file is distinct):
   ```powershell
   Copy-Item app\build\outputs\apk\release\app-release.apk SpendWise-1.5.apk
   gh release create v1.5 SpendWise-1.5.apk --title "SpendWise 1.5" --notes "…what changed…"
   ```

---

## 8. Forked or cloned this repo? Make your own key

The release keystore is **not** in this repository (that's deliberate — it's
the app author's identity). If you fork SpendWise and want your own release
builds, you create your own key once:

```powershell
keytool -genkeypair -v `
  -keystore C:\somewhere-safe\my-release.jks `
  -alias myapp -keyalg RSA -keysize 2048 -validity 10000
```

(`keytool` ships with any JDK — Android Studio's is at
`C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`. It will ask
you to invent a password and answer a few name questions; the answers just
go into the certificate.)

Then create `keystore.properties` in the project root:

```properties
storeFile=C:/somewhere-safe/my-release.jks
storePassword=<the password you invented>
keyAlias=myapp
keyPassword=<the password you invented>
```

That's it — release builds now sign with *your* key. Two things to know:

- Your build **cannot install as an update over** the original SpendWise
  (different key — that's Android's anti-impersonation protection working).
  If you're distributing a real fork, also change `applicationId` in
  `app/build.gradle.kts` so the two apps can coexist on one phone.
- Guard your keystore like the original author guards theirs: keep it out
  of git, back it up, don't lose the password.

---

## 9. Common problems

| Symptom | Cause & fix |
|---|---|
| `App not installed` / `signatures do not match` on the phone | A build signed with a *different* key is already installed (usually a debug build). Uninstall it first. |
| Release APK builds but is unsigned | `keystore.properties` missing from the project root, or a typo in one of its four values. |
| `Keystore was tampered with, or password was incorrect` | Wrong `storePassword`. Check `keystore.properties` against your backup. |
| Build fails only for release, not debug | Almost always R8/ProGuard stripping something. Read the error — it names the missing class — and add a keep rule in `app/proguard-rules.pro`. |
| Forgot to bump `versionCode` | The new APK installs fresh but won't be offered as an *update* over the old one. Bump it and rebuild. |
