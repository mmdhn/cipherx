# CIPHERX

Personal encryption app (web UI wrapped as an Android APK with Capacitor).

- **Text**: AES-GCM-256, key from PBKDF2-SHA256 (600k rounds). Versioned format `CX2.` (header is authenticated).
- **Files**: `CXv2` chunked format (1 MiB AES-GCM chunks, encrypted file name, truncation-proof). Old `CXENC` files can still be decrypted.
- **Stego**: ciphertext hidden in PNG/JPG carrier (LSB). Old stego images still decode.
- **Classic** cipher is legacy and decrypt-only.

Build: run the *Build CIPHERX APK* workflow from the Actions tab.
