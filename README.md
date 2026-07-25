<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="AlphaMusic" width="120" height="120"/>
  <h1>🎵 AlphaMusic</h1>
  <p>Streaming music has never been this seamless and smooth.</p>
  <p>
    <strong>Android</strong> · Material 3 · Jetpack Compose · Hilt · Room · ExoPlayer · SF Pro Fonts
  </p>
</div>

---

## ✨ Features

### 🎧 Music Playback
- **High Quality Audio** streaming with support for the highest available bitrates
- **Trending tracks**, curated playlists, and new releases right on the home screen
- **Search** across songs with debounced real-time results
- **Sleep Timer** — automatically pause playback after a set duration
- Download songs for **offline playback** with live progress tracking

### 📂 Library Management
- **Like** your favourite songs and access them instantly
- **Create & manage playlists** with drag-and-drop simplicity
- **Downloads tab** — all your offline music in one place
- Sort your library by name, date, or duration

### 🎨 Design
- **Material 3** design language with dynamic theming
- **Dark mode** always-on for an immersive listening experience
- **SF Pro Fonts** applied throughout for a premium Apple-inspired typography
- **Dominant color extraction** from album art for a personalised player background

### ⚡ Performance
- **Optimised scrolling** with stable list keys in all LazyColumns
- **Efficient state management** with `WhileSubscribed` Room observers
- **Memoised shuffled lists** — no more random UI reordering on recomposition
- **Minimised HTTP overhead** with basic-level OkHttp logging

---

## 📸 Screenshots

<table>
  <tr>
    <td><strong>Home</strong></td>
    <td><strong>Search</strong></td>
    <td><strong>Library</strong></td>
    <td><strong>Player</strong></td>
  </tr>
  <tr>
    <td><img src="screenshots/home.png" alt="Home Screen" width="200"/></td>
    <td><img src="screenshots/search.png" alt="Search Screen" width="200"/></td>
    <td><img src="screenshots/library.png" alt="Library Screen" width="200"/></td>
    <td><img src="screenshots/player.png" alt="Player Screen" width="200"/></td>
  </tr>
</table>

---

## 📦 Download

[![Download APK](https://img.shields.io/badge/Download-Latest_APK-green)](https://github.com/arghamuhury/AlphaMusic/releases/latest)

### Building from source

```bash
# Clone the repository
git clone https://github.com/arghamuhury/AlphaMusic.git

# Navigate to the project directory
cd AlphaMusic

# Build the debug APK
./gradlew assembleDebug

# The APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### ⚠️ Note
This app uses third-party music sources (JioSaavn) for streaming. Some features may depend on external API availability.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **UI** | Jetpack Compose, Material 3 |
| **Architecture** | MVVM with Hilt DI |
| **Database** | Room (SQLite) |
| **Media Playback** | ExoPlayer (Media3) |
| **Networking** | Retrofit + OkHttp |
| **Image Loading** | Coil |
| **Theme** | Dynamic dark theme, SF Pro typography |

---

## 👨‍💻 Author

**Argha Muhury**

- GitHub: [@arghamuhury](https://github.com/arghamuhury)

---

## 📄 License

```
Copyright (C) 2026 Argha Muhury

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
