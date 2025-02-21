package io.github.arashiyama11.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "selected_file",
    foreignKeys = [ForeignKey(
        entity = FileEntity::class,
        parentColumns = ["id"],
        childColumns = ["fileId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)

data class SelectedFileEntity(
    @PrimaryKey
    val fileId: Int = 0
)
