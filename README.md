# Learning Dashboard — Android / Kotlin

**Run:** Open in Android Studio (AGP 9.1 support), use JDK 17 and SDK 36.
`./gradlew assembleDebug testDebugUnitTest lintDebug` builds and checks the app.
Install [`submission/apk/LearningDashboard-debug.apk`](submission/apk/LearningDashboard-debug.apk) on Android 8+.
[Demo video and screenshots](submission/README.md) are included in this repository.
**Login:** `learner@example.com` / `Learn123!`. Other credentials produce a mock error.

**1. Architecture.** Compose → lifecycle-aware StateFlow/ViewModels → repository →
mock API + local JSON store. A small manual application container injects dependencies;
interfaces isolate platform storage from business logic. Repository mutations are
serialized with a Mutex. Progress is derived from lesson completion, never stored as a
second conflicting value. Navigation keeps only a course ID. Mock login survives
rotation through its ViewModel; process death requires sign-in again.

**2. Offline.** A delayed mock API reads bundled JSON and checks Android connectivity.
It deliberately fails without a network, exercising the same cache fallback as a real
service. Previously fetched courses appear immediately, followed by refresh. An
AtomicFile in private internal storage persists course and lesson state before the UI
reports success. Stable lesson IDs preserve local completion when stale mock data
returns. Refresh errors retain visible cache and offer retry. Completion works offline,
including after restart. This is a single demo-user cache; real accounts require separate
caches and logout cleanup. The mock API is not an HTTP integration or sync engine.

**3. Security.** Production refresh tokens would be encrypted with an AES-GCM key in
Android Keystore; encrypted ciphertext lives in private app storage. Keep short-lived
access tokens in memory where practical, use TLS, redact credentials from logs, rotate
refresh tokens and clear account data on logout. Passwords are not persisted. Backup is
disabled here; mock authentication creates no real tokens.

**4. Scale.** (1) Server pagination + Paging 3 and Room indexed tables. (2) Incremental
sync with versioned/idempotent lesson writes and a WorkManager retry queue. (3) Proper
OAuth/OIDC sessions and account-scoped storage. (4) Crash reporting, structured redacted
logs, network metrics and performance profiling. (5) CI, broader repository/ViewModel/UI
tests and accessibility checks. Server capacity and CDN caching belong behind the API.

**5. Second platform.** SwiftUI views + @MainActor observable view models, async/await
repository protocols, URLSession API and SwiftData/Core Data for cache. Keychain stores
refresh tokens. Use NavigationStack, shared derived progress rules and XCTest for the
same offline/restart scenario; the same layers also work on macOS.

**Verification/demo.** Unit tests cover offline completion → restart → stale refresh,
failed disk writes, empty results and zero lessons. Debug-only **Demo** menu selects
normal/failure/empty API responses. For the offline demo: load courses, turn off Wi-Fi
and cellular, tap Refresh, open a course and complete a pending lesson, then restart and
sign in. Python starts at 65%; AI starts at 38% (6/16), intentionally derived from real
lesson counts rather than the sample's inconsistent 40%.
