package io.github.arashiyama11.dncl_ide.util

import com.mikepenz.aboutlibraries.Libs
import dncl_ide.composeapp.generated.resources.Res
import kotlinx.coroutines.withContext

suspend fun getAllLicenses(): Libs = withContext(ioDispatcher) {
    val jsonString = Res.readBytes("files/aboutlibraries.json").decodeToString()
    Libs.Builder().withJson(jsonString).build()
}