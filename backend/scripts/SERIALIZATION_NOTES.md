# Notes: `@Serializable` in user scripts & fat-jar packaging

These notes capture the root cause and fix for a tricky bug where user scripts that
declare `@Serializable` classes failed at runtime with:

```
kotlinx.serialization.SerializationException: Serializer for class 'DeepSeekWordInfo' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

## Symptom

- Scripts compiled and ran fine in unit tests and via `installDist`.
- The failure only appeared in the **packaged fat jar** (ktor `buildFatJar` / shadowJar),
  and only on the first code path that actually serialized a `@Serializable` class
  (e.g. pressing "Generate Practice", which calls into `StructuredDeepSeek.prompt<T>()`).

## Root cause

User scripts are compiled at runtime by the embedded Kotlin scripting compiler. For
script-defined `@Serializable` classes to get generated serializers, the
**kotlinx.serialization compiler plugin** must be discoverable on the scripting
compiler's classpath. Two distinct fat-jar packaging problems broke this:

1. **`META-INF/services` collision.** Multiple compiler plugins (the scripting compiler
   and the serialization plugin) ship service files with identical names
   (`org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor` and
   `...CompilerPluginRegistrar`). The default shadow strategy keeps only one file,
   silently dropping the serialization plugin's registration. The scripting compiler
   then never applies the serialization plugin to user scripts.

2. **Lost per-jar manifest.** The serialization compiler plugin reads the
   kotlinx-serialization-core runtime version from the providing jar's `MANIFEST.MF`
   (`Implementation-Title` / `Implementation-Version`). A fat jar collapses every
   dependency into one jar with a single manifest, so that per-library version is lost
   and the plugin bails out.

Neither problem appears with separate-jar classpaths (`installDist`, tests), because
each library keeps its own service files and manifest there.

## Fix

In `backend/build.gradle.kts`, configure the `shadowJar` task (inside `afterEvaluate`,
because the ktor plugin reconfigures it):

- `duplicatesStrategy = DuplicatesStrategy.INCLUDE` — required so `mergeServiceFiles()`
  actually runs. The ktor plugin's default `EXCLUDE` strips duplicate service files
  *before* the merge transformer sees them.
- `mergeServiceFiles()` — merges the compiler-plugin `META-INF/services` files instead
  of overwriting them, preserving the serialization plugin's registration.
- Re-declare `Implementation-Title = kotlinx-serialization-core` and
  `Implementation-Version` on the fat-jar manifest so the plugin can read the runtime
  version.

## Lessons learned

- **Reproduce in the exact runtime artifact.** Environmental bugs that pass in tests and
  `installDist` may still fail in the fat jar. Build and exercise the same artifact you
  ship.
- **Fat jars are hostile to classpath-discovered tooling.** Anything wired via
  `META-INF/services` or probed via jar manifests (compiler plugins, etc.) can break when
  jars are merged, minimized, or relocated. Always `mergeServiceFiles()` and preserve the
  relevant manifest attributes.
- **Know your shadow plugin's defaults** (gradleup shadow 9.x): `mergeServiceFiles()`
  needs `duplicatesStrategy = INCLUDE`.
- **Listen to "it breaks exactly when…" clues.** The fact that it only failed on the
  first serializing branch, plus "I run a jar on the server", pointed straight at
  runtime serializer discovery in a packaged jar rather than a code defect.
- **Leave guardrails.** The fix lives in build config that "looks unused" and is tempting
  to delete. Keep the explanatory comment and the serialization tests so the fix can't
  silently regress.

## Guidance for script authors

- Prefer reified / reflective serialization in scripts (works through the sandbox).
- A K2 limitation remains: an explicit `Type.serializer()` companion call on a class
  declared in the *main* script may not resolve at compile time (the FIR-synthetic
  companion isn't exposed). Use reified/reflective serialization instead.
