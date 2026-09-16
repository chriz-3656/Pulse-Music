# 🤝 Contributing to Pulse Music

Thank you for your interest in contributing to **Pulse Music**! We appreciate your support in making Pulse Music the best open-source Hi-Fi music player on Android.

Please take a moment to review this document before submitting bug reports, feature requests, or pull requests.

---

## 📜 Code of Conduct

All contributors and community members are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md). Please treat everyone with respect, kindness, and empathy.

---

## 🛠️ How Can I Contribute?

### 1. Reporting Bugs
- Check existing [GitHub Issues](https://github.com/your-username/pulse-music/issues) to ensure your problem hasn't already been reported.
- Use our **Bug Report Template** when submitting an issue.
- Include:
  - Android version, device manufacturer, and model.
  - Clear steps to reproduce the bug.
  - Expected vs. actual behavior.
  - Logcat output or crash traces if applicable.

### 2. Suggesting Features
- Submit a proposal using our **Feature Request Template**.
- Clearly articulate the problem the feature solves and provide UI mockups or examples if applicable.
- Discuss ideas with the maintainers before writing code for major architecture shifts or large new features.

### 3. Submitting Pull Requests (PRs)
- **One PR per feature/fix**: Keep PRs focused, cohesive, and easy to review.
- Write meaningful commit messages following the [Conventional Commits](https://www.conventionalcommits.org/) specification (e.g., `feat:`, `fix:`, `refactor:`, `test:`, `docs:`).
- Ensure all existing unit tests pass and add new tests covering your changes.

---

## 💻 Development Workflow

### Setting Up Your Environment
1. Fork the repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/pulse-music.git
   cd pulse-music
   ```
3. Open the project in **Android Studio (Ladybug 2024.2+)**.
4. Allow Gradle to sync dependencies from the Version Catalog (`gradle/libs.versions.toml`).

### Branch Naming Conventions
Create a descriptive branch for your work:
- Feature: `feature/offline-lyrics-sync`
- Bug Fix: `bugfix/soundcloud-stream-header`
- Refactor: `refactor/mediasession-cleanup`
- Documentation: `docs/update-readme`

### Verification & Testing
Before submitting a PR, ensure the build succeeds and tests pass:

```bash
# Run JVM unit tests
gradle :app:testDebugUnitTest

# Verify app compilation
gradle :app:assembleDebug
```

---

## 📐 Coding Standards & Guidelines

- **Language**: Kotlin 2.0+ exclusively. Follow official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **UI & Theming**:
  - Build UI exclusively in **Jetpack Compose** with Material 3 components.
  - Never hardcode color hex strings inside composables; reference `Theme.kt` color tokens or `MaterialTheme.colorScheme`.
  - Maintain the project's signature skeuomorphic amber glow and tactile metallic aesthetic.
- **Architecture**:
  - Follow Clean Architecture with strict separation between Data, Domain, and UI layers.
  - Use `StateFlow` and `SharedFlow` for reactive UI state.
  - Inject dependencies cleanly through `AppContainer` (manual constructor injection).
- **Accessibility**:
  - Provide non-empty `contentDescription` for interactive icons.
  - Ensure touch targets are at least 48dp x 48dp.
- **Data Persistence**:
  - When modifying Room database schemas, ensure appropriate database migration or fallback logic is configured.

---

## 📋 Pull Request Checklist

Before opening your pull request, verify:
- [ ] My code follows the code style of this project.
- [ ] I have verified that all existing tests pass (`gradle :app:testDebugUnitTest`).
- [ ] I have added tests that prove my fix is effective or that my feature works.
- [ ] I have updated relevant documentation (README, KDoc comments).
- [ ] My branch is rebased onto the latest `main` branch.

Thank you for helping make Pulse Music awesome! 🎧
