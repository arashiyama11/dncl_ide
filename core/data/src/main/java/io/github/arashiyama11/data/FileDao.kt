package io.github.arashiyama11.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileDao {
    @Query("SELECT * FROM file")
    fun getAllFiles(): List<io.github.arashiyama11.data.entity.FileEntity>

    @Query("SELECT * FROM file WHERE id = :id")
    fun getFileById(id: Int): io.github.arashiyama11.data.entity.FileEntity?

    @Query("SELECT * FROM file WHERE name = :name")
    fun getFileByName(name: String): io.github.arashiyama11.data.entity.FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFile(file: io.github.arashiyama11.data.entity.FileEntity)

    @Query("SELECT * FROM selected_file")
    fun getSelectedFile(): io.github.arashiyama11.data.entity.SelectedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSelectedFile(selectedFile: io.github.arashiyama11.data.entity.SelectedFileEntity)
}