package com.expstudio.localai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.expstudio.localai.data.db.dao.ChatDao
import com.expstudio.localai.data.db.dao.ModelDao
import com.expstudio.localai.data.db.entities.ChatMessage
import com.expstudio.localai.data.db.entities.ChatSession
import com.expstudio.localai.data.db.entities.InstalledModel
import com.expstudio.localai.data.db.entities.Project
import com.expstudio.localai.data.db.entities.Role

class Converters {
    @TypeConverter fun roleToString(role: Role): String = role.name
    @TypeConverter fun stringToRole(value: String): Role = Role.valueOf(value)
}

@Database(
    entities = [InstalledModel::class, ChatSession::class, ChatMessage::class, Project::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localai.db",
                )
                    // Pre-release schema: recreate on upgrade rather than ship migrations.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
