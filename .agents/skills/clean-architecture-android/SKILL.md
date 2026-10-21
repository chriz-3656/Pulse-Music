---
name: clean-architecture-android
description: >-
  Clean Architecture, Modern Android Development (MAD), Room database caching,
  Kotlin Coroutines lifecycle management, and repository pattern best practices.
  Use when architecting new features, updating data models, modifying DAOs/repositories,
  or managing coroutine dispatchers and reactive state flows.
metadata:
  model: inherit
---

# Clean Architecture for Android & Modern Kotlin

This skill outlines architectural boundaries, concurrency patterns, and data persistence rules for Pulse Music.

---

## 1. Architectural Layers & Separation of Concerns

Pulse Music strictly follows Clean Architecture:

```text
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Compose Screens • Components • ViewModels (StateFlow)│
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                      Domain Layer                      │
│   Use Cases • Domain Models (Song, Playlist) • Repo IFs│
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│                       Data Layer                       │
│   Room Database • Remote API Clients • Repo Impls      │
└────────────────────────────────────────────────────────┘
```

### Layer Constraints:
* **Presentation**: Depends **only** on Domain (`UseCases`, `DomainModels`, `MusicRepository` interface). ViewModels must **never** import Room DAOs, Retrofit interfaces, or raw network DTOs.
* **Domain**: Contains zero Android framework dependencies (pure Kotlin). Defines business models and repository contracts.
* **Data**: Implements repository interfaces, maps network DTOs and Room entities into Domain models.

---

## 2. Kotlin Coroutines & Thread Concurrency

1. **Dispatcher Hygiene**:
   * Network I/O and Room DB calls **MUST** execute on `Dispatchers.IO`.
   * UI state updates, ExoPlayer commands, and UI flows **MUST** execute on `Dispatchers.Main` / `Dispatchers.Main.immediate`.
   * Avoid hardcoded `GlobalScope`. Use structured concurrency with `viewModelScope`, `serviceScope`, or injected `CoroutineScope`.
2. **StateFlow Updates**:
   * Always use atomic `.update { }` mutations to avoid race conditions:
     ```kotlin
     _uiState.update { currentState ->
         currentState.copy(isLoading = false, songs = fetchedSongs)
     }
     ```
3. **Flow Cancellation & Lifecycle**:
   * Collect flows in Composables using `collectAsStateWithLifecycle()` to prevent background collection when the UI is invisible or stopped.

---

## 3. Room Database & Offline-First Strategy

1. **Single Source of Truth**:
   * Cache remote catalog results, favorites, and playlists in Room SQLite tables (`SongEntity`, `PlaylistEntity`).
   * Expose reactive data streams as `Flow<List<Song>>` from DAOs.
2. **Database Migrations**:
   * Whenever adding new entity columns, update database schema versions in `MusicDatabase.kt` and provide appropriate migration paths or fallback destructuring.
3. **Primary Key Deduplication**:
   * Ensure track IDs are normalized (strip prefixes and URL query fragments) before saving to Room cache to avoid duplicate row insertions.
