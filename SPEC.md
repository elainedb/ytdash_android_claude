# SPEC.md

This specification describes the Android application to be built. It is organized into five incremental versions, each building on the previous. The app is a native Android application (Kotlin, Jetpack Compose) that authenticates users via Google Sign-In and displays YouTube video data with caching, filtering, sorting, map visualization, and performance monitoring.

**Package:** `dev.elainedb.{project_name}`
**Min SDK:** 29 | **Target/Compile SDK:** 36

---

## Architecture

The project follows **Clean Architecture** organized by feature under the main package. Each feature has three layers:

- **Domain:** Kotlin data classes for entities, abstract repository interfaces, and use cases. Use cases implement a common `UseCase<T, P>` base class. All repository methods return `Result<T>` (a sealed class) for type-safe error handling.
- **Data:** Repository implementations, remote data sources (Retrofit API calls), local data sources (Room database), and serialization models with `@Serializable` annotations. Models have `toEntity()` extension functions to convert to domain entities.
- **Presentation:** ViewModels with `StateFlow` for state management and Jetpack Compose screens.

### Dependency Injection (Hilt)

Uses Hilt (`dagger.hilt.android`) for dependency injection:
- `@HiltAndroidApp` on the `Application` class.
- `@AndroidEntryPoint` on Activities.
- `@HiltViewModel` on ViewModels.
- A `NetworkModule` provides singleton instances of `OkHttpClient`, `Retrofit`, and `YouTubeApiService`.
- A `DatabaseModule` provides the `VideoDatabase` and `VideoDao` singletons.
- A `RepositoryModule` binds repository implementations to their interfaces.

### Error Handling

- **Exceptions** (`core/error/Exceptions.kt`): A sealed class `AppException` with subclasses `ServerException`, `CacheException`, `NetworkException`, and `AuthException`. Each carries a `message: String`.
- **Failures** (`core/error/Failures.kt`): A sealed class `Failure` with variants: `Server`, `Cache`, `Network`, `Auth`, `Validation`, `Unexpected`. Each carries a `message: String`.
- **Result type** (`core/error/Result.kt`):
  ```kotlin
  sealed class Result<out T> {
      data class Success<T>(val data: T) : Result<T>()
      data class Error(val failure: Failure) : Result<Nothing>()
  }
  ```
- Data layer catches exceptions and returns `Result.Error`. Presentation layer maps failures to UI error messages.

### Use Case Base Class

`core/usecases/UseCase.kt` defines:
```kotlin
abstract class UseCase<out T, in P> {
    abstract suspend operator fun invoke(params: P): Result<T>
}
```
Use cases that take no parameters use a `Unit` params type.

---

## V1 — Google Sign-In Authentication

### Overview
A login screen that authenticates users via Google Sign-In. Only users whose email addresses appear on an authorized list may access the app.

### Login Screen (`LoginActivity`)
- Display a title: **"Login with Google"**.
- Display a button labeled **"Sign in with Google"**.
- Display an error message area below the button (initially empty, red text).
- Use a traditional Android XML layout (`activity_login.xml`) with a centered `LinearLayout`.

### Authentication Logic
- Use `GoogleSignInClient` from `play-services-auth` for Google authentication.
- After sign-in, check the user's email against an authorized email list.
- **If authorized:** log "Access granted to [email]" and navigate to `MainActivity`. Use `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` to clear the back stack.
- **If unauthorized:** display "Access denied. Your email is not authorized." in the error area. Sign the user out immediately so they can retry with a different account.

### Configuration System (`ConfigHelper`)
Authorized emails and the YouTube API key are loaded from properties files with a fallback chain:
1. `app/src/main/assets/config.properties` (local development — gitignored)
2. `app/src/main/assets/config.properties.ci` (CI builds — committed)
3. `app/src/main/assets/config.properties.template` (fallback template — committed)
4. Hardcoded fallback values (last resort)

The `config.properties` file format:
```properties
authorized_emails=email1@domain.com,email2@domain.com
youtubeApiKey=YOUR_YOUTUBE_API_KEY
```

### Firebase Setup
- The app requires a `google-services.json` file in `app/` (gitignored).
- A `google-services.json.template` with dummy values is committed for CI.
- A Gradle task `generateDummyGoogleServices` automatically creates `google-services.json` from the template before `preBuild` if it doesn't exist.

---

## V2 — YouTube Video List

### Overview
After login, the user sees a main screen displaying a list of YouTube videos fetched from four specific channels. Tapping a video opens it in YouTube.

### Main Screen (`MainActivity`)
- Uses Jetpack Compose. `MainActivity` hosts the `VideoListScreen` composable.
- Displays a top app bar with a **Logout** button that signs the user out of Google and navigates back to `LoginActivity`.

### Video List UI (`VideoListScreen`)
- Each video item in a `LazyColumn` displays:
  - Thumbnail image (loaded via Coil's `AsyncImage`)
  - Title
  - Source channel name
  - Publication date (formatted as `YYYY-MM-DD`)
- Videos are sorted by publication date, newest first.
- Tapping a video opens it via Intent: tries the YouTube app first (`vnd.youtube:` URI), falls back to browser (`https://www.youtube.com/watch?v=`).

### YouTube Data API Integration (`YouTubeApiService`)
- Uses Retrofit with kotlinx.serialization JSON converter.
- Base URL: `https://www.googleapis.com/youtube/v3/`
- **Search endpoint** (`GET /search`): fetches videos from a channel. **Exhaustive pagination:** follows `nextPageToken` until absent (see Repository below).
  - Parameters: `part=snippet`, `channelId`, `maxResults=50`, `order=date`, `type=video`, `pageToken`, `key`
- Fetches from four hardcoded channel IDs:
  - `UCynoa1DjwnvHAowA_jiMEAQ`
  - `UCK0KOjX3beyB9nzonls0cuw`
  - `UCACkIrvrGAQ7kuc0hMVwvmA`
  - `UCtWRAKKvOEA0CXOue9BG8ZA`

### Data Models (`YouTubeModels`)
- API response models: `YouTubeSearchResponse`, `YouTubeVideoItem`, `YouTubeVideoId`, `YouTubeVideoSnippet`, `YouTubeThumbnails`, `YouTubeThumbnail` — all annotated with `@Serializable` and `@SerialName`.
- Domain model: `Video` data class with fields: `id`, `title`, `channelName`, `channelId`, `publishedAt`, `thumbnailUrl`, `description`, `tags`, `locationCity`, `locationCountry`, `locationLatitude`, `locationLongitude`, `recordingDate`.
- Extension function `YouTubeVideoItem.toVideo()` converts API model to domain model.

### Repository (`YouTubeRepository`)
- Fetches all channels in parallel using `coroutineScope { async {} }.awaitAll()`.
- **Exhaustive pagination:** For each channel, follows `nextPageToken` until the API returns no more pages (i.e., `nextPageToken` is `null`/absent in the response). This ensures every video from every channel is fetched regardless of channel size. There is no artificial page limit — pagination continues until the API signals completion. Each page requests `maxResults=50`.
- OkHttp client adds `X-Android-Package` and `X-Android-Cert` (SHA1 signature) headers for API key restrictions.
- Uses `HttpLoggingInterceptor` at BODY level.

### Use Cases
- `SignInWithGoogle` — triggers Google Sign-In flow, validates email, returns `Result<User>`.
- `SignOut` — signs out from Google, takes `Unit`.
- `GetVideos` — params: `GetVideosParams(channelIds, forceRefresh)`, fetches videos via repository.
- `GetVideosByChannel` — params: `channelName: String`, queries cached videos by channel.
- `GetVideosByCountry` — params: `country: String`, queries cached videos by country.

### ViewModel (`VideoListViewModel`)
- Injected via `@HiltViewModel` with use cases as constructor dependencies.
- Exposes `uiState: StateFlow<VideoListUiState>`.
- `VideoListUiState` is a sealed class with variants: `Loading`, `Empty`, `Success(videos, totalCount)`, `Error(message)`.

---

## V3 — Enhanced Data, Caching, Filtering & Sorting

### Overview
Extends V2 with additional video metadata (tags, location, recording date), Room-based local caching, and filter/sort functionality.

### Additional Video Data
- **Videos endpoint** (`GET /videos`): fetches detailed metadata for batches of video IDs.
  - Parameters: `part=snippet,recordingDetails`, `id` (comma-separated, max 50 per request), `key`
  - Response models: `YouTubeVideosResponse`, `YouTubeVideoDetails`, `YouTubeVideoDetailsSnippet`, `YouTubeRecordingDetails`, `YouTubeLocation`
- Extension function `Video.mergeWithDetails()` enriches a Video with tags, GPS coordinates, and recording date from the details endpoint.

### Reverse Geocoding (`LocationUtils`)
- Converts GPS coordinates to city/country names using Android's `Geocoder`.
- On Android 13+ (Tiramisu), uses the async callback-based `Geocoder.getFromLocation()` API.
- On older versions, uses the synchronous API.
- City resolution fallback chain: `locality` → `subAdminArea` → `adminArea` → `subLocality` → `thoroughfare`.
- Gracefully returns `(null, null)` if Geocoder is unavailable.

#### Performance & Robustness
- **In-memory cache:** A `ConcurrentHashMap<Pair<Double, Double>, Pair<String?, String?>>` caches resolved coordinates to avoid redundant lookups. Coordinates are rounded to 3 decimal places (~111m precision) as the cache key to group nearby points.
- **Concurrency control:** Geocoding runs on `Dispatchers.IO` with a `Semaphore(maxConcurrency = 5)` to limit parallel lookups and avoid overwhelming the Geocoder.
- **Retry with backoff:** Failed lookups retry up to 3 times with exponential backoff (500ms, 1s, 2s) before falling back.
- **Fallback chain:** If the Android `Geocoder` is unavailable or all retries fail, falls back to the OpenStreetMap Nominatim API (`nominatim.openstreetmap.org/reverse`) with a proper `User-Agent` header (`dev.elainedb.{project_name}/1.0`) and 1-second minimum delay between requests (Nominatim policy). If Nominatim also fails, falls back to parsing `locationDescription` from the YouTube API snippet using regex for "City, Country" patterns.
- **Batch processing:** Videos are geocoded in batches during the API fetch phase. Already-cached coordinates are resolved instantly; only uncached coordinates hit the Geocoder or network.

### Room Database

#### `VideoEntity`
- Table name: `videos`, primary key: `id`
- Fields: `id`, `title`, `channelName`, `channelId`, `publishedAt`, `thumbnailUrl`, `description`, `tags` (comma-separated string), `locationCity`, `locationCountry`, `locationLatitude`, `locationLongitude`, `recordingDate`, `cacheTimestamp`
- Conversion functions `toVideo()` and `Video.toEntity()` handle domain ↔ entity mapping. Tags are stored as comma-separated strings.

#### `VideoDao`
- `getVideosWithFiltersAndSort(channelName, country, sortBy)` — combined filter+sort query using `CASE WHEN` SQL expressions. Returns `Flow<List<VideoEntity>>`.
- `getDistinctCountries()`, `getDistinctChannels()` — for populating filter dropdowns.
- `getVideosWithLocation()` — returns videos with non-null GPS coordinates (used by map).
- `getVideosNewerThan(threshold)` — cache freshness check.
- `getTotalVideoCount()` — total count for display.
- Standard CRUD: `insertVideos`, `insertVideo`, `updateVideo`, `deleteVideo`, `deleteAllVideos`, `deleteOldVideos`.

#### `VideoDatabase`
- Room database, version 1, singleton with double-checked locking via `@Volatile` + `synchronized`.
- Single entity: `VideoEntity`.

### Caching Strategy
- Cache expiry: **24 hours** (`CACHE_EXPIRY_HOURS = 24`).
- `getLatestVideos()`: checks cache first; if cached videos exist newer than threshold, returns them. Otherwise fetches from API.
- `refreshVideos()`: always fetches from API and updates the cache.
- After API fetch, videos are inserted with `OnConflictStrategy.REPLACE`.

### Enhanced Video List UI
Each video item now additionally displays:
- Tags as chips
- Location (city, country, GPS coordinates) when available
- Recording date when available
- Video count display: "Showing X of Y videos" when filters are active

### Filter Dialog (`FilterDialog`)
- Modal alert dialog with radio buttons.
- Filter by **channel name** (one of the four channels, or "All Channels").
- Filter by **country** (from distinct countries in DB, or "All Countries").
- Apply and Cancel buttons.

### Sort Dialog (`SortDialog`)
- Modal alert dialog with radio buttons.
- Four sort options:
  - Publication Date (Newest First) — default
  - Publication Date (Oldest First)
  - Recording Date (Newest First)
  - Recording Date (Oldest First)
- Apply and Cancel buttons.

### Control Buttons
The main screen has a row of buttons: **Refresh**, **View Map**, **Filter**, **Sort**.

### ViewModel Enhancements
- `_filterOptions: MutableStateFlow<FilterOptions>` and `_sortOption: MutableStateFlow<SortOption>` drive reactive filtering.
- `observeVideoChanges()` uses `Flow.combine` on filter+sort options, then `collectLatest` on the repository's Flow to update UI state in real time.
- Exposes `availableCountries` and `availableChannels` StateFlows for filter dropdowns.
- Methods: `applyFilter()`, `applySorting()`, `clearFilters()`.

---

## V4 — Map View

### Overview
A new Map Screen displays video recording locations on an interactive OpenStreetMap. Tapping a marker shows video details in a bottom sheet.

### Navigation
- A **"View Map"** button on the Main Screen launches `MapActivity` via Intent.
- `MapActivity` provides a companion `newIntent(context)` factory method.

### Map Screen (`MapActivity`)
- Uses the **osmdroid** library (`org.osmdroid:osmdroid-android:6.1.10`) to display an OpenStreetMap tile-based map.
- Loads videos with valid GPS coordinates from the Room database (`videoDao.getVideosWithLocation()`).
- Places a marker on the map for each geolocated video.
- After all markers are placed, automatically adjusts the map viewport to a bounding box that fits all markers with padding.
- Users can manually pan and zoom after the initial fit.

#### OpenStreetMap Tile Usage Policy Compliance
The app must comply with the [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/) to avoid request rejection or blocking:
- **User-Agent identification:** Configure osmdroid with a unique, identifiable `User-Agent` string (e.g., `dev.elainedb.{project_name}/1.0`) via `Configuration.getInstance().userAgentValue`. This is **mandatory** — requests without a proper User-Agent are rejected.
- **Tile caching:** Enable osmdroid's built-in file-based tile cache (`Configuration.getInstance().osmdroidTileCache`) to avoid re-downloading tiles. Never send `Cache-Control: no-cache` or `Pragma: no-cache` headers — respect the server's cache directives.
- **Rate limiting:** Do not aggressively prefetch tiles. Rely on osmdroid's default tile loading behavior which loads tiles on-demand as the user pans/zooms. Avoid custom tile download loops.
- **No bulk downloading:** Only request tiles that are visible in the current viewport. Do not preload large tile areas.

### Marker Interaction
- Tapping a marker displays a **bottom sheet** with:
  - Video thumbnail
  - Title
  - Source channel name
  - Publication date
  - Tags
  - Location (city, country, GPS coordinates)
  - Recording date
- The bottom sheet occupies no more than ~25% of screen height and is dismissible by swiping down.
- Tapping the bottom sheet opens the video in YouTube (app with `vnd.youtube:` URI fallback to browser).

---

## V5 — Performance Monitoring

### Overview
Adds Firebase Performance Monitoring to track app performance metrics.

### Dependencies
- Firebase BOM: `com.google.firebase:firebase-bom:33.7.0`
- Firebase Performance: `com.google.firebase:firebase-perf-ktx`
- Gradle plugin: `com.google.firebase.firebase-perf` version `1.4.2`

### Integration
- The Firebase Performance plugin is applied in the app-level `build.gradle.kts`.
- Automatic monitoring of app startup time, HTTP requests, and screen rendering is enabled by including the dependency.

---

## Code Quality & CI

### SonarQube
- Plugin: `org.sonarqube` version `6.3.1.5724`
- Project key: `elainedb_{project_name}`, organization: `elainedb`
- Sources: `src/main/java`, tests: `src/test/java`
- Coverage via JaCoCo XML report.

### JaCoCo
- Enabled for debug builds (`enableUnitTestCoverage = true`).
- Task `JacocoDebugCodeCoverage` generates XML + HTML reports.
- Root task `jacocoTestReport` depends on the app-level JaCoCo task.

### Unit Tests
- Located in `app/src/test/java/dev/elainedb/{project_name}/`.
- `StringHelperTest` covers utility functions: palindrome check, word count, word reversal, capitalization, vowel removal, email validation.

### Utility Functions (`StringHelper`)
- `isPalindrome(input)` — ignores non-alphanumeric characters, case-insensitive.
- `countWords(input)` — splits on whitespace regex.
- `reverseWords(input)` — reverses word order.
- `capitalizeWords(input)` — title-cases each word.
- `removeVowels(input)` — strips vowels (case-insensitive regex).
- `isValidEmail(email)` — validates email format via regex.

---

## Key Dependencies

| Library | Purpose |
|---|---|
| `play-services-auth` 21.2.0 | Google Sign-In |
| `hilt-android` 2.51.1 + `hilt-navigation-compose` | Dependency injection |
| `retrofit` 2.11.0 + `okhttp` 4.12.0 | HTTP networking |
| `kotlinx-serialization-json` 1.7.3 | JSON parsing |
| `room` 2.6.1 + KSP | Local database |
| `coil-compose` 2.7.0 | Image loading |
| `osmdroid-android` 6.1.10 | OpenStreetMap |
| `firebase-bom` 33.7.0 | Firebase platform |
| `firebase-perf-ktx` | Performance monitoring |
| `kotlinx-coroutines-android` 1.9.0 | Async operations |
| `lifecycle-viewmodel-compose` 2.8.7 | ViewModel integration |
| Compose BOM `2024.09.00` + Material3 | UI framework |
| `mockk` 1.13.+ | Mocking for tests |
| `kotlinx-coroutines-test` 1.9.0 | Coroutine testing |

---

## Android Manifest

### Permissions
- `INTERNET` — API calls and map tile loading
- `WRITE_EXTERNAL_STORAGE` — osmdroid tile cache
- `ACCESS_NETWORK_STATE` — network availability checks

### Activities
- `LoginActivity` — launcher activity (MAIN/LAUNCHER intent filter)
- `MainActivity` — video list screen
- `MapActivity` — map screen

---

## Common Agent Mistakes

This section documents recurring mistakes that AI agents make when building this project. Address these proactively to avoid build failures and runtime crashes.

### ClassNotFoundException on `LoginActivity`

**Problem:** The app crashes immediately on launch with:

```
java.lang.RuntimeException: Unable to instantiate activity ComponentInfo{...LoginActivity}
Caused by: java.lang.ClassNotFoundException: Didn't find class "dev.elainedb.{project_name}.LoginActivity"
```

The root cause is that **Kotlin source files are not being compiled into the APK**. Only generated `R` classes end up in the DEX file, so no app code (Activities, ViewModels, etc.) is present at runtime.

**Fix — three issues must be addressed together:**

1. **Missing `kotlin-android` plugin:** The `app/build.gradle.kts` file must include the Kotlin Android plugin in its `plugins` block. Without it, the Android Gradle Plugin ignores all `.kt` files under `src/main/java`:
   ```kotlin
   plugins {
       alias(libs.plugins.kotlin.android)  // REQUIRED — compiles Kotlin source files
       // ... other plugins
   }
   ```

2. **JVM target mismatch:** Once the Kotlin plugin is added, the Kotlin compiler may default to a JVM target (e.g., 21) that differs from the project's `sourceCompatibility` (e.g., Java 11), causing a compilation mismatch. Add a `kotlinOptions` block to synchronize them:
   ```kotlin
   android {
       compileOptions {
           sourceCompatibility = JavaVersion.VERSION_11
           targetCompatibility = JavaVersion.VERSION_11
       }
       kotlinOptions {
           jvmTarget = "11"
       }
   }
   ```

3. **Outdated Retrofit kotlinx.serialization converter import:** Retrofit 2.11.0 natively includes the kotlinx.serialization converter, but agents sometimes import the old JakeWharton library (`com.jakewharton.retrofit2.converter...`). Update the import to use the official converter:
   ```kotlin
   import retrofit2.converter.kotlinx.serialization.asConverterFactory
   ```

### AppCompat Theme Crash on `LoginActivity`

**Problem:** The app crashes on launch with:

```
java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
```

This happens because `LoginActivity` (and `MapActivity`) extend `AppCompatActivity`, but the app theme in `app/src/main/res/values/themes.xml` uses a non-AppCompat theme such as `android:Theme.Material.Light.NoActionBar`.

**Fix:** Change the app theme in `app/src/main/res/values/themes.xml` to an AppCompat-compatible theme. Any of the following work:

- `Theme.AppCompat.DayNight.NoActionBar` — recommended, supports dark mode
- `Theme.AppCompat.Light.NoActionBar` — light only
- `Theme.MaterialComponents.Light.NoActionBar` — Material Components variant, also AppCompat-compatible

Example:
```xml
<resources>
    <style name="Theme.{ProjectName}" parent="Theme.AppCompat.DayNight.NoActionBar">
        <!-- Customize theme attributes here -->
    </style>
</resources>
```

This applies to all Activities that extend `AppCompatActivity`. Since `LoginActivity` and `MapActivity` use XML layouts (not Compose), they must extend `AppCompatActivity` and therefore require an AppCompat theme. `MainActivity` (Compose) uses `ComponentActivity` and is not affected.
