# AGENTS Guide for BTTH1

## Scope and source of truth
- This repo currently has one Android module: `:app` (see `settings.gradle.kts`).
- Required glob scan found no existing AI instruction files (`README.md`, `AGENTS.md`, `.cursorrules`, etc.), so this file is the canonical agent guidance.

## Project shape (what exists today)
- This is an Android Views-era starter app with resources/tests but no app code yet in `app/src/main/java/com/example/btth1`.
- `AndroidManifest.xml` defines `<application ... />` with no `activity`, so there is currently no launcher entry point.
- UI/theme resources live in `app/src/main/res/values/` (`strings.xml`, `themes.xml`, `colors.xml`).
- Tests are template-based JUnit files:
  - Local JVM test: `app/src/test/java/com/example/btth1/ExampleUnitTest.java`
  - Device/instrumented test: `app/src/androidTest/java/com/example/btth1/ExampleInstrumentedTest.java`

## Build system and dependency conventions
- Use version catalog aliases from `gradle/libs.versions.toml`; do not hardcode library/plugin versions in module build files.
- Root `build.gradle.kts` only declares plugin aliases; module config belongs in `app/build.gradle.kts`.
- Repositories are centralized and locked by `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts`; add repos only there.
- Current stack:
  - AGP plugin `com.android.application` version `9.0.1`
  - Gradle wrapper `9.2.1` (`gradle/wrapper/gradle-wrapper.properties`)
  - Java toolchain metadata targets JDK 21 for daemon provisioning (`gradle/gradle-daemon-jvm.properties`)
  - Android compile options are Java 11 (`app/build.gradle.kts`)

## High-value workflows (verified)
- On Windows, use the wrapper from repo root:
  - `./gradlew.bat :app:tasks --all --console=plain` (verified)
- Common tasks for this project:
  - Build + checks: `./gradlew.bat :app:build`
  - Unit tests (host JVM): `./gradlew.bat :app:testDebugUnitTest`
  - Instrumented tests (device/emulator required): `./gradlew.bat :app:connectedDebugAndroidTest`
  - Lint: `./gradlew.bat :app:lintDebug`

## Coding and change patterns for agents
- Keep package namespace consistent: `com.example.btth1` (manifest, `namespace`, and test package assertions rely on it).
- When adding app code, place production classes under `app/src/main/java/com/example/btth1/` and wire new components in `AndroidManifest.xml` as needed.
- If adding dependencies/plugins, update `gradle/libs.versions.toml` first, then reference via `libs.*` aliases in `app/build.gradle.kts`.
- Preserve AndroidX/non-transitive R settings in `gradle.properties` unless a change is intentional and explained.
- Prefer minimal, template-consistent edits: this codebase is currently scaffold-level and intentionally simple.

## Integration points to watch
- External dependencies are currently limited to `appcompat`, `material`, `junit`, `androidx.test.ext:junit`, and `espresso-core` via catalog aliases.
- No backend/service integration, DI container, or persistence layer is present yet; any such addition should be documented in this file when introduced.

