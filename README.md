# NAS Project

NAS Project is a personal network-attached-storage client. Browse folders, upload and download files, and share them through public links — from Android, Desktop, or the browser. One Compose Multiplatform codebase drives all three.

**Backend**: [github.com/andreasmlbngaol/nas-project](https://github.com/andreasmlbngaol/nas-project) (Rust / axum) — runs separately; this repo is the client.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Modules](#modules)
- [Getting Started](#getting-started)
- [Port Reference](#port-reference)
- [Environment Variables](#environment-variables)
- [Screenshots](#screenshots)
- [License](#license)

---

## Features

- Email/password login and registration
- Persistent session across app restarts (session cookie, stored per platform)
- Folder browsing with breadcrumbs, list and grid layouts, and sorting
- File upload and download with the server-supplied filename
- Create, rename, move, and delete folders and files
- Public links: toggle a file or folder public, copy its share URL, and open it in a read-only viewer
- Material 3 Expressive theming with light/dark support

---

## Tech Stack

| Layer             | Technology                                                    |
|-------------------|---------------------------------------------------------------|
| UI                | Compose Multiplatform 1.11.0, Material 3 Expressive           |
| Language          | Kotlin (via Kotlin Toolchain 0.12.2)                          |
| HTTP              | Ktor Client 3.6.0                                             |
| Serialization     | kotlinx.serialization 1.11.0                                  |
| Coroutines        | kotlinx.coroutines 1.11.0                                     |
| Targets           | Android, Desktop (JVM), Web (Kotlin/Wasm)                     |
| Build             | Kotlin Toolchain (Amper), self-bootstrapping `./kotlin` wrapper |

---

## Architecture

```
Compose Multiplatform (shared UI + logic)
 ├── androidApp  → Android — Ktor OkHttp engine
 ├── desktopApp  → JVM (Swing window) — Ktor CIO engine
 └── webApp      → Kotlin/Wasm in the browser — Ktor JS engine
        │
        ↓  Ktor Client (REST, session cookie)
        │
   Backend (Rust / axum)  — http://localhost:3000
        └── PostgreSQL
```

### Platform Split

The `shared` module holds the Compose UI, controller, and API client. Only three
things differ per platform, each an `expect`/`actual` pair:

| Concern       | Android                          | Desktop (JVM)                  | Web (Wasm)                              |
|---------------|----------------------------------|--------------------------------|-----------------------------------------|
| Base URL      | `https://3000.booroong.online`   | `http://localhost:3000`        | `https://3000.booroong.online`          |
| Session store | SharedPreferences                | `~/.nas-project/session`       | `localStorage`                          |
| File picker   | `OpenDocument` / `CreateDocument`| AWT `FileDialog`               | `<input type="file">` / Blob download   |

> **Web auth note**: in a browser the session cookie lives in the browser's own
> jar — JS cannot read `Set-Cookie` or send a `Cookie` header. The web build uses
> fetch `credentials: 'include'` instead, which requires the backend to answer
> with `Access-Control-Allow-Credentials: true` and a `SameSite=None; Secure`
> cookie. Set `CORS_ALLOWED_ORIGINS` on the backend to the web app's origin.

---

## Modules

This is a Kotlin Multiplatform project with 4 modules:

| Module                      | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| [`androidApp/`](./androidApp) | Android application                                                       |
| [`desktopApp/`](./desktopApp) | Desktop (JVM) application                                                 |
| [`shared/`](./shared)         | Compose UI, controller, API client, and platform `expect`/`actual` pairs  |
| [`webApp/`](./webApp)         | Web application, compiled to WebAssembly with Kotlin/Wasm                 |

---

## Getting Started

### Prerequisites

- [JDK 17+](https://adoptium.net/)
- [Android SDK](https://developer.android.com/studio) (only for the Android target)
- A running [NAS Project backend](https://github.com/andreasmlbngaol/nas-project)

The `kotlin` (macOS/Linux) and `kotlin.bat` (Windows) scripts in the project root
are self-bootstrapping wrappers for the Kotlin Toolchain: they download the pinned
version on first use, so no separate installation is required.

### 1. Clone the repository

```bash
git clone https://github.com/andreasmlbngaol/nas-project.git
cd nas-project
```

### 2. Start the backend

The client needs the backend reachable. From the backend repo:

```bash
docker compose up --build
```

### 3. Run

Use the run widget in your IDE's toolbar, or run a target from the command line:

```bash
# Desktop (JVM) — talks to http://localhost:3000
./kotlin run -m desktopApp

# Web (Wasm) — served by the toolchain dev server
./kotlin run -m webApp

# Android — device or emulator
./kotlin run -m androidApp
```

> The first run downloads the toolchain and dependencies, so it may take a few
> minutes. Subsequent runs are faster.

### 4. Build

```bash
# Whole project
./kotlin build

# A single module
./kotlin build -m webApp
```

### 5. Test

```bash
./kotlin test
./kotlin test -m shared
```

---

## Port Reference

| Service         | URL                         | Notes                                                          |
|-----------------|-----------------------------|----------------------------------------------------------------|
| Backend API     | http://localhost:3000       | Rust/axum backend, run separately                              |
| Backend tunnel  | https://3000.booroong.online| Public HTTPS endpoint used by the Android and Web builds       |
| PostgreSQL      | `localhost:5433`            | Backend database (host-mapped to avoid clashing with a local install) |
| Web dev server  | *toolchain-assigned*        | Port chosen by `./kotlin run -m webApp`                        |

---

## Environment Variables

The client has **no `.env` file**. The backend base URL is compiled per platform in
`shared/src@<platform>/Platform.<platform>.kt` (the `expect` lives in
[`shared/src/data/Platform.kt`](./shared/src/data/Platform.kt)):

| Target        | File                                          | Default value                  |
|---------------|-----------------------------------------------|--------------------------------|
| Desktop (JVM) | `shared/src@jvm/Platform.jvm.kt`              | `http://localhost:3000`        |
| Android       | `shared/src@android/Platform.android.kt`      | `https://3000.booroong.online` |
| Web (Wasm)    | `shared/src@wasmJs/Platform.wasmJs.kt`        | `https://3000.booroong.online` |

To point a target at a different backend, edit that file's `defaultBaseUrl`.

---

## Screenshots

> _Screenshots coming soon._

---

## License

Licensed under the [Apache License 2.0](./LICENSE) © 2026 [andreasmlbngaol](https://github.com/andreasmlbngaol)
