---
name: github-actions-ci-cd
description: >-
  Workflows, inspection guidelines, and best practices for GitHub Actions CI/CD pipelines,
  remote automated builds, release signing preservation, and GitHub REST API integration.
  Use when modifying CI workflows, debugging build failures, drafting releases, or managing
  remote build pipelines without compiling locally.
metadata:
  model: inherit
---

# GitHub Actions CI/CD & Remote Build Validation

This skill enforces best practices for CI/CD pipelines and remote verification in Pulse Music.

---

## ⚠️ Critical Environment Constraint: No Local Compilation

> [!IMPORTANT]
> **Strict Machine Constraint**: Do NOT run local Gradle builds or tests (`./gradlew assemble`, `./gradlew testDebugUnitTest`, etc.) in the agent workspace. The local environment has limited system resources.
>
> All compilation, automated unit test suites, APK generation, and signing verification **MUST** be offloaded to the GitHub Actions CI pipeline (`.github/workflows/ci.yml`).

---

## 1. Remote Build & CI Pipeline Architecture

Pulse Music uses GitHub Actions (`.github/workflows/ci.yml`) triggered on `push` and `pull_request`:
1. **JVM Unit Tests**: Executes `:app:testDebugUnitTest` across player controllers, radio affinity, and caching suites.
2. **APK Assembly**: Assembles `:app:assembleDebug` and `:app:assembleRelease`.
3. **Artifact Publishing**: Packages debug and release APKs into `pulse-music-apks.zip` uploaded to the workflow run.
4. **Release Publishing**: Triggers automated GitHub Release publication upon Git tag creation (`v*`).

---

## 2. Release Keystore Preservation Constraint

* **Static Cryptographic Signature**: Pulse Music requires the static `release.keystore` located in the repository root directory for all release builds.
* **Why**: Locks the app's cryptographic signature across builds so that the in-app Auto-Updater (`AppUpdater.kt`) works seamlessly without Android "Signature match failed" conflicts.
* **Mandatory Rule**: Never alter `release` signing configurations in `app/build.gradle.kts` to use dynamic or auto-generated keys.

---

## 3. Remote CI Inspection & Workflow Verification

To check pipeline status and review build/test logs without running locally, query the GitHub REST API using `curl`:

### A. List Recent Workflow Runs
```bash
curl -s -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/chriz-3656/Pulse-Music/actions/runs?per_page=5"
```

### B. Inspect Failed Jobs & Error Steps
```bash
# Get job IDs for a specific run:
curl -s -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/chriz-3656/Pulse-Music/actions/runs/<RUN_ID>/jobs"

# Fetch step execution status and failure logs:
curl -s -L -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/chriz-3656/Pulse-Music/actions/jobs/<JOB_ID>/logs"
```

---

## 4. Release Tagging & Changelog Workflow

When cutting a new version:
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add a new section in `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/) standards.
3. Update version references in `README.md` and `pulse-landing/config.json`.
4. Commit changes, tag the commit (`git tag vX.Y.Z`), and push to trigger automated release packaging:
   ```bash
   git push origin main --tags
   ```
