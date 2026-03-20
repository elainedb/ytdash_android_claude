# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android app with Google Sign-In authentication that displays YouTube videos from specified channels. Features include video feed with filtering/sorting, local Room database caching, and a map view showing video recording locations.

## Build & Run Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew testDebugUnitTest      # Run unit tests
./gradlew test --tests "dev.elainedb.android_claude.utils.StringHelperTest"  # Run single test class
./gradlew JacocoDebugCodeCoverage  # Generate coverage report (HTML+XML)
./gradlew lint                   # Run Android lint
```

## Configuration

Sensitive config files are gitignored. For local development:
1. `app/google-services.json` — download from Firebase Console (see FIREBASE_SETUP.md)
2. `app/src/main/assets/config.properties` — copy from `config.properties.template`, add real YouTube API key and authorized emails (see CONFIG_SETUP.md)

`ConfigHelper` loads properties with fallback chain: `config.properties` → `config.properties.ci` → `config.properties.template` → hardcoded defaults.

CI builds auto-generate dummy versions of both files.

## Architecture

Single-module Android app (`dev.elainedb.android_claude`) using Jetpack Compose, targeting SDK 36 / min SDK 29.

**Data flow:** YouTube Data API v3 → Retrofit (`YouTubeApiService`) → `YouTubeRepository` → Room DB cache (`VideoDao`/`VideoEntity`) → `VideoListViewModel` (StateFlow) → Compose UI

Key layers:
- **network/** — Retrofit interface for YouTube search and video details endpoints
- **model/** — API response models (kotlinx.serialization) + `Video` domain model with extension functions for conversion
- **repository/** — `YouTubeRepository` orchestrates API calls (paginated, batched in parallel via coroutines), reverse geocoding, Room caching (24h expiry), filtering, and sorting
- **database/** — Room database with `VideoEntity` and `VideoDao` (combined filter+sort query)
- **viewmodel/** — `VideoListViewModel` with sealed `VideoListUiState`, exposes filter/sort state as StateFlow
- **ui/** — Compose screens: `VideoListScreen`, `FilterDialog`, `SortDialog`

**Activities:**
- `LoginActivity` — Google Sign-In with email allowlist from config
- `MainActivity` — hosts Compose video list UI
- `MapActivity` — OSMDroid map showing geolocated videos

### Key Patterns

- **State management**: StateFlow in ViewModel, consumed by Compose UI
- **Caching**: Room database with 24-hour TTL. Repository falls back to cache when remote fails
- **Networking**: Retrofit + OkHttp with kotlinx.serialization. Paginated API calls batched in parallel via coroutines
- **Serialization**: kotlinx.serialization for API response models

## Testing

JUnit tests with JaCoCo for coverage. Run `./gradlew testDebugUnitTest` for unit tests, `./gradlew JacocoDebugCodeCoverage` for coverage report (HTML+XML).

## CI/CD

GitHub Actions: checkout → build → test → lint → SonarQube scan. Runs on pushes to main and PRs.

## Key Dependencies

- Retrofit + OkHttp + kotlinx.serialization for networking
- Room + KSP for local database
- Coil for image loading
- OSMDroid for maps (no Google Maps dependency)
- Firebase Performance Monitoring
- Google Play Services Auth for sign-in
- SonarQube integration for code quality
