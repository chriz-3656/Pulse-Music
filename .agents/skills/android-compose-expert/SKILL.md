---
name: android-compose-expert
description: >-
  Advanced Jetpack Compose development patterns, tactile skeuomorphic & neumorphic UI design,
  recomposition performance optimization, state hoisting, canvas drawing, and lyric teleprompter
  rendering. Use when developing or modifying Compose screens, custom buttons, animations,
  or player UI components.
metadata:
  model: inherit
---

# Android Jetpack Compose Expert Guide

This skill guides UI development in Pulse Music using modern Jetpack Compose, Material 3, and custom analog hardware styling (Skeuomorphic & Neumorphic design language).

---

## 1. Neumorphic & Skeuomorphic Design Standards

Pulse Music employs a vintage analog audio hardware aesthetic with tactile mechanical feedback and warm amber accents (`#FFC107`).

### Tactile Spring Buttons (`SkeuoTactileButton`)
* **Physical Spring Depression**: Simulate tactile button travel using `animateDpAsState` on press interactions.
* **Scroll vs. Tap Decoupling**: Do not capture vertical scroll gestures in touch modifiers. Use `Modifier.pointerInput` with `detectTapGestures` or `clickable(interactionSource, indication = null)` so parent `LazyColumn` scrolling remains fluid.
* **Neumorphic Elevation & Shadow Hygiene**:
  * Ensure adequate outer margin and inner padding around elevated cards and buttons so dark and light drop shadows do not bleed or clip into adjoining components.

---

## 2. Recomposition Hygiene & Performance

Pulse Music plays high-framerate animated VU meters and real-time lyric scrolling; avoiding unnecessary recompositions is critical.

### Key Performance Rules:
1. **Defer State Reads to Draw/Layout Phase**:
   * For continuous animations (e.g., VU meter needles, progress sliders), prefer `Modifier.graphicsLayer { rotationZ = ... }` or `Modifier.drawBehind { ... }` rather than rebuilding the Composable tree on every frame.
2. **Stable Keys in Lazy Lists**:
   * Always provide explicit, stable keys in `LazyColumn` and `LazyRow`:
     ```kotlin
     LazyColumn {
         items(items = songs, key = { it.id }) { song ->
             SongRow(song = song)
         }
     }
     ```
3. **Remembered Lambdas**:
   * Pass hoisted event lambdas `(Song) -> Unit` or method references rather than creating unremembered inline closures inside list item scopes.
4. **Derived State**:
   * Wrap derived conditions in `remember { derivedStateOf { ... } }` to ensure dependents only recompose when the boolean/value actually changes.

---

## 3. Synced Lyrics & Teleprompter (`SyncedLyricsView`)

* **LRC Parsing**: Time-synced lyrics map timestamps (in milliseconds) to line indices.
* **Auto-Scroll Behavior**:
  * Use `LaunchedEffect(activeLyricIndex)` with `lazyListState.animateScrollToItem(index, scrollOffset)` centered on the active vocal line.
  * Allow manual user scroll gestures to temporarily suspend auto-scroll for 3–5 seconds before resuming follow mode.

---

## 4. ViewModel & UI State Integration

* **Unidirectional Data Flow (UDF)**:
  * ViewModels expose a single immutable `StateFlow<UiState>`.
  * Composable screens consume state via `collectAsStateWithLifecycle()`.
* **Side Effects**:
  * Execute one-off navigation or snackbar alerts via `SharedFlow` or `Channel` observed with `LaunchedEffect`.
