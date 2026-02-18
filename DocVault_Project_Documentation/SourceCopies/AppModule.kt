package com.docvaultbasic.di

import android.content.Context
import com.docvaultbasic.data.database.AppDatabase
import com.docvaultbasic.data.repository.DocumentRepository
import com.docvaultbasic.data.repository.FolderRepository
import com.docvaultbasic.security.PinManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePinManager(@ApplicationContext context: Context): PinManager {
        return PinManager(context)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context, pinManager: PinManager): AppDatabase {
        return AppDatabase.getInstance(context, pinManager.getDatabasePassphrase())
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(appDatabase: AppDatabase): DocumentRepository {
        return DocumentRepository(appDatabase.documentDao())
    }

    @Provides
    @Singleton
    fun provideFolderRepository(appDatabase: AppDatabase): FolderRepository {
        return FolderRepository(appDatabase.folderDao())
    }
}
