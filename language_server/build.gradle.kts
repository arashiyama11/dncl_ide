@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import java.util.jar.Attributes

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    jvmToolchain(17)

    wasmJs {
        browser {
            binaries.executable()
        }
    }

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    androidLibrary {
        namespace = "io.github.arashiyama11.dncl_ide.language_server"
        compileSdk = 36
        minSdk = 26

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
    val xcfName = "language_serverKit"

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
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.arrow.core)
                implementation(libs.arrow.fx.coroutines)
                implementation(project(":interpreter"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}


val runJsonRpcServer by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Run stdio-based JSON-RPC 2.0 server (Main.kt entrypoint)"

    // KMP の 'desktop' JVM ターゲットの main コンパイル結果と依存をクラスパスに含める
    val desktopMain = kotlin.targets
        .getByName("desktop")
        .compilations
        .getByName("main") as KotlinJvmCompilation

    classpath = files(
        desktopMain.runtimeDependencyFiles,      // ライブラリ依存
        desktopMain.output.allOutputs            // コンパイルされた .class
    )

    mainClass.set("io.github.arashiyama11.dncl_ide.language_server.MainKt")
    standardInput = System.`in`
    standardOutput = System.out
    args = project.properties["args"]?.toString()?.split(" ") ?: emptyList()
}


val createJsonRpcServerJar by tasks.registering(Jar::class) {
    group = "build"
    description = "desktop ターゲット用の全依存入り実行 JAR を生成"

    // === desktop JVM ターゲットのコンパイル成果物＆依存を取得 ===
    val desktopMain = kotlin.targets
        .getByName("desktop")
        .compilations
        .getByName("main") as KotlinJvmCompilation

    // fat-jar に含めるファイルを追加
    // 1) 自身でコンパイルされた .class
    from(desktopMain.output.classesDirs)
    // 2) resources (もしあれば)
    from(desktopMain.output.resourcesDir)
    // 3) 依存ライブラリ JAR を展開して取り込む
    from({
        desktopMain.runtimeDependencyFiles
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })

    // === 出力ファイル名など ===
    archiveBaseName.set("language_server")   // お好みで
    archiveClassifier.set("desktop-all")     // 「-desktop-all.jar」になる
    archiveVersion.set("")                   // 省略すると “-1.0.0” などが付く場合は空にするとスッキリ

    // === 実行可能 JAR にするため Main-Class を指定 ===
    manifest {
        attributes(
            Attributes.Name.MANIFEST_VERSION.toString() to "1.0",
            "Main-Class" to "io.github.arashiyama11.dncl_ide.language_server.MainKt"
        )
    }

    // 競合ファイルは除外しておくと安全
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Kotlin コンパイルが先に走るよう依存関係を張る
    dependsOn(desktopMain.compileTaskProvider)
}
