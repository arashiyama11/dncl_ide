package io.github.arashiyama11.data

import android.content.Context
import io.github.arashiyama11.data.repository.FileRepository
import io.github.arashiyama11.data.repository.IFileRepository
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class DataModule {

    @Single
    fun appDatabase(applicationContext: Context) = AppDatabase.getDatabase(applicationContext)

    @Single
    fun fileDao(appDatabase: AppDatabase) = appDatabase.fileDao()

    @Single(binds = [IFileRepository::class])
    fun fileRepository(context: Context, fileDao: FileDao) = FileRepository(context, fileDao)
}