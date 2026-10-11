# Security notes

This app handles data that identifies physical access cards, so it is built to
minimise what is collected, stored, and exposed. The controls below map to OWASP
MASVS / Mobile Top 10 and OWASP secure-coding guidance.

## Data handling

- **No network.** The app declares **no `INTERNET` permission**. It cannot make
  any outbound connection, so card data cannot be exfiltrated by the app or by a
  compromised dependency. Data leaves the device only via a user-initiated share
  sheet (the diagnostics log).
- **Encrypted at rest.** Saved card details are serialised to JSON and encrypted
  with **AES-256-GCM** before being written to the database. The key is generated
  in and never leaves the **Android Keystore** (hardware-backed where available).
  Each record uses a fresh random IV. Only non-identifying metadata (card family,
  tech summary, timestamp) is stored in cleartext for list rendering.
- **Redacted logs by default.** The diagnostics logger redacts sensitive values
  (UID, NDEF payloads) unless the user explicitly enables verbose logging. Logs
  are size-capped, rotated, and stored in app-private storage.
- **No backups.** `allowBackup=false`, plus explicit data-extraction rules that
  exclude all domains, so card data and the encrypted DB are not copied off-device
  by cloud backup or device-to-device transfer.

## Attack surface

- **Foreground-only NFC.** NFC uses Reader Mode, active only while the app is in
  the foreground. The app never registers background tag intent filters, so it
  can't silently intercept cards.
- **Non-exported components.** Only the launcher activity is exported. The
  `FileProvider` is not exported; it grants temporary, per-URI read access to a
  single export file.
- **Read-only by design.** The app reads cards; it never writes to them, never
  authenticates to protected memory, and never attempts key recovery.

## Build hardening

- Release build: **R8 full-mode** minification + resource shrinking (smaller,
  obfuscated binary); `android.util.Log` calls stripped.
- `dependenciesInfo` excluded from APK/bundle.
- No secrets, keys, or credentials in source. Signing material is git-ignored.

## Reporting

This is a personal alpha. If you find an issue, open a GitHub issue describing it
(please do not include real card UIDs or payloads).
