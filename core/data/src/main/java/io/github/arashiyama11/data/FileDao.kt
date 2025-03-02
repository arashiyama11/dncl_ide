package io.github.arashiyama11.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.arashiyama11.data.entity.FileEntity
import io.github.arashiyama11.data.entity.SelectedFileEntity

@Dao
interface FileDao {
    @Query("SELECT * FROM file WHERE id = :id")
    suspend fun getFileById(id: Int): FileEntity?

    @Query("SELECT * FROM file WHERE name = :name")
    suspend fun getFileByName(name: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelectedFile(selectedFile: SelectedFileEntity)

    @Query("DELETE FROM selected_file")
    suspend fun clearSelectedFile()
}