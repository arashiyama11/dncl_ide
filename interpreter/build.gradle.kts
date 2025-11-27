@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.kotlin.dsl.buildConfig
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildconfig)
}


buildConfig {
    buildConfigField("DNCL_VERSION", providers.gradleProperty("dncl.version"))
}

kotlin {
    jvmToolchain(17)

    wasmJs {
        browser {
            binaries.executable()
        }
    }

    listOf(
        macosArm64(),
        linuxArm64(),
        mingwX64()
    ).forEach {
        it.binaries {
            executable {
                entryPoint("io.github.arashiyama11.dncl_ide.interpreter.main")
            }
        }
    }


// Target declarations - add or remove as needed below. These define
// which platforms this KMP module supports.
// See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "io.github.arashiyama11.dncl_ide.interpreter"
        compileSdk = 35
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm("desktop")


// For iOS targets, this is also where you should
// configure native binary output. For more information, see:
// https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

// A step-by-step guide on how to include this library in an XCode
// project can be found here:
// https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "interpreterKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

// Source set declarations.
// Declaring a target automatically creates a source set with the same name. By default, the
// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
// common to share sources between related targets.
// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            kotlin.srcDir("build/generated/sources/dnclLibs/main")

            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.arrow.core)
                implementation(libs.arrow.fx.coroutines)
                // Add KMP dependencies here
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.runner)
                implementation(libs.androidx.core)
                implementation(libs.androidx.test.junit)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}

tasks.register<JavaExec>("runInterpreter") {
    group = "application"
    description = "Runs the interpreter main with custom args"

    val jvmMain = kotlin.targets
        .getByName("desktop")
        .compilations
        .getByName("main")

    classpath = files(jvmMain.output.classesDirs, jvmMain.runtimeDependencyFiles)
    mainClass.set("io.github.arashiyama11.dncl_ide.interpreter.MainKt")

    // コマンドラインの -PinterpreterArgs="arg1 arg2 ..." から取得
    val interpreterArgs: String? by project
    args = interpreterArgs
        ?.split("\\s+".toRegex())
        ?: emptyList()
}


tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assemble a fat JAR containing all runtime dependencies"

    // 出力 JAR 名のサフィックス
    archiveClassifier.set("all")

    // main コンパイル成果物を含める
    from(
        kotlin.targets["desktop"]
            .compilations["main"]
            .output
    )

    // ランタイムクラスパスの JAR を展開して取り込む
    dependsOn(configurations["desktopRuntimeClasspath"])
    from({
        configurations["desktopRuntimeClasspath"]
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    // 重複ファイルは無視
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Main-Kt のフルパスを指定
    manifest {
        attributes["Main-Class"] = "io.github.arashiyama11.dncl_ide.interpreter.MainKt"
    }
}


val generateTextMap by tasks.registering {
    val inputDir = rootDir.resolve("dncl")
    val generatedTextDir = layout.buildDirectory.dir("generated/sources/dnclLibs/main")

    inputs.dir(inputDir)
    outputs.dir(generatedTextDir)

    doLast {
        val pkg = "io.arashiyama11.dncl_ide.generated"
        val outDir = generatedTextDir.get()
            .dir(pkg.replace('.', '/'))
            .asFile

        outDir.mkdirs()

        val outFile = File(outDir, "DnclLib.kt")

        val baseDirFile = inputDir

        val files = baseDirFile
            .walkTopDown()
            .filter { it.isFile }
            .toList()

        outFile.writeText(buildString {
            appendLine("package $pkg")
            appendLine()
            appendLine("object DnclLibs {")
            appendLine("    val texts: Map<String, String> = mapOf(")

            files.forEachIndexed { index, file ->
                val relPath = baseDirFile.toPath()
                    .relativize(file.toPath())
                    .toString()
                    .replace(File.separatorChar, '/')

                val raw = file.readText(Charsets.UTF_8)

                val escaped = raw
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")

                append("        \"$relPath\" to \"$escaped\"")
                if (index != files.lastIndex) appendLine(",") else appendLine()
            }

            appendLine("    )")
            appendLine()
            appendLine("    fun load(name: String): String? =")
            appendLine("        texts[name]")
            appendLine("}")
        })
    }
}

tasks.filterIsInstance<KotlinCompile>().forEach {
    it.dependsOn(generateTextMap)
}

tasks.named { it.startsWith("compileKotlin") }.forEach {
    it.dependsOn(generateTextMap)
}