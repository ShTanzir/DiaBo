# DiaBo — Phase 5: Hardening

This phase adds the safety net around everything built in Phases 1–4: automated
tests, crash visibility, and a disciplined release process — per the PRD's
"zero bugs" requirement (§11).

## 1. Automated test suite

| Layer | File(s) | Tool |
|---|---|---|
| Pure logic | `JavaMethodExtractorTest` | plain JUnit |
| Android-framework-dependent logic | `XmlLayoutParserTest`, `XmlViewRendererTest`, `JavaClickSimulatorTest`, `FileManagerTest` | Robolectric |
| Data layer (Room + filesystem together) | `ProjectRepositoryTest` (in-memory Room DB) | Robolectric |
| UI flow | `NewProjectDialogTest` | Compose UI test (instrumented) |

Run them:
```bash
./gradlew testDebugUnitTest        # JVM + Robolectric tests
./gradlew connectedDebugAndroidTest # Compose UI test, needs a device/emulator
```

**Coverage focus, deliberately**: the tests target the riskiest Phase 2/3 logic —
XML parsing, the sandboxed click simulator, and file I/O — because those are the
places a subtle bug would either crash the app or (worse) silently corrupt a
user's project. UI screens are covered by one representative flow
(`NewProjectDialogTest`) rather than every screen, to keep the suite fast; widen
this list as new features land rather than trying to reach 100% line coverage.

## 2. Crashlytics setup (one-time, required to activate)

Crash reporting is wired in (`util/CrashReporter.kt`, a global uncaught-exception
handler in `DiaBoApplication`) but stays a **safe no-op** until you do this:

1. Create a Firebase project at https://console.firebase.google.com.
2. Add an Android app with package name `com.tanzir.diabo`.
3. Download the generated `google-services.json` and place it at `app/google-services.json`.
4. That's it — `app/build.gradle.kts` detects the file and applies the
   `google-services` + `crashlytics` Gradle plugins automatically. Without the
   file, the app builds and runs identically, just without crash reporting.

Once enabled, `CrashReporter` is available via Hilt injection anywhere:
```kotlin
class SomeViewModel @Inject constructor(private val crashReporter: CrashReporter) {
    fun risky() {
        try { ... } catch (e: Exception) { crashReporter.recordException(e) }
    }
}
```

## 3. State management discipline (already in place, documented here)

Every screen follows one pattern: a single `StateFlow<XyzUiState>` per ViewModel,
built via `combine(...)` over the individual mutable sources, with the Composable
only ever reading that one state object. This was true from Phase 1 onward
(`HomeUiState`, `ProjectListUiState`, `CodeIdeUiState`) — Phase 5 doesn't change
the pattern, it just adds tests that lock it in place.

## 4. Staged rollout process (Play Console)

DiaBo isn't published yet, so this is the process to follow when it is:

1. **Internal testing track** — upload the release AAB, add your own account +
   a couple of trusted testers. Watch Crashlytics for at least a few days of
   real usage.
2. **Closed beta** — widen to a small group (20–50 people). Watch:
   - Crash-free users % (target ≥ 99.5% before widening further)
   - ANR rate
   - Any Cloud Build failures reported (Phase 3 feature is the highest-risk
     surface since it depends on the user's own GitHub setup)
3. **Staged production rollout** — Play Console supports percentage rollouts
   (e.g., 5% → 20% → 50% → 100%). Increase only after the crash-free bar above
   holds for 48+ hours at each stage. Halt and roll back immediately if it drops.
4. **Post-launch**: keep the Phase 1 CI (`build-diabo.yml`) green on every
   commit — a red `main` branch should never be eligible for the next rollout step.

## 5. Known limitations carried into this phase (tracked, not hidden)

- XML syntax highlighting still needs the one-time TextMate asset drop (Phase 2 note).
- Import via SAF (file/folder picker) is still stubbed.
- Per-app language switching (`AppCompatDelegate`) works best with
  `AppCompatActivity`; `MainActivity` is a plain `ComponentActivity` for Compose
  simplicity, so a manual activity recreation may occasionally be needed after
  switching language — worth revisiting if this becomes a real pain point.
- No code has been compiled/run in an actual Android environment during this
  build (no Android SDK available in this sandbox) — treat the first Android
  Studio sync as the true first compile check, and report back any errors.

## 6. Full-project review pass (post-Phase-5 audit)

A dedicated review pass across all 53 Kotlin files found and fixed the following
real bugs before this ZIP was cut:

1. **Missing launcher icon** — `AndroidManifest.xml` referenced `@mipmap/ic_launcher`
   with no icon resources present at all; this would have failed the build outright.
   Fixed: generated real PNGs at every density bucket + an adaptive icon XML.
2. **`java.util.Base64` requires API 26+**, but minSdk is 24 — would crash on
   Android 7.0/7.1 devices when triggering a Real Build. Fixed: switched to
   `android.util.Base64` (available since API 1).
3. **`java.time.Instant` also requires API 26+** for the same reason (used to
   parse GitHub run timestamps). Fixed: enabled core library desugaring.
4. **`FileProvider` path config mismatch** — declared `<external-files-path>`
   while `FileManager` actually uses `context.filesDir` (true internal storage,
   which needs `<files-path>`). This would have thrown
   `IllegalArgumentException` on every "Install Now"/"Share APK" tap. Fixed.
5. **Data-loss bug in `CodeIdeViewModel.onCleared()`** — the unsaved-changes
   flush used `viewModelScope.launch { ... }`, but `viewModelScope` is already
   cancelled by the time `onCleared()` runs, so it silently did nothing. Fixed
   with a synchronous `runBlocking` fallback used only at that shutdown point.
6. **`VerticalDivider()` version risk** — this Material3 composable needs
   material3 1.3.0+, which the pinned compose-bom may not include. Replaced
   with a manually-drawn divider `Box` that compiles regardless of version.
7. **Broken UX promise** — the delete confirmation said "restore from Trash
   within 24 hours," but no Trash screen existed and the purge job was never
   scheduled. Added `TrashSheet.kt`, `TrashPurgeWorker`, and
   `TrashPurgeScheduler` (scheduled once from `MainActivity.onCreate`).
8. **Robolectric tests implicitly used the real `DiaBoApplication`** (Hilt +
   WorkManager) instead of a plain `Application`, which was unnecessary risk
   for tests that construct their own dependencies manually. Fixed by pinning
   `@Config(application = android.app.Application::class)`.
9. Added the missing `ksp { arg("room.schemaLocation", ...) }` block so
   `exportSchema = true` has somewhere to write (was silently warning before).

### One documented risk NOT fixed (needs real-world testing to resolve safely)
`CloudBuildRepository`'s run-matching logic (finding which GitHub Actions run
corresponds to a given `build_id`) works by time-window heuristics, because
GitHub's `workflow_dispatch` API doesn't return a run ID synchronously. If two
builds are dispatched within a few seconds of each other against the same
template repo, they could theoretically cross-match. Low risk for a
single-user template repo (the intended setup), but worth hardening further
(e.g., embedding the build_id in a run-name via a wrapper step) if this repo
is ever shared across multiple concurrent users.
