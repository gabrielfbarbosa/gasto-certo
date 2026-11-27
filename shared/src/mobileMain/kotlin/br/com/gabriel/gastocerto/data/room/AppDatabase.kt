package br.com.gabriel.gastocerto.data.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Room database shared by Android and iOS (mobileMain). Room does not run on the Web,
 * which keeps the in-memory repositories (persistence is replaceable).
 */
@Database(entities = [PersonEntity::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
}

/** The Room compiler (KSP) generates the `actual` implementations for each mobile target. */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

const val DATABASE_NAME = "gastocerto.db"

/** Finishes the platform builder: bundled SQLite (same version on Android and iOS) and queries off the main thread. */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
