import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    js {
        browser()
        binaries.executable()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    android {
       namespace = "br.com.gabriel.gastocerto.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    // Reaplica a hierarquia padrão (commonMain → iosMain, webMain…) antes de criar o mobileMain;
    // sem isso, o dependsOn manual abaixo desligaria o template padrão.
    applyDefaultHierarchyTemplate()

    sourceSets {
        // mobileMain: código compartilhado só entre Android e iOS (Room KMP, que não roda na Web).
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation3.ui)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

/**
 * RNF07 — o domínio não depende de framework nem de outra camada.
 * Falha se algum arquivo em `domain/` importar algo fora da lista permitida:
 * Kotlin puro, kotlinx-datetime e o próprio domínio; `kotlinx.coroutines.flow` só em `domain.repositorio`.
 */
abstract class VerificarDominio : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fontes: ConfigurableFileCollection

    @get:OutputFile
    abstract val relatorio: RegularFileProperty

    @TaskAction
    fun verificar() {
        val permitidos = listOf("kotlin.", "kotlinx.datetime.", "br.com.gabriel.gastocerto.domain.")
        val permitidosNoRepositorio = permitidos + "kotlinx.coroutines.flow."
        val importacao = Regex("""^\s*import\s+([\w.]+)""")
        val violacoes = fontes.files.sorted().flatMap { arquivo ->
            val noRepositorio = arquivo.invariantSeparatorsPath.contains("/domain/repositorio/")
            val lista = if (noRepositorio) permitidosNoRepositorio else permitidos
            arquivo.readLines().mapIndexedNotNull { i, linha ->
                val pacote = importacao.find(linha)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                if (lista.none { pacote.startsWith(it) }) "${arquivo.name}:${i + 1}: import $pacote" else null
            }
        }
        val texto = if (violacoes.isEmpty()) "OK: ${fontes.files.size} arquivo(s) do domínio sem dependências proibidas."
        else "RNF07 violado — o domínio só pode importar ${permitidos.joinToString()} " +
            "(e kotlinx.coroutines.flow em domain.repositorio):\n" + violacoes.joinToString("\n")
        relatorio.get().asFile.writeText(texto)
        if (violacoes.isNotEmpty()) throw GradleException(texto)
    }
}

val verificarDominio by tasks.registering(VerificarDominio::class) {
    group = "verification"
    description = "RNF07: verifica que o pacote domain não importa Compose, Room, Koin, Android, iOS nem outras camadas."
    fontes.from(fileTree("src") { include("*/kotlin/br/com/gabriel/gastocerto/domain/**/*.kt") })
    relatorio.set(layout.buildDirectory.file("reports/verificarDominio.txt"))
}

tasks.named("check") { dependsOn(verificarDominio) }