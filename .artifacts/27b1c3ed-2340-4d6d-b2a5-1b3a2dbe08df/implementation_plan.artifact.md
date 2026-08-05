# Setup MVVM Architecture for Zamnia Quiz App

Is plan ka maqsad project mein MVVM (Model-View-ViewModel) architecture setup karna hai taake code organized aur scalable rahe.

## Proposed Changes

### Dependencies Layer

#### [MODIFY] [libs.versions.toml](file:///C:/Users/hamza/AndroidStudioProjects/Zamnia/gradle/libs.versions.toml)
- `androidx-lifecycle-viewmodel-compose` dependency add karenge taake Compose mein ViewModels use ho sakein.
- `androidx-navigation-compose` add karenge future navigation ke liye.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/hamza/AndroidStudioProjects/Zamnia/app/build.gradle.kts)
- Nayi dependencies ko implementation mein shamil karenge.

---

### Data Layer (Model)

#### [NEW] [Question.kt](file:///C:/Users/hamza/AndroidStudioProjects/Zamnia/app/src/main/java/com/zamnia/quizapp/data/model/Question.kt)
- Quiz ke sawalat ke liye aik simple data class: `Question(id, text, options, correctAnswerIndex)`.

---

### UI Layer (View & ViewModel)

#### [NEW] [QuizViewModel.kt](file:///C:/Users/hamza/AndroidStudioProjects/Zamnia/app/src/main/java/com/zamnia/quizapp/ui/QuizViewModel.kt)
- `QuizViewModel` jo state manage karega (current question, score, etc.).

#### [MODIFY] [MainActivity.kt](file:///C:/Users/hamza/AndroidStudioProjects/Zamnia/app/src/main/java/com/zamnia/quizapp/MainActivity.kt)
- `MainActivity` ko update karenge taake wo `QuizViewModel` se data le aur UI update kare.

## Verification Plan

### Automated Tests
- `gradlew assembleDebug` run karke check karenge ke build pass ho rahi hai.

### Manual Verification
- App ko run karke dekhenge ke ViewModel se data correctly UI par display ho raha hai ya nahi.
