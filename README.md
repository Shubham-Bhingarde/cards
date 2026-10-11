# Cards — NFC card identifier (alpha)

A small, offline Android app that **reads and identifies** NFC cards you tap to
your phone, so you can find out *what technology* your office cards use. It is a
diagnostic tool, not a card emulator.

> **Why this exists.** Before you can know whether a phone could ever replace a
> physical access card, you have to know what the card actually is. This alpha
> answers that: it reports the card family (MIFARE Classic, DESFire, NFC-A/B/F/V,
> NDEF, …), its low-level identifiers, and whatever is openly readable — then
> stores it encrypted and lets you export a redacted diagnostics log.

## What it does (alpha scope)

- **Identify** a tapped card's technologies and card family.
- **Read** the safe, non-protected data: UID, ATQA/SAK, ATS/historical bytes,
  NDEF records, and card structure (size/sectors for MIFARE Classic).
- **Store** saved cards locally, with all details **encrypted at rest**
  (AES-256-GCM, key held in the Android Keystore).
- **Log & export** a diagnostics log you can share. Identifiers are **redacted
  by default**; a verbose toggle includes them if you choose.

## What it deliberately does NOT do (yet)

- It does **not** crack keys or dump protected MIFARE sectors.
- It does **not** emulate/clone cards. Whether emulation is even *possible* for a
  given card depends on what this alpha discovers — see the honest limits below.
- It has **no internet permission at all**. Card data cannot leave the device
  except through a share sheet you trigger yourself.

## Honest limitations (read this first)

| Card type | Can a stock Android phone read it here? |
|---|---|
| 13.56 MHz HF (MIFARE, DESFire, NFC-A/B/F/V, NDEF) | ✅ Yes — this app reads/identifies it. |
| 125 kHz LF (HID Prox, EM4100 — common for parking/building) | ❌ No. Phones have no 125 kHz radio. If a tap shows nothing, it's almost certainly this. |

Emulation (a later phase) is far more constrained: Android HCE can't change the
NFC UID without root, and encrypted cards can't be replicated without their keys.
We'll decide what's feasible based on what the scans show.

## Build

Requires Android Studio (Ladybug or newer) or a configured Android SDK.

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # minified release (configure signing first)
./gradlew test                    # JVM unit tests
```

The debug APK lands in `app/build/outputs/apk/debug/`.

- **Min SDK:** 24 (Android 7.0) · **Target SDK:** 35
- **Language:** Kotlin · **UI:** Jetpack Compose (Material 3)

## Security

See [SECURITY.md](SECURITY.md). Summary: no network permission, encrypted storage
via Keystore, redacted logs by default, backups disabled, release build minified
with R8, all components non-exported except the launcher activity.

## Status

`0.1.0-alpha` — functionality for identifying and logging cards. Not yet published
to Google Play.
