# Verification — 3 October 2026

- JDK 17, Gradle 9.3.1, Android SDK 36; Android API 37 emulator.
- `./gradlew assembleDebug testDebugUnitTest lintDebug`: successful.
- 5 unit tests passed, 0 failures/errors/skips.
- Lint: 0 errors, 10 warnings (newer dependency/Gradle versions, target API and older-platform backup configuration advice).
- Emulator: valid login, course list and details, lesson completion (Python 65% → 70%), and persisted progress after process restart with Wi-Fi and mobile data disabled.
- Emulator also verified mock API failure with cached content, empty state, and lesson completion persisted while offline.
- Recorded silent demo: 55.55 seconds, 720×1600 MP4. Shows login, courses, details, completion, network shutdown and offline restart.

The APK is signed with the standard debug key for evaluation, not for store release.
Authentication and the course API are mocked; there is no HTTP backend. No production tokens are stored.

## Publishing the prepared local repository

GitHub CLI authentication on this machine has expired. No remote repository was created.
After signing in, from this project directory run:

```sh
gh auth login
gh repo create learning-dashboard-android --private --source=. --remote=origin --push
```

Choose public visibility if your evaluator requires it. The APK, MP4, and PNG screenshots are tracked under `submission/` and will be included when you push.
The separate `.bundle` is a portable copy of the local Git repository; restore with:

```sh
git clone LearningDashboard.bundle LearningDashboard
```
