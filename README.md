# LocalAI — Run LLMs locally on Android

A native Android app for running large language models **fully offline** on your
phone via [llama.cpp](https://github.com/ggerganov/llama.cpp). It targets modest
devices (≈2–3 GB usable RAM) using 3–4 bit GGUF quantization, with a one-tap
**Smart Setup** that tunes everything to your device's *currently available*
resources — not just its raw specs.

> Status: scaffold / MVP. The full app architecture, UI, data layer, download
> manager and smart configurator are implemented. Real inference requires
> fetching the native backend (see [Native backend](#native-backend)); until
> then the app runs in a **simulation mode** so the whole UI is usable.

## Features

- 👋 **Guided first launch** — a welcome flow downloads a starter model in the
  background and requests the permissions the app + agent need. No computer and
  **no terminal** required (there isn't one on Android) — the app sets itself up.
- 🗂️ **Projects** — group related chats into projects (like folders), create new
  chats straight inside a project, or keep loose chats in the main list.
- ⏬ **Background downloads** — model downloads run on an app-scoped coordinator
  and a foreground service, so they keep going while you navigate around or
  background the app, with a progress notification.
- 🧠 **Smart Setup** — one button analyzes free RAM, CPU cores and device class,
  then picks the best model and tunes context window, threads, batch size and
  sampling. It deliberately leaves headroom for the OS and your other apps, and
  **explains every decision** so you can trust (and override) it.
- 🎚️ **Deep manual settings** — categorized, collapsible control over *every*
  llama.cpp knob: temperature, top-P, top-K, min-P, typical-P, repeat/frequency/
  presence penalties, repeat-last-N, Mirostat (mode/tau/eta), tail-free-Z, seed,
  context window, max tokens, threads, batch size, GPU layers, mmap/mlock/flash-
  attention, a default system prompt, stop sequences and streaming. Set app-wide
  defaults, override per-chat, or one-tap Smart Setup → **Apply to my defaults**.
- 🤖 **Agent mode** — let the AI act on your device: read/write/delete files,
  make folders, list & open apps, read device info, and run shell commands.
  Three safety levels — **Ask** / **Accept edits** / **Auto** — *plus* a
  per-action permission switch for each capability so you can disable any tool
  entirely. Permissions are requested only when you enable the agent, and
  destructive actions are flagged before they run.
- 💬 **Chat** — a visible **Chat ⇄ Agent** segmented switch, streaming
  responses, message timestamps + copy, adjustable font size, multiple saved
  sessions, per-session model and parameters, in-chat model switcher.
- 🎨 **Appearance** — light / dark / system theme, Material You dynamic colour,
  font scaling and a timestamp toggle.
- 📥 **Model manager** — curated, mobile-optimized models with resumable
  downloads, progress/speed, and on-device cache management.
- ⚙️ **Settings & system dashboard** — live RAM usage (total / available /
  in-use), CPU/arch info, backend status and cache size.

## Curated models

Four headline picks tuned for phones, plus optional larger downloads:

| Model | Params | Quant | Size | Min free RAM |
|-------|--------|-------|------|--------------|
| **Gemma 2 2B Instruct** (Google) | 2.6B | Q4_K_M | ~1.7 GB | ~1.8 GB |
| **Qwen2.5 1.5B Instruct** (Alibaba) | 1.5B | Q4_K_M | ~1.0 GB | ~1.3 GB |
| **Phi-3.5 Mini Instruct** (Microsoft) | 3.8B | Q4_K_M | ~2.4 GB | ~2.6 GB |
| **Mistral 7B Instruct v0.3** (Mistral AI) | 7.2B | Q4_K_M | ~4.4 GB | ~4.6 GB |

Plus ~14 more downloads spanning tiny to flagship: Qwen2.5 0.5B / 3B / 7B,
Llama 3.2 1B / 3B and Llama 3.1 8B, Gemma 3n E2B / E4B and Gemma 2 9B,
Phi-3 Mini 4K, SmolLM2 1.7B, TinyLlama 1.1B, StableLM 2 Zephyr 1.6B and
DeepSeek-R1 Distill Qwen 1.5B. See
[`ModelCatalog.kt`](app/src/main/java/com/expstudio/localai/data/model/ModelCatalog.kt).

## Architecture

MVVM + a small manual-DI container ([`LocalAiApp`](app/src/main/java/com/expstudio/localai/LocalAiApp.kt)).

```
ui/            Jetpack Compose screens + ViewModels + navigation
  screens/     Home (sessions), Chat, ModelManager, Settings
  vm/          ChatViewModel, ModelManagerViewModel, SettingsViewModel
data/
  db/          Room entities, DAOs, database
  model/       CatalogModel, ModelCatalog, InferenceParams
  repo/        ModelRepository, ChatRepository
download/      Resumable ModelDownloadManager + foreground DownloadService
inference/     LlamaInferenceEngine (Kotlin) + LlamaBridge (JNI)
smart/         DeviceProfile + SmartConfiguratorEngine (the Smart Setup brain)
cpp/           JNI bridge + CMake for the llama.cpp backend
```

## Building

Requires Android Studio (Koala+) or the Android SDK with the command-line tools.

```bash
# 1. Point Gradle at your SDK (or set ANDROID_HOME)
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 2. Build the debug APK
./gradlew assembleDebug
```

Open the project in Android Studio and run on a device/emulator for the full
experience. Minimum Android 8.0 (API 26).

## Native backend

> **End users never touch a terminal.** This section is for *developers* building
> the app from source who want real (non-simulated) inference compiled into the
> APK. On a phone you just install the APK and the in-app first-launch flow does
> the rest.

To enable real on-device inference, fetch llama.cpp into the native source tree
and rebuild:

```bash
./scripts/setup_native.sh        # clones llama.cpp into app/src/main/cpp/llama.cpp
./gradlew assembleDebug
```

The CMake build auto-detects the checkout: present → real backend, absent →
a lightweight JNI **stub** so the app still compiles and the UI works
(responses are clearly labelled as simulated). The model weights themselves are
never committed — they're downloaded in-app to app-scoped storage.

## How Smart Setup works

[`SmartConfiguratorEngine`](app/src/main/java/com/expstudio/localai/smart/SmartConfiguratorEngine.kt)
budgets from **available** RAM (via `ActivityManager.MemoryInfo`), reserves
headroom (more on low-RAM devices) so the OS/background apps stay healthy, then:

1. picks the largest model whose weights + a minimal context fit the budget
   (preferring already-installed models);
2. sizes the context window to leftover RAM after weights (KV-cache estimate);
3. leaves CPU cores for the UI/system and scales batch size to RAM;
4. estimates throughput and returns a human-readable rationale.
