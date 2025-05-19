# AI Intro

High-level tour of the Ai La Ai codebase. It’s organized as a small monorepo of four self-contained Gradle projects that stitch together into a full-stack, cross-platform AI-driven social/chat app.

    1. Shared (“ailaai-shared”)
       • A Kotlin Multiplatform composite build (included by Android, Web and Backend)
       • Sub-modules for:
         – models (data classes, IDs, DB entities)
         – api (typed REST client definitions, serializers)
         – push (notification payloads & helper functions)
         – widgets, reminders, content, config, etc.
       • Published locally so each platform can reference e.g.
         `implementation("app.ailaai.shared:models")`
    2. Backend (“Ai La Ai Backend”)
       • Kotlin + Ktor running on Netty (see `Application.kt`)
       • Plugs in: HTTP, JSON serialization, security, routing (all in `com.queatz.plugins`)
       • Exposes ~40+ REST routes (Accounts, Bots, Calls, Chat, Groups, Cards, Scripts, etc.) under `com.queatz.api`
       • Persists to ArangoDB (see instructions in `backend/README.md`)
       • Composite-builds the shared modules so it can reuse the same models/api definitions
    3. Android App (“Ai La Ai” in `android/app`)
       • Kotlin + Jetpack Compose UI (with view-binding fallback)
       • Ktor HTTP client + kotlinx-serialization for talking to the backend
       • ObjectBox as local cache, DataStore for prefs, ACRA crash reporting
       • Push via Firebase & Huawei (dual-push support), plus deep-linking, maps (Google & Huawei via ChoiceSDK)
       • Feature set: group chat, 1:1 messages, voice/video calls (VideoSDK), bots & NPCs, trades/items, reminders, “stories” slideshows, maps, rich text, …
       • Pulls in exactly the same shared modules via `includeBuild("../shared")`
    4. Web Front-end (“ailaai” in `web/`)
       • Kotlin/JS + JetBrains Compose for Web + Webpack
       • Reuses the shared `api` + `models` modules to generate and consume the very same JSON REST types
       • Delivers a browser UI counterpart to the Android app

How it all comes together
– Developers edit shared business-logic/data/model code once and get it on all three platforms.
– The backend exposes a single, consistent REST surface under /…/api/….
– Android and Web clients both pull in the shared “api” module to strongly type those calls.
– Each of the three projects (android, backend, web) is a standalone Gradle build that “includeBuild(../shared)” to compose in the common modules.


