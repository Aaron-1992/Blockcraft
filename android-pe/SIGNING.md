# Blockcraft PE release signing

The PE release workflow **fails closed** unless it receives a persistent signing keystore and verifies the APK certificate fingerprint. Never publish an APK produced with the local disposable fallback as an update.

## GitHub Actions configuration

Add these repository **Actions secrets**:

- `BLOCKCRAFT_PE_KEYSTORE_B64` — base64-encoded Java keystore (`.jks` or `.keystore`)
- `BLOCKCRAFT_PE_STORE_PASSWORD` — keystore password
- `BLOCKCRAFT_PE_KEY_ALIAS` — signing key alias inside the keystore
- `BLOCKCRAFT_PE_KEY_PASSWORD` — key password

Add this repository **Actions variable**:

- `BLOCKCRAFT_PE_EXPECTED_CERT_SHA256` — the signing certificate's SHA-256 fingerprint (64 hex digits; colons are accepted)

Generate the base64 secret without adding a newline, for example on Linux:

```sh
base64 -w 0 blockcraft-pe-release.jks
```

Do not commit the keystore, its passwords, or their base64 encoding. The workflow materializes the keystore with owner-only file permissions, passes passwords to `apksigner` via environment-variable references, and removes its temporary build directory after signing.

To obtain the fingerprint for a new persistent key, sign a local build with the four secrets configured and run `apksigner verify --print-certs <apk>`. The builder also prints this certificate report. Set the reported SHA-256 digest as `BLOCKCRAFT_PE_EXPECTED_CERT_SHA256` before enabling a release.

The builder verifies the signed APK and checks the certificate fingerprint against `BLOCKCRAFT_PE_EXPECTED_CERT_SHA256`; replacing the keystore without intentionally changing that variable makes the release fail.

## Signing history warning

Earlier PE release builds generated a temporary signing key on every run, and that previous key is not available in the repository. The new configuration makes future releases consistent with the first configured persistent key, **but cannot restore compatibility with APKs signed by the unavailable key**. In particular, a newly configured key must not be described as an in-place upgrade path from v1.11 unless the original certificate is recovered and verified. Do not publish a new PE release until the signing identity and the user migration plan are confirmed.
