package io.github.arashiyama11.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "path")
    val path: String = "",

    @ColumnInfo(name = "cursor_position")
    val cursorPosition: Int = 0
)