import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptAcceptedLocation
import kotlin.script.experimental.api.ScriptCollectedData
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.acceptedLocations
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.baseClass
import kotlin.script.experimental.api.collectedAnnotations
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.ide
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.with
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.Repository
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromScriptSourceAnnotations
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.jvmTarget

@KotlinScript(
    compilationConfiguration = ScriptWithMavenDepsConfiguration::class,
    fileExtension = "kts"
)
abstract class ScriptWithMavenDeps

object ScriptWithMavenDepsConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            DependsOn::class,
            Repository::class,
            Serializable::class,
            Json::class
        )

        jvm {
            jvmTarget("21")
            dependenciesFromCurrentContext(
                "scripts", // :scripts (this library)
                "models", // :models
                "kotlin-scripting-dependencies", // DependsOn and Repository annotation
                wholeClasspath = true,
                unpackJarCollections = true
            )
            compilerOptions(
                "-Xadd-modules=ALL-MODULE-PATH",
                "-Xplugin=kotlin-serialization-compiler-plugin-embeddable-2.1.20.jar"
            )
        }

        refineConfiguration {
            onAnnotations(
                DependsOn::class,
                Repository::class,
                handler = ::configureMavenDepsOnAnnotations
            )
        }
    }
) {
    private fun readResolve(): Any = ScriptWithMavenDepsConfiguration
}

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

private fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData
        ?.get(ScriptCollectedData.collectedAnnotations)
        ?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}
