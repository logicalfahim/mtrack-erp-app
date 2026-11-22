# M Track Vehicle Installer Android App

Enterprise Android Mobile Application built with **Kotlin** and **Jetpack Compose (Material 3)** for customer vehicle onboarding and GPS tracker installation requests. Powered by the **M TRACK Enterprise Dynamic REST API Gateway** (`/api/v1/install-requests`).

---

## 📱 Features

- **Modern & Lightweight UI:**
  - Designed with minimal/reduced font boldness (`FontWeight.Normal` and `FontWeight.Medium`) for an elegant, eye-friendly enterprise aesthetic.
  - Official M Track Brand Colors (Royal Blue `#2563EB`, Slate Dark `#0F172A`, Emerald Accent `#10B981`).
- **Sidebar (Navigation Drawer):**
  - Brand header with M Track logo, app version, and telematics subtitle.
  - Interactive menu items:
    - **New Installation Request**
    - **Static About Us** (Company profile, GPS services, hotline & website)
    - **Direct 24/7 Helpline Dialer** (`09638-988988`)
    - **WhatsApp Desk Dispatcher** (`+8801321119888`)
- **4-Step Progressive Installation Wizard:**
  - **Step 1: Customer Details:** Name, Contact Phone, Alternative Phone, NID Number, Profession, Billing Address.
  - **Step 2: Vehicles Fleet Repeater:** Dynamic list of vehicles (`+ Add Vehicle` / remove) with Registration Number, Vehicle Type, and GPS Device tier (Voice, Fuel, Dashcam).
  - **Step 3: Schedule & Location:** Target installation address/city, preferred date and time, agent reference.
  - **Step 4: Review & Live API Submission:** Instant validation and transmission to the backend API.
- **Success & Confirmation:**
  - Generates Tracking Batch ID.
  - 1-click WhatsApp confirmation dispatcher to official M Track support.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.0+
- **UI Toolkit:** Jetpack Compose (Material 3) + Navigation Compose
- **Architecture:** MVVM + Clean Repository Pattern
- **Networking:** Retrofit 2 + OkHttp 3 (with logging interceptor)
- **Serialization:** Google Gson
- **Async & Concurrency:** Kotlin Coroutines & StateFlow

---

## 🚀 How to Push this Project to GitHub

1. Open your terminal / command prompt in this directory (`mtrack_installer_app`):
   ```bash
   cd d:\Projects\mtrack_erp\mtrack_installer_app
   ```

2. Initialize Git and commit all files:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: M Track Installer Kotlin App with Compose & Sidebar"
   ```

3. Create a new repository on [GitHub](https://github.com/new) (e.g. `mtrack-installer-app`).

4. Link and push to your GitHub repository:
   ```bash
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/mtrack-installer-app.git
   git push -u origin main
   ```

---

## 💻 How to Open and Run in Android Studio

1. Open **Android Studio** (Ladybug / Koala / Hedgehog or newer).
2. Click **File -> Open...** and select the folder `mtrack_installer_app`.
3. Wait for Gradle Sync to complete.
4. Select an Android Emulator or physical device (Android 7.0+ / API 24+).
5. Click **Run (Shift + F10)**.

---

## 🌐 Configuring Backend API Gateway

Open `app/src/main/java/com/mtrack/installer/data/api/RetrofitClient.kt`:

```kotlin
// Change the URL based on your testing environment:
// 1. Live Production Server:
private const val DEFAULT_BASE_URL = "https://erp.mtrack.com.bd/"

// 2. Local Android Emulator testing your PC's local server:
// private const val DEFAULT_BASE_URL = "http://10.0.2.2/"

// 3. Physical Device connected to the same Wi-Fi:
// private const val DEFAULT_BASE_URL = "http://192.168.0.XXX/"
```

---

## 📦 Project Directory Structure

```
mtrack_installer_app/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mtrack/installer/
│       │   ├── MainActivity.kt               # Scaffold, Drawer & Router
│       │   ├── data/
│       │   │   ├── api/                      # MTrackApiService, RetrofitClient
│       │   │   ├── model/                    # InstallationRequestPayload, VehicleItem, ApiResponse
│       │   │   └── repository/               # InstallRepository
│       │   └── ui/
│       │       ├── components/               # AppDrawer (Sidebar with About Us)
│       │       ├── screens/                  # InstallWizardScreen, AboutUsScreen, SuccessScreen
│       │       ├── theme/                    # Color, Type (Low Boldness), Theme
│       │       └── viewmodel/                # InstallViewModel
│       └── res/
│           ├── values/                       # colors.xml, strings.xml, themes.xml
│           └── xml/network_security_config.xml
├── gradle/
│   └── libs.versions.toml                   # Gradle Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
└── README.md
```
