# DiaBo — Product Requirements Document (PRD)
### Native Android Java/XML IDE with GitHub Actions Cloud Build
**Version:** 2.0 | **Owner:** Tanzir | **Build Method:** GitHub Actions (Gradle Cloud Build)

---

## 1. Vision

DiaBo is a **native Android app** that lets a developer write Java + XML Android code **directly on their phone**, see an **instant approximate preview** as they type, and trigger a **real cloud APK build** (via GitHub Actions) to get a 100% accurate emulator screenshot and a downloadable, installable APK — all without needing a laptop or Android Studio.

**Core Promise:** *Code anywhere. Preview instantly. Build for real. Zero bugs.*

---

## 2. Goals & Success Criteria

1. User can create, edit, and manage multiple Android projects entirely on-device.
2. User gets a live/instant layout preview within milliseconds of editing XML.
3. User can trigger a real Gradle build via GitHub Actions and receive a screenshot + installable APK within ~5 minutes.
4. App must be **stable, crash-free, and predictable** — every feature must handle edge cases (empty files, invalid XML, no internet, GitHub API failure, storage full, etc.) gracefully.
5. UI must feel premium — glassmorphism, nature-inspired palette, smooth motion, bilingual (English/Bangla) friendly.

---

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Toolkit | Jetpack Compose + Material 3 |
| Local DB | Room (SQLite) |
| Preferences | DataStore |
| Background work | WorkManager (build polling, auto-save) |
| Networking | Retrofit + OkHttp (GitHub REST API) |
| File I/O | Kotlin `java.io.File` / Scoped Storage APIs |
| XML Instant Render | `LayoutInflater` + dynamic `ViewGroup` inflation from parsed XML string |
| Java Logic Simulation | Lightweight sandboxed interpreter (custom mini-parser for supported statements) or **Rhino/BeanShell-style embedded interpreter** for supported subset |
| Code Editor Core | Custom `EditText`/Compose-based editor OR embedded **CodeEditor (rosemoe/sora-editor)** library — recommended for syntax highlighting, auto-complete, line numbers out of the box |
| Cloud Build | GitHub Actions (`workflow_dispatch`) + GitHub REST API (Octokit-equivalent via Retrofit) |
| Crash Reporting | Firebase Crashlytics (or self-hosted alternative) |
| Dependency Injection | Hilt |
| Image/Screenshot handling | Coil (image loading for build screenshots) |
| Zip handling | Apache Commons Compress / java.util.zip |

---

## 4. Storage Architecture

**Root folder (Internal Storage, app-scoped, survives app restarts):**

```
/Android/data/com.tanzir.diabo/files/DiaBo/
│
├── Projects/
│   ├── MyFirstApp/
│   │   ├── src/
│   │   │   └── MainActivity.java
│   │   ├── res/
│   │   │   └── layout/
│   │   │       └── activity_main.xml
│   │   ├── project.json          (metadata: name, created date, last modified, icon, build history)
│   │   └── .diabo_cache/         (instant-preview render cache, thumbnails)
│   │
│   └── SecondApp/...
│
├── Templates/                     (starter templates: Blank, Login UI, List View, Bottom Nav, etc.)
├── Backups/                       (JSON export/import, .zip project exports)
├── BuildCache/                    (downloaded APKs & screenshots from GitHub Actions runs)
└── Logs/                          (build logs, crash logs, debug logs)
```

- Every project = self-contained folder → easy to **export as ZIP**, **share**, or **duplicate**.
- `project.json` acts as the single source of truth for Room DB sync (Room indexes this folder; folder is portable/rebuildable even if DB is cleared).

---

## 5. Required Android Permissions

| Permission | Purpose | Type |
|---|---|---|
| `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*` (API 33+ scoped) | Import files/folders from device storage into a project | Runtime |
| `MANAGE_EXTERNAL_STORAGE` (optional, only if deep import needed) | Full folder import (e.g. importing an existing Android project tree) | Runtime, special |
| `INTERNET` | GitHub API calls, triggering builds, downloading APK/screenshots | Normal |
| `ACCESS_NETWORK_STATE` | Detect connectivity before attempting cloud build; show offline banner | Normal |
| `REQUEST_INSTALL_PACKAGES` | Allow installing the built APK directly from in-app download | Runtime, special |
| `POST_NOTIFICATIONS` (API 33+) | Notify when a background build completes | Runtime |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` | Keep build-polling alive reliably in background | Normal |
| `WRITE_EXTERNAL_STORAGE` (API ≤28 only, legacy) | Legacy file export support | Runtime |
| `VIBRATE` | Haptic feedback on key actions (build success/fail, errors) | Normal |
| `WAKE_LOCK` | Prevent doze mode interrupting an in-progress cloud build poll | Normal |

**Permission UX rule:** Every permission is requested **contextually** (only when the relevant feature is first used), never all at app launch — with a clear rationale dialog beforehand (Material 3 style), matching DiaBo's polished-UI goal.

---

## 6. App Information Architecture (Pages)

1. **Splash / Onboarding** *(first-launch only)*
2. **Home**
3. **Project List**
4. **Code IDE** (Sidebar + Editor + Preview Panel)
5. **Settings**
6. **Build History** *(new)*
7. **Template Gallery** *(new)*
8. **Project Details / Export** *(new)*
9. **Help & Docs** *(new)*
10. **About / Credits** *(new)*

---

## 7. Page-by-Page Feature Specification

### 7.1 Splash / Onboarding (5+ features)
1. Animated DiaBo logo splash (glassmorphism fade-in)
2. 3-slide walkthrough: "Code," "Preview Instantly," "Build for Real"
3. GitHub account connect (OAuth device-flow or PAT input) — skippable, required only before first real build
4. Storage permission primer screen with rationale
5. "Skip" and "Get Started" CTA with smooth page-indicator animation
6. Auto-detect returning user → skip onboarding entirely

### 7.2 Home Screen (5+ features)
1. **FAB "+ New Project"** → bottom-sheet dialog: project name, package name auto-suggested, template picker (Blank/List/Login/etc.)
2. Recent Projects horizontal carousel (last 5 opened, with thumbnail of last preview)
3. Quick stats card: total projects, total builds this month, last build status
4. Quick-access shortcuts: "Import Project," "Open Templates," "View Build History"
5. Search bar (search across all projects by name)
6. Pull-to-refresh syncs Room DB with actual folder state (self-healing if folder edited externally)
7. Empty-state illustration + CTA when no projects exist yet

### 7.3 Project List Screen (5+ features)
1. Grid/List view toggle
2. Sort by: Name / Last Modified / Created Date / Last Build Status
3. Swipe actions: Rename, Duplicate, Delete (with confirm dialog + undo snackbar)
4. Long-press multi-select → bulk delete/export
5. Per-project context menu: Open, Export ZIP, Share, Rename, Delete, Duplicate, View Build History
6. Filter chips: "Has Builds" / "Never Built" / "Build Failed"
7. Project card shows: name, last modified, mini thumbnail (last instant preview snapshot), build status badge (✅/❌/⏳/—)

### 7.4 Code IDE Screen — Overview
Top bar: `[☰ Files] [Project Name ▾] [⚡ Instant] [▶ Real Build] [⋮ More]`

#### 7.4.1 File/Folder Explorer Sidebar (10+ features)
1. Collapsible file tree (folders expand/collapse with animation)
2. `+ New File` (auto-suggests `.java`/`.xml` based on current folder: `src/` vs `res/layout/`)
3. `+ New Folder`
4. **Import** — pick file(s)/folder(s) from device storage (via `ACTION_OPEN_DOCUMENT_TREE`)
5. Search-within-files (filename + in-content search, jump-to-result)
6. Rename (inline edit, validates against illegal filename chars)
7. Delete (with confirmation + soft-trash: recoverable for 24h before permanent delete)
8. Copy / Cut / Paste files & folders
9. Duplicate file (`MainActivity.java` → `MainActivity_copy.java`)
10. File-type icons (distinct icons: `.java`, `.xml`, `.gradle`, folder-open/closed states)
11. Drag-and-drop reordering / move-into-folder
12. Multi-select mode (checkbox overlay) → bulk delete/move/export
13. Pin/Favorite frequently-used files to top of tree
14. Right-click / long-press context menu (all actions above, one place)
15. File size + last-modified timestamp on long-press info
16. Git-style modified indicator (dot/color) for files changed since last build

#### 7.4.2 Code Editor (10+ features)
1. Syntax highlighting (Java + XML grammars, theme-aware)
2. Line numbers + current-line highlight
3. Auto-indent + smart bracket/tag matching (`{}`, `<tag></tag>`)
4. Auto-complete (Java keywords, common Android XML attributes/tags, snippet suggestions)
5. Find & Replace (regex support, replace-all, match count)
6. Undo/Redo (multi-level history stack, per-file)
7. Multi-tab editing (open several files at once, unsaved-change dot indicator per tab)
8. Font size pinch-to-zoom + settings control
9. Theme selector (Dark, Light, Solarized, custom accent colors)
10. Real-time basic lint (unclosed tag, mismatched brackets, missing semicolon — underlined in red/yellow)
11. Auto-save (debounced, every few seconds + on tab-switch/background)
12. Code folding (collapse method bodies / XML nested tags)
13. Word wrap toggle
14. Pinch-zoom + horizontal scroll for long lines
15. "Go to line" quick-jump
16. Keyboard toolbar (quick-insert: `{ } ; < > / " tab`) for faster mobile typing

#### 7.4.3 Preview Panel (5+ features)
1. **⚡ Instant Preview** — live, debounced (300–500ms after typing pause), renders real inflated Android Views from parsed XML
2. **▶ Real Build** — triggers GitHub Actions workflow; shows progress states: `Queued → Building → Installing → Capturing → Done`
3. Split-screen or bottom-sheet toggle (editor+preview side-by-side on tablets/landscape, sheet on phones)
4. Preview zoom/pan + device-frame chooser (Pixel/Small/Tablet aspect ratios) for instant preview
5. Real Build tab shows: emulator screenshot, build log (scrollable, collapsible errors), APK download button, "Install Now" button (via `REQUEST_INSTALL_PACKAGES`)
6. Error state UI — if instant-render fails (invalid XML), show inline friendly error instead of crash/blank screen
7. Build history mini-timeline within panel (last 5 builds for this project, tap to revisit screenshot)

### 7.5 Settings Screen (5+ features)
1. Editor preferences: font family/size, tab size (2/4 spaces), theme, keyboard toolbar toggle
2. GitHub integration: connect/disconnect account, PAT management (encrypted via `EncryptedSharedPreferences`), default repo/branch config
3. Build preferences: default emulator API level, auto-delete old build cache after N days, Wi-Fi-only build toggle
4. Storage management: view DiaBo folder size, clear cache, clear soft-trash
5. Backup & Restore: export all projects as JSON/ZIP, import from backup
6. App appearance: light/dark/system theme, accent color (nature-inspired palette picker)
7. Language toggle (English/Bangla UI strings)
8. Notification preferences (build-complete alerts on/off)

### 7.6 Build History Screen *(new page, 5+ features)*
1. Chronological list of all builds across all projects
2. Filter by project / status (success/fail/in-progress)
3. Tap entry → full build log viewer (syntax-highlighted log output)
4. Re-download APK or screenshot from any past successful build
5. Retry-failed-build shortcut (re-triggers workflow with same code snapshot)
6. Delete old build records (frees BuildCache storage)

### 7.7 Template Gallery *(new page, 5+ features)*
1. Curated starter templates: Blank Activity, Login Screen, List/RecyclerView, Bottom Navigation, Settings Screen, Form with Validation
2. Preview thumbnail per template
3. "Use Template" → pre-fills new project with template's Java+XML
4. Community/custom templates — user can save any project as a personal template
5. Search/filter templates by category (UI pattern type)

### 7.8 Project Details / Export *(new page, 5+ features)*
1. Project metadata view: created date, last modified, total files, total builds
2. Export as ZIP (full project folder)
3. Export as installable APK (latest successful build)
4. Share project (ZIP via Android share sheet)
5. Rename / change package name
6. Danger zone: delete project permanently

### 7.9 Help & Docs *(new page, 5+ features)*
1. Getting-started guide (in-app, bilingual)
2. XML tag reference (supported tags/attributes for instant preview)
3. Java subset reference (what's supported in sandboxed simulation)
4. FAQ (build failures, permission issues, GitHub token setup walkthrough)
5. Contact/feedback form (bug report shortcut, pre-fills device+app version info)

### 7.10 About / Credits
1. App version, changelog
2. Open-source licenses used
3. Developer credit (Tanzir)
4. Links: GitHub repo, feedback

---

## 8. Dual Preview System — Detailed Spec

### 8.1 Instant Preview (Primary, Default)
- **Trigger:** Debounced auto-render on every XML edit pause (~400ms), plus manual "⚡ Refresh" button.
- **Mechanism:**
  1. Parse XML string → validate well-formed (balanced tags, valid attribute syntax).
  2. Map supported tags (`LinearLayout`, `RelativeLayout`, `ConstraintLayout`, `TextView`, `Button`, `ImageView`, `EditText`, `ScrollView`, `RecyclerView`-stub, `CardView`) to real Android View objects via `LayoutInflater.inflate()` on a dynamically-built XML resource (or programmatic View construction if resource compilation isn't feasible at runtime).
  3. Render into an off-screen `ViewGroup` inside a sandboxed `Fragment`/Compose `AndroidView` wrapper.
  4. Java logic: only a **safe supported subset** is simulated (variable declarations, simple `onClick` listeners that mutate visible view properties, `Toast`/basic control flow). Unsupported constructs show a non-blocking "⚠ Not simulated in Instant Preview — Real Build supported" badge.
- **Failure handling:** Any parse/inflate exception is caught → friendly inline error panel with line number reference, **never a crash**.
- **Performance:** Must render within <300ms for typical layouts; render on background thread, publish to UI via `StateFlow`.

### 8.2 Real Build (Secondary, On-Demand)
- **Trigger:** User taps "▶ Real Build" explicitly (cost/time-aware — not automatic).
- **Flow:**
  1. App bundles current project's `MainActivity.java` + `activity_main.xml` + any additional files.
  2. Calls GitHub REST API → `workflow_dispatch` on a pre-configured template repo, passing code as base64 inputs + unique `build_id`.
  3. WorkManager background worker polls `GET /repos/{owner}/{repo}/actions/runs` every 15–20s (respecting GitHub API rate limits) until run completes.
  4. On success: downloads `apk` and `screenshot.png` artifacts via `GET /repos/{owner}/{repo}/actions/artifacts/{id}/zip`.
  5. Displays screenshot + build log + "Download APK" + "Install Now" in Preview Panel and saves to `BuildCache/`.
  6. Push local notification when build completes (if app is backgrounded).
- **Failure handling:** Distinguish between (a) network failure, (b) GitHub auth failure, (c) Gradle compile error, (d) emulator/runtime crash — each with a distinct, actionable error message (not a generic "build failed").
- **Rate/Cost safety:** Per-user cooldown (e.g., 1 real build per 2 minutes) to avoid GitHub Actions quota exhaustion; clear UI indicator of remaining quota if using shared/free-tier Actions minutes.

---

## 9. GitHub Actions Build Pipeline (Backend Repo Side)

**Template repo structure:**
```
android-template/
├── app/
│   ├── build.gradle
│   └── src/main/{java,res/layout,AndroidManifest.xml}
├── .github/workflows/build-preview.yml
├── build.gradle
├── settings.gradle
└── gradlew
```

**Workflow responsibilities:**
1. Accept `workflow_dispatch` inputs: `java_code` (base64), `xml_code` (base64), `build_id`.
2. Inject code into template project.
3. `./gradlew assembleDebug`.
4. Upload APK as artifact tagged `apk-{build_id}`.
5. Boot emulator (`reactivecircus/android-emulator-runner`), install APK, launch activity, `adb exec-out screencap`.
6. Upload screenshot artifact tagged `screenshot-{build_id}`.
7. Fail gracefully with clear logs on compile error (so app can surface a readable message).

**Security hardening:**
- Sanitize/validate incoming code server-side before injection (block shell-breakout patterns, oversized payloads).
- No secrets/tokens ever embedded in injected code path.
- Timeout every job (e.g., 10 min max) to avoid runaway/abuse jobs.

---

## 10. UI/UX Design Direction

- **Style:** Glassmorphism cards, frosted blur app bars, soft drop shadows, subtle depth layering.
- **Palette:** Nature-inspired — deep forest green / moss / soft sand / sky-blue accents, adapting to dark & light themes.
- **Typography:** Clean geometric sans for UI chrome; monospace (e.g., JetBrains Mono / Fira Code) for the code editor.
- **Motion:** Shared-element transitions (project card → IDE screen), smooth FAB morph into dialog, skeleton loaders (not blank spinners) during builds.
- **Bilingual:** All UI strings available in English + Bangla via `strings.xml` locale resources; user-toggleable regardless of system locale.
- **Accessibility:** Minimum touch target 48dp, sufficient contrast in both themes, TalkBack labels on icon-only buttons.

---

## 11. Reliability & "Zero Bugs" Engineering Standards

Since flawless operation is a stated top priority, DiaBo's engineering process must include:

1. **Defensive parsing everywhere** — XML/Java parsing wrapped in try/catch with typed error results (sealed classes), never raw exceptions surfaced to UI.
2. **Automated tests:**
   - Unit tests for XML parser, file-tree operations, Room DAOs.
   - UI tests (Compose testing) for critical flows: create project → edit → instant preview → real build trigger.
   - Instrumented tests for permission flows on multiple API levels (24–34).
3. **State management discipline:** Single source of truth per screen (`ViewModel` + `StateFlow`/`UiState` sealed classes) — no ad-hoc mutable state causing UI desync.
4. **Crash reporting + remote logging** (Crashlytics) from day one of internal testing, monitored before every release.
5. **Graceful degradation:** No internet → Instant Preview still fully functional; only Real Build disabled with clear messaging.
6. **Data integrity:** Auto-save + periodic Room↔folder sync verification; never lose user's code (write-to-temp-then-atomic-rename pattern for file saves).
7. **Storage-full / permission-denied handling:** Explicit UI states, not silent failures.
8. **Version-safe migrations:** Room DB migrations tested for every schema change; project folder format versioned (`project.json` has a `schemaVersion` field) for forward compatibility.
9. **Staged rollout:** Internal testing → closed beta → production, using Play Console staged rollout percentages, watching crash-free-users metric (target 99.5%+) before widening.
10. **CI for the app itself:** Every PR runs lint + unit tests + build via GitHub Actions before merge (dogfooding DiaBo's own build philosophy).

---

## 12. Data Models (Room Entities — simplified)

```kotlin
@Entity data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val packageName: String,
    val folderPath: String,
    val createdAt: Long,
    val lastModified: Long,
    val lastBuildStatus: BuildStatus, // NONE, SUCCESS, FAILED, IN_PROGRESS
    val thumbnailPath: String?
)

@Entity data class ProjectFile(
    @PrimaryKey val id: String,
    val projectId: String,
    val relativePath: String,
    val type: FileType, // JAVA, XML, GRADLE, OTHER
    val isPinned: Boolean,
    val lastModified: Long
)

@Entity data class BuildRecord(
    @PrimaryKey val buildId: String,
    val projectId: String,
    val status: BuildStatus,
    val triggeredAt: Long,
    val completedAt: Long?,
    val apkPath: String?,
    val screenshotPath: String?,
    val logSummary: String?
)
```

---

## 13. Phased Delivery Roadmap

| Phase | Scope |
|---|---|
| **Phase 1 — Core Local IDE** | Home, Project List, File Explorer, Code Editor, local save/load, no preview yet |
| **Phase 2 — Instant Preview** | XML→View renderer, basic Java simulation, preview panel |
| **Phase 3 — Cloud Build** | GitHub integration, Actions workflow, build polling, screenshot/APK retrieval, Build History |
| **Phase 4 — Polish** | Templates gallery, backup/restore, bilingual strings, glassmorphism UI pass, accessibility audit |
| **Phase 5 — Hardening** | Full test suite, Crashlytics integration, staged rollout, performance profiling |

---

## 14. Open Decisions (to confirm before build starts)

1. Code editor: build custom vs integrate `sora-editor` library (recommended: integrate, saves significant time and reduces bug surface).
2. GitHub template repo — dedicated public repo under Tanzir's account vs private with PAT-scoped access.
3. Emulator API level default (recommend API 30 for `android-emulator-runner` stability).
4. Whether Real Build cooldown/quota applies per-device or requires a lightweight backend (Render.com, consistent with existing infra) to centrally manage GitHub Actions quota across users if this becomes multi-user.
