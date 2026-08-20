# DiaBo — Phases 1–5 (Complete Roadmap)

Native Android Java/XML IDE, built to be compiled via GitHub Actions (no
Android Studio required for the end build). This drop implements **all five
phases** from the DiaBo PRD: Core Local IDE, Instant Preview, Cloud Build,
Polish, and Hardening.

> **Post-build review pass**: after Phase 5 was finished, a full review of all
> 53 Kotlin files found and fixed 7 real bugs (a missing launcher icon that
> would have failed the build outright, two minSdk-24-vs-API-26 crashes, a
> FileProvider misconfiguration, a data-loss bug, a Material3 version risk,
> and a broken "restore from Trash" promise with no UI behind it). Full list
> in `PHASE5_HARDENING.md` §6.

## Phase 5 — Hardening (this update, see `PHASE5_HARDENING.md` for full detail)
- **Automated test suite**: `JavaMethodExtractorTest` (pure JVM), plus
  Robolectric tests for `XmlLayoutParser`, `XmlViewRenderer`,
  `JavaClickSimulator`, `FileManager`, and `ProjectRepository` (in-memory Room).
  One Compose UI instrumented test (`NewProjectDialogTest`) covers the
  new-project flow end to end.
- **Crashlytics** wired via `util/CrashReporter.kt` + a global uncaught-exception
  handler in `DiaBoApplication` — stays a safe no-op until you drop in your own
  `google-services.json` (one-time setup, see `PHASE5_HARDENING.md`).
- **CI re-enabled**: `build-diabo.yml` now runs `lint` + `testDebugUnitTest`
  before `assembleDebug`, and uploads test reports as an artifact even on failure.
- **Staged rollout process** documented for when this ships to Play Console.

## Phase 4 — Polish (this update)
- **Templates Gallery** (`ui/templates/`) — Blank Activity, Login Screen,
  List (RecyclerView-style), Bottom Navigation, Settings Screen, Form with
  Validation. Picking one seeds a new project's Java+XML instead of the bare
  default.
- **Backup & Restore** (`data/backup/BackupRepository.kt`) — exports every
  project as a single ZIP (via Storage Access Framework `CreateDocument`) and
  restores from a previously exported ZIP (`OpenDocument`), re-indexing Room
  from the extracted folders.
- **Bilingual UI** — `values-bn/strings.xml` with Bangla translations for all
  chrome strings, plus a language toggle in Settings using
  `AppCompatDelegate.setApplicationLocales` (per-app language, independent of
  system locale).
- **Glassmorphism pass** — gradient-tinted scaffold backgrounds behind
  `GlassCard` throughout Home/Project List/Settings so the frosted-glass effect
  actually has depth to show against.
- **Accessibility pass** — content descriptions added to every icon-only button
  that was missing one; touch targets verified ≥48dp on interactive rows.

## Phase 3 — Cloud Build
- `android-template/` (sibling folder in this ZIP, and its own repo:
  [github.com/ShTanzir/android-template](https://github.com/ShTanzir/android-template))
  — separate minimal Android project + `.github/workflows/diabo-preview-build.yml`
  that "▶ Real Build" triggers via `workflow_dispatch`.
- `data/remote/GitHubApiService.kt` + `NetworkModule.kt` — Retrofit client for
  GitHub's REST API.
- `data/remote/GitHubConfigStore.kt` — PAT stored via EncryptedSharedPreferences
  (AES-256-GCM, Keystore-backed).
- `data/build/CloudBuildRepository.kt` + `BuildPollWorker.kt` — trigger, poll,
  download artifacts, all survivable in the background via WorkManager.
- `util/BuildNotifier.kt` — local notification on build success/failure.
- `ui/ide/RealBuildSheet.kt` — progress stages, screenshot, Install Now/Share APK.
- Settings → GitHub Integration form. Room bumped to schema v2 with an explicit
  `Migration(1, 2)`.

### One-time setup to use Real Build
1. The `android-template/` folder already lives in its own repo:
   **[github.com/ShTanzir/android-template](https://github.com/ShTanzir/android-template)**
   — no push needed unless you're customizing it.
2. Generate a fine-grained PAT (`Actions: read/write`, `Contents: read`) scoped to it.
3. DiaBo → Settings → GitHub Integration → enter token + owner (`ShTanzir`) / repo (`android-template`).
4. Open a project's `.java` and `.xml` tabs, tap ▶ Real Build.

## Phase 2 — Instant Preview
- sora-editor integration for real Java syntax highlighting (`ui/editor/SoraCodeEditor.kt`)
- XML → real Android View renderer, no compiled resources needed (`preview/XmlViewRenderer.kt`)
- Sandboxed Java `onClick` simulator (Toast / setText / setVisibility subset)
- Instant Preview bottom sheet, debounced ~400ms

## Phase 1 — Core Local IDE
- Home, Project List, File Explorer sidebar (16 features), multi-tab editor
- Room-backed local persistence + atomic file I/O (`FileManager.kt`)
- Nature-inspired glassmorphism theme

## Remaining known gaps (tracked honestly, not hidden — see `PHASE5_HARDENING.md` §5)
- Import (SAF file/folder picker for arbitrary external files) — stubbed
- XML syntax highlighting needs a one-time TextMate asset drop (Java is fully
  highlighted without it) — see `ui/editor/SoraCodeEditor.kt` doc comment
- Crashlytics needs your own `google-services.json` to activate
- No code in this drop has been compiled in a real Android environment (no
  Android SDK in this sandbox) — first Android Studio sync is the true first
  compile check

## Getting it running
1. Open the `DiaBo/` folder in Android Studio (Iguana+), or build headlessly
   via GitHub Actions (`.github/workflows/build-diabo.yml`).
2. Let Gradle sync — sora-editor resolves from JitPack (already in `settings.gradle.kts`).
3. Run on a device/emulator with **API 24+**.

`gradlew` is already `chmod +x`'d, so the recurring "Permission denied" CI error
from earlier DiaBo/JavaGhor work won't resurface here.
