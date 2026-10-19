# Muay Thai (`com.muaythai.arena`)

A Muay Thai training companion, fight simulator, fighter RPG and gym club app for Android, in red and white.
Kotlin, Jetpack Compose, Material 3, MVVM, Room and DataStore. Fully offline: no account, no backend, no internet permission.

## Build

Requirements: Android Studio Meerkat 2024.3.1 Patch 1 or newer, or JDK 17 plus the Android SDK (platform 36).

```bash
./gradlew testDebugUnitTest   # 42 engine tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew bundleRelease       # app/build/outputs/bundle/release/app-release.aab
```

In Android Studio: open the folder, let Gradle sync, then **Run**, or **Build > Generate Signed App Bundle / APK**.

### Signing the release bundle

`bundleRelease` works without a key and produces an unsigned `.aab`. To have Gradle sign it:

1. Create an upload key once:
   `keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. Copy `keystore.properties.example` to `keystore.properties` and fill in the absolute keystore path, passwords and alias.
   The file is git-ignored.

CI can sign as well: see the secrets listed in `.github/workflows/android.yml`.

| Setting | Value |
| --- | --- |
| applicationId / namespace | `com.muaythai.arena` |
| minSdk / targetSdk / compileSdk | 24 (Android 7.0) / 36 (Android 16) / 36 |
| Gradle / AGP / Kotlin / KSP | 8.11.1 / 8.9.1 / 2.0.21 / 2.0.21-1.0.28 |
| Compose BOM / Room / DataStore | 2024.12.01 / 2.6.1 / 1.1.1 |

## Verification status

This project was written in an environment with no Android SDK and no access to Google's Maven repository,
so it has **not** been built with the Android Gradle Plugin there. What was checked:

- `domain/` (models, content, all engines) was compiled with the Kotlin 2.0.21 compiler and the 42 unit tests in
  `app/src/test` pass. Thousands of simulated fights were run to balance the Fight Lab.
- The data, view-model and Compose layers were type-checked with the same compiler against hand-written stand-ins for
  the Android, Compose, Room, DataStore, Lifecycle and Navigation APIs. That catches mistakes inside the app's own code,
  but it cannot catch a wrong assumption about a library signature, a Room annotation-processing error, a resource or
  manifest problem, or anything that only shows at runtime.

Expect the first real Gradle build to be the true test. If it reports an error, it is most likely a library API detail in `ui/`.

## Club and moderator panel

The Club area (Home > Club, or Profile > Club) is the part of the app a gym uses day to day:

| Member | Moderator (PIN-protected panel) |
| --- | --- |
| Browse the schedule, sign up, join a waiting list, cancel | Create, edit, cancel and delete events |
| Check in on the day (logged as training, awards XP) | See booked and waiting lists, add walk-ins, mark attendance |
| Answer surveys (single choice, multiple choice, rating, text) for XP | Build surveys, open or close them, collect answers from other people on the device |
| See survey results after answering | Aggregated results per question, respondent list, plain-text export |
| Read the notice board | Post, pin, edit and delete notices |
| Send requests (private lesson, schedule, question...) and read replies | Requests inbox with status and reply |

The app has no backend, so member and moderator share one device: a coach's phone or a tablet at the front desk.
The moderator sets a 4-digit PIN on first entry; only its SHA-256 hash is stored, and the panel locks when the app closes.
The PIN keeps members out of the tools. It is not a security boundary against someone with access to the device's storage.

First launch seeds four surveys, three notices and three special events. The regular weekly timetable is generated two
weeks ahead on every start (switchable in the moderator panel), so the schedule is never empty.

## Theme

Red (`#D4102F`) and white are the main colours: white surfaces, red headers, buttons and accents. The light theme is the
default. Dark mode is still available in Settings and keeps the red headers.

## Architecture

```
app/src/main/java/com/muaythai/arena
├── domain/                  pure Kotlin, no Android imports
│   ├── model/               Fighter, GameState, FightState, Combination, ...
│   ├── content/             30 opponents, 59 fight scenarios, 42 defense, 32 clinch, 30 techniques,
│   │                        32 combinations, 16 programs
│   └── engine/              MuayThaiTrainingEngine, CombinationEngine, DefenseEngine, ReactionEngine, ClinchEngine,
│                            FightSimulationEngine, OpponentEngine, FightCampEngine, ProgressionEngine,
│                            AchievementEngine, CoachEngine, ChallengeEngine, StatsEngine, RecordEngine, GameReducer,
│                            ClubEngine (sign-ups, waiting lists, surveys, timetable, exports)
├── data/
│   ├── local/               Room: 19 entities, GameDao, ClubDao, ArenaDatabase, mappers
│   ├── prefs/               DataStore: theme, sound, vibration, reminders, onboarding flag
│   └── repository/          GameRepository and ClubRepository: one StateFlow each, transactional writes
├── reminder/                optional daily local notification
└── ui/
    ├── theme/  components/  palette, typography, cards, ring, rank badge, reward dialog
    ├── vm/                  view models (StateFlow in, events out)
    ├── nav/                 bottom navigation and NavHost
    └── screens/             Home, Train, Fight Lab, Progress, Profile, Club, Moderator and everything under them
```

How a write flows: a screen calls its view model; the view model hands a `SessionOutcome` to
`GameRepository.commit`; inside one Room transaction the repository loads a snapshot, `GameReducer.applySession`
returns the next `GameState` and a `RewardSummary` (XP, attribute gains, challenges, achievements, records, rank-up),
and only the changed rows are written. Every screen observes the same `StateFlow<GameState>`.

### Fight simulation in one paragraph

`FightSimulationEngine` is a pure state machine. Each round: the opponent's strategy is shown, you pick one of eight
strategies, three tactical situations are drawn to fit the state of the fight (stamina, damage, score, opponent
strategy), and each choice succeeds or fails based on its quality, the relevant attributes, the opponent's weaknesses
and your stamina. The round is then scored from attributes, the strategy match-up, stamina, damage, momentum and the
corner instruction, and three judges apply the ten-point must system. Zone damage of 100 ends the fight. The seed is
stored in the state, so tests can replay a fight exactly.

### Database version

The Room schema is at version 2 and the builder uses `fallbackToDestructiveMigration()`. Version 1 was never
published, so a development install of it is rebuilt (local data is wiped) instead of crashing. Before changing the
schema of a released build, replace that with real `Migration` objects.

## Content and privacy

All opponents, gyms and nicknames are fictional. No real fighters, logos or organisation branding are used.
The app declares three permissions, none of them network related: `VIBRATE`, `POST_NOTIFICATIONS` (only requested when
the reminder is switched on) and `RECEIVE_BOOT_COMPLETED` (to restore that reminder after a restart).
