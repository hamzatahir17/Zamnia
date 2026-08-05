# Zamnia - Modern Android Quiz Application

Zamnia is a high-performance, feature-rich quiz application built with modern Android development standards. It offers an engaging experience where users can participate in quizzes, earn coins, manage a digital wallet, and customize their experience with themes.

## 🚀 Features

- **Dynamic Quiz Engine**: Interactive quiz sessions with 20 random questions per session, complete with a countdown timer and instant feedback.
- **Wallet & Economy**: A built-in coin system. Users earn coins for correct answers and can transfer coins to other users via Public IDs.
- **Content Sync (Offline First)**: Quiz packs are fetched from Supabase and stored locally in a Room database, allowing for a smooth experience even with intermittent connectivity.
- **Theming System**: Users can unlock and apply beautiful custom themes using their earned coins.
- **User Authentication**: Secure login via Supabase Auth, supporting both Google Sign-In and Anonymous Guest access.
- **Class-wise Content**: Educational content organized by class levels, subjects, and chapters.
- **Progress Tracking**: Tracks your quiz history and performance over time.

## 🛠 Tech Stack

- **UI**: Jetpack Compose (100% Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **Navigation**: Navigation Compose with Nested Graphs
- **Database**: 
    - **Remote**: Supabase (PostgreSQL, Realtime, Auth, Storage)
    - **Local**: Room Database (Offline caching)
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Dependency Management**: Gradle Version Catalog (libs.versions.toml)
- **Network**: Ktor Client & Supabase Kotlin SDK
- **Serialization**: Kotlinx Serialization

## 📦 Project Structure

- `ui/`: Contains all Compose screens and ViewModels organized by feature (auth, dashboard, quiz, wallet, settings).
- `data/`: Data layer handling local Room entities/DAOs and remote Supabase services.
- `domain/`: Business logic and use cases.
- `di/`: Dependency injection / Initialization logic via `ZamniaEngine`.

## ⚙️ Setup & Installation

To run this project, you need to provide your own Supabase and Google Auth credentials.

1. Clone the repository.
2. Create a `local.properties` file in the root directory if it doesn't exist.
3. Add the following keys to your `local.properties`:

```properties
SUPABASE_URL=your_supabase_project_url
SUPABASE_KEY=your_supabase_anon_key
GOOGLE_WEB_CLIENT_ID=your_google_web_client_id
```

4. Build and run the app.

## 🔒 Security Note
Sensitive data (API Keys, Client IDs) are managed through `BuildConfig` and `local.properties`. They are **never** committed to version control.

---
Built with ❤️ by [Your Name/Github Handle]
