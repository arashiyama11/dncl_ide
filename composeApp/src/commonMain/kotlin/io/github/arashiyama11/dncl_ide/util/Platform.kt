package io.github.arashiyama11.dncl_ide.util

enum class Platform {
    Android, Ios, Desktop
}

expect val currentPlatform: Platform