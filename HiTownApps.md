# Hi Town Apps — Implementation Plan

## Overview

Hi Town Apps are a custom, plug-and-play UI system that runs identically on mobile
(Android) and Web. An **App** is a server-side Kotlin script (`.kts`) that emits a
cross-platform UI tree (`StoryContent`); the clients are thin, generic renderers.

Roughly **80% of the platform already exists** in the form of the **Scripts engine**.
"Apps" are a productized, installable, multi-screen layer on top of that engine. This
document is the concrete plan to get there.

## What Already Exists

### Universal UI contract — `StoryContent`
`shared/models/src/commonMain/kotlin/StoryContent.kt` defines the primitive set shared by
stories, cards, widgets, and scripts:

- `Section`, `Text`, `Photos`, `Audio`, `Video`, `Profiles`, `Groups`, `Cards`, `Scene`
- `Button(text, script, data, style, color, enabled)` — invokes another script run
- `Input(key, value, hint, inputType)` — text/photo input
- `Widget` — embeds richer components

Rendered by the same code path on each client:
- Android: `ui/story/StoryContents` (used by `ui/script/ScriptContent.kt`)
- Web: `app/scripts/ScriptsPage.kt`, `components/Content.kt`, `app/widget/ScriptWidget.kt`

### Script runtime (the App runtime)
`backend/src/main/kotlin/com/queatz/scripts/RunScript.kt` compiles & evaluates scripts in
a sandbox (`RestrictedClassLoader` blocks `java.io`, `java.nio`, `System.exit`, …) and
injects capabilities as provided properties (declared in
`backend/scripts/src/main/kotlin/com/queatz/scripts/Scripts.kt`):

- `render: ScriptRender` — emits `StoryContent` (the UI)
- `storage: ScriptStorage` — per-user DB: `Global`/`Local` scopes, typed collections,
  key/value, raw AQL (`ArangoScriptStorage(scriptOwner, person)`)
- `http: ScriptHttp` — GET/POST to any API (how apps currently reach AI providers)
- `secret: String?` — per-script secret (e.g. an OpenAI key) in `ScriptData.secret`
- `app: ScriptApp` — downloads/saves assets (`MainScriptApp.download`)
- `me: Person`, `input: Map`, `data: String?`, `equipment`
- Composition: `@file:DependsOnScript("id")` + `@DependsOn(...)` Maven resolution →
  apps can be built from reusable sub-apps/libraries

### Invocation surface (end-to-end)
- Backend: `POST /scripts/{id}/run` with `RunScriptBody(data, input, useCache)` →
  `ScriptResult(content: List<StoryContent>)` (`api/ScriptRoutes.kt`)
- Shared client API: `Api.runScript(...)` (`shared/api/.../ScriptApi.kt`)
- CRUD/discovery: `GET /scripts` (+search), `/me/scripts`, `POST /scripts`,
  `/scripts/{id}/data` (secrets), `/pin`, `/unpin`, `/stats`

### AI & audio
`backend/src/main/kotlin/com/queatz/OpenAi.kt` already wraps:
- `script(prompt, script)` — AI code generation (`AiScriptRequest`/`AiScriptResponse`)
- `json(prompt, schema)` — structured output
- `image(prompt, transparentBackground)` — image generation
- `speak(text)` — TTS / AI audio generation
- `transcribe(audioBytes)` — speech-to-text

### Reference app (from the screenshot)
The "microcosmic" language-learning app maps 1:1 onto today's primitives:
- Screen 1: `Section` title + `Input(hint="Enter IRL situation…")` + `Button("Start")` +
  "Previous situations" `Section` and history `Button`s (from per-user `storage`).
- Screen 2: `Section`s + vocabulary card grid (`Photos`/`Widget`) + grammar `Text` lines,
  each with `Audio(url)` (TTS), + `Button("Done")`.
- AI calls, AI audio, and per-user history are all achievable now.

## Gap Analysis

1. **No first-class App identity / multi-screen model.** A `Script` is a single `run`
   returning one `StoryContent` list. Apps are multi-screen, stateful; navigation is
   currently faked by `Button` re-running a script. No launch entry / title bar / screen
   stack.
2. **No App launcher / store / "installed apps" surface.** Android `MainActivity.kt` has
   fixed tabs and a `NavHost` with no `app/{id}` route; Web `ScriptsPage`/`AppNav.Script`
   is a developer view, not a consumer launcher.
3. **No per-user "app install" relationship** (analogous to `ScriptPin`, but with saved
   state/permissions).
4. **Limited interaction set.** `InputType` is only `Text`/`Photo`; `Scene` rendering is
   still a `// todo` in `ScriptRender`.
5. **AI/audio not exposed as a sandbox-safe capability.** Apps must hold a raw provider
   `secret` and call out via `http`; no governed `ai`/`audio` provided-property using the
   platform `OpenAi` wrapper (no per-app cost/rate-limit/key management).
6. **Caching for stateful apps.** `RunScript` compile cache is an unbounded `mutableMap`
   with TODOs (no FIFO bound, no cache-bust when dependent scripts change).

## Implementation Plan

Build "Apps" as a thin product layer over the proven Scripts engine — do **not** reinvent
the runtime. Reuse `StoryContent` as the rendering contract everywhere (reuse, no
duplication, new components in their own files).

### Phase 1 — Model "App" as a packaged Script (backend + shared)
- Introduce an `App` concept. Lowest-risk path: treat an App as a `Script` with app
  metadata (`name`, `description`, `photo`, `categories`, `background` already exist on
  `Script`). Add an `isApp`/`appManifest` field, or a dedicated `App` model in
  `shared/models/Models.kt` that references an entry script.
- Add a per-user **install** edge (mirror `ScriptPin`/`pinScript`):
  `AppInstall(person, app)` with `POST /apps/{id}/install` / `uninstall`, `GET /me/apps`.
- Add `GET /apps` (+search) and `GET /apps/{id}` for an app directory; reuse
  `searchScripts`/`scriptStats` patterns.

### Phase 2 — Multi-screen navigation in the UI tree (shared)
- Extend the script→client contract so a `ScriptResult` can request **navigation**
  (push/replace/pop a screen) plus a **title/back affordance**, instead of always
  replacing content in-place. Add a `Navigation`/`Screen` element (or fields on
  `ScriptResult`) so a `Button` can mean "open screen".
- Add `StoryContent` primitives as needed (grid/layout hints for the vocabulary card
  grid, more `InputType`s). Implement the pending `Scene` rendering.

### Phase 3 — Governed AI & audio capabilities (backend)
- Expose new sandbox-safe provided properties to scripts: `ai` (text/json/image) and
  `audio` (TTS) backed by the existing `OpenAi` wrapper. Removes the need to embed raw
  provider secrets; centralizes rate-limiting/cost accounting per app/user.
- Persist generated audio via the existing asset pipeline
  (`MainScriptApp.download`/`save`) and return `StoryContent.Audio(url)`.

### Phase 4 — App launcher surface (Android + Web)
- Android `MainActivity.kt`: add an **Apps** destination (`AppNav.Apps` tab/entry) plus an
  `app/{id}` composable route that loads the entry script and renders via the existing
  `StoryContents` renderer (generalize `ui/script/ScriptContent.kt` into a full-screen,
  navigable `AppScreen` with the Phase-2 screen stack). Reuse `background(url, opacity)`
  for app backgrounds (`Script.background` already exists).
- Web: promote `app/scripts` rendering into a consumer **Apps** section in `AppNav.kt`
  (launcher grid of installed apps → `App(id)` screen) reusing `ScriptsPage`/`Content`.

### Phase 5 — Hardening & developer experience
- Fix `RunScript` caching TODOs: bounded FIFO cache and cache-busting when any
  `DependsOnScript` dependency changes.
- Lean on the existing AI authoring path (`AiScriptRequest`/`AiScriptResponse`,
  `OpenAi.script`) to let creators generate/iterate apps from a prompt.
- Add tests alongside `backend/src/test/.../ScriptVerificationTest.kt` for app lifecycle
  (install, multi-screen run, per-user storage isolation, AI/audio capability).

## Suggested First PRs (smallest viable slice)

1. **Backend**: `App`/`AppInstall` model + `/apps`, `/apps/{id}`,
   install/uninstall/`me/apps` routes (reuse Script + Pin patterns).
2. **Shared**: `appApi` client methods mirroring `ScriptApi.kt`.
3. **Clients**: an `AppScreen` (Android route `app/{id}`, Web `AppNav.App`) that runs the
   entry script and renders `StoryContent`, plus an Apps launcher tab.
4. **Backend**: `ai`/`audio` provided properties wrapping `OpenAi`; return `Audio` URLs
   via the asset pipeline.
5. **Engine**: navigation element in `ScriptRender`/`ScriptResult` + `RunScript` cache
   hardening.

## Why This Approach

- **Reuses the cross-platform contract** (`StoryContent`) so one app definition renders
  identically on Android and Web — zero per-platform app code.
- **Builds on a hardened, sandboxed runtime** (`RunScript` + `RestrictedClassLoader`) and
  existing per-user DB (`ArangoScriptStorage`); security and data isolation already
  addressed.
- **Minimal new surface area**: Apps = Script + install edge + navigation extension +
  launcher UI + governed AI/audio capability. Everything else (execution, rendering,
  storage, HTTP, secrets, composition via `DependsOnScript`, AI authoring) already ships.
