package com.realityexpander.androidroommigration

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
//    entities = [User::class], // version 1-5 of db
    entities = [User::class, School::class], // version 6 of db
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),  // adding a column (created) is an AutoMigration
        AutoMigration(from = 2, to = 3, spec = UserDatabase.Migration2To3::class),
        AutoMigration(from = 3, to = 4, spec = UserDatabase.Migration3To4::class),
    ]
)
abstract class UserDatabase: RoomDatabase() {

    abstract val dao: UserDao

    // The from/to ColumnNames are the data-class property names, *NOT* the ColumnInfo annotation (SQL name)
    @RenameColumn(tableName = "User", fromColumnName = "created", toColumnName = "createdAt")
    class Migration2To3: AutoMigrationSpec

    // The from/to ColumnNames are the data-class property names, *NOT* the ColumnInfo annotation (SQL name)
    @RenameColumn(tableName = "User", fromColumnName = "username", toColumnName = "studentName")
    class Migration3To4: AutoMigrationSpec

    companion object {

        // Renaming a table's column in a table requires a custom migration (not an AutoMigration)
        val migration4To5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This changes the "ColumnInfo"(SQL) name annotation on the property (not the data class property name)
                database.execSQL("ALTER TABLE user RENAME COLUMN username TO student_name")
            }
        }

        // Adding a new table to a database requires a custom migration (not an AutoMigration)
        val migration5To6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Adds the new table (school) to the database
                database.execSQL("CREATE TABLE IF NOT EXISTS school (name CHAR NOT NULL PRIMARY KEY)")
            }
        }
    }
}