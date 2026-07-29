# Habit Tracker

<p align="center">
  <img src="https://img.shields.io/badge/Android-Java%20%2B%20Material%203-blue" alt="Android" />
  <img src="https://img.shields.io/badge/Architecture-Room%20%2B%20Retrofit-green" alt="Architecture" />
  <img src="https://img.shields.io/badge/Sync-Supabase-orange" alt="Supabase" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License" />
</p>

<p align="center">
  <strong>Build better habits with a calm, adaptive, and locally resilient tracker.</strong>
</p>

Habit Tracker is an Android habit-building app designed to help users create habits, track daily progress, and improve over time with adaptive difficulty and streak-based feedback.

It combines a local-first experience with optional cloud sync through Supabase, so users can keep their data available across devices while still working offline.

## ✨ Features

### For users
- Create and manage habits with custom names, icons, colors, and cadence
- Check in daily and record how the day went with easy, okay, or hard effort
- See streaks, progress, and difficulty changes over time
- Receive reminders to keep habits visible and consistent
- Sign in to back up progress and sync data with Supabase

### For developers
- Built with Android Java and Material 3
- Uses Room for local storage and offline-first behavior
- Uses Retrofit + WorkManager for Supabase sync and background processing
- Includes an adaptive difficulty engine and reminder logic
- Structured around clear layers for UI, domain logic, and data access

## 🖼️ Screenshots

<div align="center">
  <img src="images/screenshot-home.svg" alt="Home screen preview" width="250" />
  <img src="images/screenshot-create-habit.svg" alt="Create habit preview" width="250" />
  <img src="images/screenshot-details.svg" alt="Habit details preview" width="250" />
</div>

> These images are currently lightweight SVG placeholders and can be replaced with real app screenshots later.

## 🛠️ Tech stack
- Android app built with Java
- Gradle + Android Gradle Plugin
- Room Database
- Retrofit / OkHttp for API calls
- WorkManager for background sync
- Security Crypto for secure token storage
- Material 3 UI components
- Supabase for auth, REST API, and database sync

## 🧱 Project structure
- app/src/main/java/com/habittracker/app
  - data: repositories, Room DAOs, remote API clients, sync logic
  - domain: models and algorithmic logic such as difficulty calculation
  - ui: activities, fragments, adapters, and custom views
- app/src/main/res: layouts, drawables, strings, navigation, and themes
- supabase: database migrations and server-side function configuration

## ✅ Requirements
- Android Studio
- JDK 17
- Android SDK API 34
- A working Android emulator or physical device

## ▶️ Getting started

### 1. Clone the repository
```bash
git clone <your-repo-url>
cd "Habit Tracker"
```

### 2. Open the project in Android Studio
- Open the folder as an Android project
- Let Gradle sync complete
- Ensure the Android SDK for API 34 is installed

### 3. Build the app
On Windows:
```bash
gradlew.bat assembleDebug
```

On macOS/Linux:
```bash
./gradlew assembleDebug
```

### 4. Run the app
- Select a device or emulator
- Run the app from Android Studio or use:

```bash
gradlew.bat installDebug
```

## ☁️ Supabase setup
The app is wired to use Supabase for authentication and cloud sync.

Important notes:
- The project already includes a Supabase configuration file in supabase/config.toml
- The Android app uses build configuration values in app/build.gradle for the Supabase URL and anon key
- For a real deployment or personal fork, replace these values with your own Supabase project settings

## 🗺️ Feature roadmap
- [x] Habit creation and editing
- [x] Daily check-in flow
- [x] Streaks and difficulty feedback
- [x] Local persistence with Room
- [x] Supabase sync foundation
- [ ] Better analytics and insights dashboard
- [ ] More advanced reminders and streak recovery
- [ ] Cloud backup/export improvements
- [ ] UI polish and theming upgrades

## 🤝 Contributing
Contributions are welcome.

1. Fork the repository
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-improvement
   ```
3. Make your changes and test them locally
4. Open a pull request with a clear summary of the change

Please keep changes focused, document new behavior clearly, and follow the existing project structure.

## 🧪 Development notes
### Local-first architecture
The app stores habits and logs locally first, then syncs updates to Supabase in the background.

### Adaptive difficulty
The difficulty engine adjusts habit challenge levels based on recent completion patterns and effort feedback.

### Notifications
The app includes reminder support and boot-time notification scheduling for habit follow-ups.

## 🔧 Common development tasks
- Add or update UI screens under app/src/main/java/com/habittracker/app/ui
- Add or change data models in app/src/main/java/com/habittracker/app/domain/model
- Update persistence logic in the Room entities and DAOs under app/src/main/java/com/habittracker/app/data
- Adjust Supabase API behavior in app/src/main/java/com/habittracker/app/data/remote

## 🧰 Troubleshooting
- If Gradle sync fails, make sure JDK 17 is selected in Android Studio
- If the app cannot reach Supabase, verify your network connection and Supabase config values
- If notifications do not appear, confirm the app has notification permissions on the device

## 📄 License
This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
