package com.example.borrowbox.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `borrowings` ADD COLUMN `userId` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `username` TEXT NOT NULL,
                `passwordHash` TEXT NOT NULL,
                `salt` TEXT NOT NULL,
                `role` TEXT NOT NULL,
                `fullName` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 1,
                `createdAt` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `borrowings` ADD COLUMN `conditionLent` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `borrowings` ADD COLUMN `conditionReturned` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `borrowings` ADD COLUMN `returnPhotoUri` TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [BorrowingEntity::class, BorrowerEntity::class, UserEntity::class],
    version = 6,
    exportSchema = true
)
abstract class BorrowBoxDatabase : RoomDatabase() {
    abstract fun borrowingDao(): BorrowingDao
    abstract fun borrowerDao(): BorrowerDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: BorrowBoxDatabase? = null

        fun getInstance(context: Context): BorrowBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BorrowBoxDatabase::class.java,
                    "borrowbox_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
