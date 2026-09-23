# Pulse Music Agent Guidelines

## Release & Build Signatures
- **CRITICAL CONSTRAINT**: Pulse Music requires the static `release.keystore` (located in the repository root directory) for all release builds.
- **Why**: This permanently locks in the app's cryptographic signature across GitHub Actions CI runs, ensuring the in-app Auto-Updater works seamlessly without Android "Signature match failed" conflicts. 
- **Action**: Never modify the `release` signing configuration in `app/build.gradle.kts` to use random or auto-generated keys. Always preserve the static `release.keystore` dependency.
