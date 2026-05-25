# 🚗 Driver Monitor - Drowsiness Detection Android App

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg?logo=android)](https://developer.android.com/about/versions/nougat)
[![Google ML Kit](https://img.shields.io/badge/ML%20Kit-Face%20Detection-orange.svg)](https://developers.google.com/ml-kit/vision/face-detection)
[![Room](https://img.shields.io/badge/Room-SQLite-blue.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Driver Monitor** is a real-time, on-device Android application designed to detect driver drowsiness and prevent accidents. It processes live video streams from the front-facing camera, calculates the **Eye Aspect Ratio (EAR)** using **Google ML Kit Face Detection**, and alerts the user with an audible alarm if drowsiness is detected.

---

## ✨ Features

- **⚡ Real-Time Camera Analysis**: Uses **Jetpack CameraX** to run efficient on-device eye tracking.
- **🧠 Offline AI Processing**: Uses **Google ML Kit Face Detection** (Contour Mode) locally to locate eye contours without sending data to the cloud.
- **🚨 Audible Alarm**: Instantly plays a warning sound via **MediaPlayer** when eye closure threshold drops below `0.25`.
- **📈 Live Analytics**: Plots real-time EAR levels (line chart) and session distribution (pie chart) using **MPAndroidChart**.
- **💾 Local History Logs**: Saves detection events and statistics locally in a **Room SQLite database**.

---

## 🛠️ Tech Stack & Architecture

- **Pattern**: MVVM with reactive UI flow (Coroutines, StateFlow, ViewBinding).
- **Libraries**: CameraX, Room DB, Navigation Component, Google ML Kit, MPAndroidChart.
- **Requirements**: JDK 17, Android target SDK 34 (min SDK 24), physical device or emulator with camera.

---

## 📐 Detection Logic (EAR)

The application tracks the **Eye Aspect Ratio (EAR)** using 16 eyelid contour points:

$$EAR = \frac{||p_3 - p_{13}|| + ||p_5 - p_{11}||}{2 \cdot ||p_0 - p_8||}$$

- **EAR >= 0.25**: Eye is open (`AWAKE`).
- **EAR < 0.25**: Eye is closed (`DROWSY`) $\rightarrow$ Triggers warning alarm.
- **EAR == 0.0**: No face detected (`WAITING`).

---

## 🚀 Getting Started

### Installation & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_GITHUB_USERNAME/DrowsinessApp.git
   cd DrowsinessApp
   ```
2. Open the project in **Android Studio**.
3. Build the project using the IDE or CLI:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run the app on a connected device/emulator and grant **Camera Permission**.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.
