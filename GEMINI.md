# Antigravity Project Guide: M Track Installer Android App

## 📱 Project Overview
- **Repository:** https://github.com/logicalfahim/mtrack_installer_app
- **Platform:** Android Mobile App (Min SDK 24, Target SDK 35)
- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture Repository Pattern
- **Networking:** Retrofit 2 + OkHttp 3 + Coroutines & Flow

---

## 🎨 UI & Design Rules
1. **Low Boldness (বোল্ডনেস কম):**
   - Strictly avoid overly heavy font weights (`FontWeight.Black`, `FontWeight.ExtraBold`).
   - Use `FontWeight.Normal`, `FontWeight.Medium`, and at most `FontWeight.SemiBold` for title headings.
2. **Brand Colors:**
   - Primary: `BrandBluePrimary` (`#2563EB`)
   - Surface / Background: `SurfaceBackground` (`#F8FAFC`)
   - Accent / Success: `EmeraldSuccess` (`#10B981`)
   - Deep Slate: `SlateNavyDark` (`#0F172A`)
3. **Sidebar / Drawer (`ui/components/AppDrawer.kt`):**
   - Always retain Brand header and the Static **About Us** screen link (`AboutUsScreen.kt`).
   - Support Helpline: `09638-988988`, WhatsApp: `+8801321119888`, Website: `mtrack.com.bd`.

---

## 🌐 Backend API Gateway Specification
- **Base URL:** `https://erp.mtrack.com.bd/` (Configured in `RetrofitClient.kt`)
- **Target Endpoint:** `POST /api/v1/install-requests`
- **Payload Schema:**
  - `full_name` (String, required)
  - `contact_number` (String, required)
  - `nid_number` (String, required)
  - `full_address` (String)
  - `install_location` (String, required)
  - `pref_date_time` (String)
  - `vehicles` (List<VehicleItem> - supports multi-vehicle fleet repeater)

---

## 🚀 CI/CD & Build
- **GitHub Actions:** `.github/workflows/android_build.yml` automatically triggers on push to `main` and compiles `app-debug.apk` in the cloud.
