package com.docvaultbasic.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [DocumentEntity::class, FolderEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray? = null): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docvault_db"
                )
                
                if (passphrase != null) {
                    val factory = SupportFactory(passphrase)
                    builder.openHelperFactory(factory)
                }

                val instance = builder.build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
