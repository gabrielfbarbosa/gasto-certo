package br.com.gabriel.gastocerto.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Android builder: the database file lives in the app's private databases folder. */
fun appDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
}
