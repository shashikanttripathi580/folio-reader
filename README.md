# FolioReader — Lightweight Native Android PDF Book Reader

FolioReader is an ultra-lightweight, 100% native Android PDF book reader application written in **Kotlin** with **Jetpack Compose**. It brings a realistic, tactile, physical-book reading experience to digital screens while keeping the total installed app footprint under **8 MB**.

---

## 📱 Release APK Size & Delivery Metrics (Measured)

Measured directly from the optimized R8 release build (`app-release.apk`):

| Metric | Measured Value |
|---|---|
| **Release APK File Size** | **3,329,924 bytes (3.18 MB)** |
| **Installed Uncompressed Size** | **8,275,698 bytes (7.89 MB)** |
| **Google Play Delivery / Download Size** | **~3.16 MB** |
| **Third-Party Heavy Engines** | **0 MB** (No WebView, no Flutter, no Unity, no MuPDF) |

---

## 📖 Key Features

1. **Storage Access Framework (SAF) Integration**: Open any PDF file directly from on-device storage or SD card with persistent URI permissions.
2. **Realistic 3D Canvas Page-Turn Physics**:
   - Custom Canvas-rendered cylinder fold curvature with angle skewing based on finger drag coordinate.
   - Dynamic 3D specular highlight and drop shadow cast on revealed pages.
   - Verso (back-of-page) paper tint and illumination.
   - Outer stacked page bevels simulating 200+ physical leaves.
3. **Dual Facing Pages (Book Mode) & Single Page Mode**:
   - Automatically switches to two facing pages on tablets or in landscape orientation, complete with an authentic center spine gutter shadow.
   - Supports manual toggling between **Auto**, **Single Page**, and **Facing Pages**.
4. **Gesture & Tap Controls**:
   - Swipe or drag anywhere from outer edges/corners to curl the page in real-time.
   - Tap left 22% margin to turn backward; tap right 22% margin to turn forward.
   - Tap center 56% to smoothly toggle minimal reading toolbars.
5. **Memory-Safe LRU Bitmap Cache**:
   - Strictly memory-bounded LRU cache (6–8 pages) prevents `OutOfMemoryError` on large PDFs.
   - Native OS `android.graphics.pdf.PdfRenderer` renders pages directly at screen resolution.
   - Asynchronous coroutine prefetching for instant page turns.
6. **Jetpack DataStore Persistence**:
   - Automatically remembers the last-read page for each opened document.
   - Stores user preferences (theme, layout mode, recent books list).
7. **Warm Reading Themes**:
   - **Warm Paper**: Soft sepia background (`#F3EDE2`), cream pages (`#FDFBF7`), and warm charcoal ink (`#2C221E`).
   - **Daylight**: Crisp clean white background and dark graphite typography.
   - **Midnight**: True dark OLED background (`#0F0F0F`) for comfortable nighttime reading.
8. **Built-in Classical Sample Book**:
   - Includes an on-demand generated classic chapter of *"Alice's Adventures in Wonderland"* created at runtime with `android.graphics.pdf.PdfDocument` (0 assets bundled in APK).
9. **Completely Offline & Private**:
   - Zero permissions required beyond SAF.
   - No internet access, no tracking, no analytics, no ads, no accounts.

---

## 🛠 Project Structure

```
bookreader/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/folioreader/bookreader/
│       │   ├── MainActivity.kt
│       │   ├── data/
│       │   │   ├── PdfDocumentManager.kt     # PdfRenderer, LRU cache, sample book generator
│       │   │   └── ReaderPreferences.kt      # DataStore preferences & recent books
│       │   └── ui/
│       │       ├── BookReaderScreen.kt       # Reader screen orchestrator
│       │       ├── theme/
│       │       │   ├── Color.kt              # Warm paper, daylight, midnight palettes
│       │       │   └── Theme.kt              # CompositionLocal theme provider
│       │       └── components/
│       │           ├── PageCurlCanvas.kt     # Custom Canvas 3D page-curl animation engine
│       │           ├── ReaderTopBar.kt       # Collapsible header with layout/theme options
│       │           ├── ReaderBottomBar.kt    # Collapsible page scrubber & page badge
│       │           └── RecentBooksShelf.kt   # Bookshelf home screen with reading progress
│       └── res/
│           ├── values/{strings,colors,themes}.xml
│           └── drawable/ & mipmap-anydpi-v26/
├── release_apk/
│   └── BookReader-v1.0.0.apk
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── measure_apk.ps1
```

---

## 🚀 Building & Testing

### Assemble Release APK:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

### Install on Device via ADB:
```bash
adb install release_apk/BookReader-v1.0.0.apk
```
