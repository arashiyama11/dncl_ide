package io.github.arashiyama11.dncl_ide.util

enum class Platform {
    Android, Ios, Desktop, Web
}

expect val currentPlatform: Platform