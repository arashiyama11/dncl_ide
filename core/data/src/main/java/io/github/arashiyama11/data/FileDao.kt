package io.github.arashiyama11.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.arashiyama11.data.entity.FileEntity

@Dao
interface FileDao {
    @Query("SELECT * FROM file WHERE id = :id")
    suspend fun getFileById(id: Int): FileEntity?

    @Query("SELECT * FROM file WHERE path = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)
}