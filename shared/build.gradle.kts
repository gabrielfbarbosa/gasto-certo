import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidxRoom)
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

    // Re-applies the default hierarchy (commonMain → iosMain, webMain…) before creating mobileMain;
    // without it, the manual dependsOn below would turn the default template off.
    applyDefaultHierarchyTemplate()

    sourceSets {
        // mobileMain: code shared only by Android and iOS (Room KMP, which does not run on the Web).
        val mobileMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)
        mobileMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

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
        getByName("androidHostTest").dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.androidx.testRunner)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    // Room compiler (KSP) only for the mobile targets: Room does not run on js/wasm.
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.androidx.room.compiler) }
}

room {
    schemaDirectory("$projectDir/schemas")
}

/**
 * RNF07 — the domain depends on no framework and no other layer.
 * Fails if any file in `domain/` imports something outside the allowed list:
 * plain Kotlin, kotlinx-datetime and the domain itself; `kotlinx.coroutines.flow` only in `domain.repository`.
 */
abstract class VerifyDomain : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val allowed = listOf("kotlin.", "kotlinx.datetime.", "br.com.gabriel.gastocerto.domain.")
        val allowedInRepository = allowed + "kotlinx.coroutines.flow."
        val importLine = Regex("""^\s*import\s+([\w.]+)""")
        val violations = sources.files.sorted().flatMap { file ->
            val inRepository = file.invariantSeparatorsPath.contains("/domain/repository/")
            val list = if (inRepository) allowedInRepository else allowed
            file.readLines().mapIndexedNotNull { i, line ->
                val imported = importLine.find(line)?.groupValues?.get(1) ?: return@mapIndexedNotNull null
                if (list.none { imported.startsWith(it) }) "${file.name}:${i + 1}: import $imported" else null
            }
        }
        val text = if (violations.isEmpty()) "OK: ${sources.files.size} domain file(s) with no forbidden dependencies."
        else "RNF07 violated — the domain may only import ${allowed.joinToString()} " +
            "(and kotlinx.coroutines.flow in domain.repository):\n" + violations.joinToString("\n")
        report.get().asFile.writeText(text)
        if (violations.isNotEmpty()) throw GradleException(text)
    }
}

val verifyDomain by tasks.registering(VerifyDomain::class) {
    group = "verification"
    description = "RNF07: checks that the domain package imports no Compose, Room, Koin, Android, iOS or other layers."
    // Production code only: domain tests may use kotlinx-coroutines-test, kotlin.test etc.
    sources.from(fileTree("src") {
        include("*/kotlin/br/com/gabriel/gastocerto/domain/**/*.kt")
        exclude("*Test/**")
    })
    report.set(layout.buildDirectory.file("reports/verifyDomain.txt"))
}

tasks.named("check") { dependsOn(verifyDomain) }
