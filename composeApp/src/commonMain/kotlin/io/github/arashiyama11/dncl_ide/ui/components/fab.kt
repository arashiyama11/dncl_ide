package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import  androidx.compose.ui.graphics.Color

@Composable
fun Fab(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.primary).clickable { onClick() }) {
        Box(modifier = Modifier.padding(16.dp)) {
            icon()
        }
    }
}

@Composable
fun SmallFab(onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(Color.White).clickable { onClick() }) {
        Box(modifier = Modifier.padding(8.dp)) {
            icon()
        }
    }
}