# Offline check

Compiles the whole app and runs its JVM unit tests on a machine that can't reach
Google Maven (for example a locked-down cloud container). Android Studio users
don't need this.

```bash
tools/offline-check/run.sh
```

How it works: the app's Kotlin sources are compiled against the real Android
framework API (Robolectric's `android-all` jar), JetBrains Compose for desktop, and the
real OpenCV and TFLite classes, all from Maven Central. Libraries that only exist on
Google Maven (Room, DataStore, CameraX, Activity, Lifecycle, Navigation) are replaced by
thin signature stubs in `stubs/`. It catches Kotlin errors, wrong imports and misuse of
the Android, Compose, OpenCV and TFLite APIs. `check_resources.py` also checks `strings.xml` for the mistakes aapt2 rejects. It does
**not** check Room's annotation processing, other resources or the manifest; only a real Gradle Android build does that.
