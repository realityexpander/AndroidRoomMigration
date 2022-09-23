package com.realityexpander.androidroommigration

import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Make individual database for each test
// Make database in memory not files
private const val DB_NAME = "test13"

// Migration tests only work with AutoMigration and always migrates
//   up to the Latest Database version (in UserDatabase.kt)

// Build custom RawQuery
//  https://stackoverflow.com/questions/44287465/how-to-dynamically-query-the-room-database-at-runtime

// Show columns for table
// PRAGMA table_info(user)

// Adam Mcneilly
// https://medium.com/androiddevelopers/understanding-migrations-with-room-f01e04b07929
// https://www.youtube.com/watch?v=tUGUNU6DPtk

@RunWith(AndroidJUnit4::class)
class UserMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        UserDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration1To2_containsCorrectData() {

        var db = helper.createDatabase(DB_NAME, 1).apply {
            execSQL("INSERT INTO user VALUES('test@test.com', 'Philipp')")  // note must use SQL
            close()
        }

        db = helper.runMigrationsAndValidate(DB_NAME, 2, true)

        db.query("SELECT * FROM user").apply {
            assertThat(moveToFirst()).isTrue()

            // ColumnIndex is the SQL name (not the data class property name)
            assertThat(getLong(getColumnIndex("created"))).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migration2To3_containsCorrectData() {

        val helper2to3 = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            UserDatabase::class.java,
            listOf(
                UserDatabase.Migration2To3(),
            ),
            FrameworkSQLiteOpenHelperFactory()
        )

        var db =
            helper2to3.createDatabase(DB_NAME, 2)
                .apply {
            execSQL("INSERT INTO user VALUES('test@test.com', 'Philipp', 100)")  // note must use SQL
            close()
        }

        db = helper2to3.runMigrationsAndValidate(DB_NAME, 3, true)

        db.query("SELECT * FROM user").apply {
            assertThat(moveToFirst()).isTrue()

            // ColumnIndex returns the data-class name (not the SQL name)
            assertThat(getLong(getColumnIndex("created"))).isEqualTo(100)
        }


        // Test that the data class (for the dao) uses the new column name
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                UserDatabase::class.java,
                DB_NAME
            )
            .setTransactionExecutor(testDispatcher.asExecutor())
            .setQueryExecutor(testDispatcher.asExecutor())
            .fallbackToDestructiveMigrationFrom(3)
            .build()
            .apply {
                testScope.runTest {
                    dao.insertUser(User("test@email.com", "student test name", 100))

                    // The dao uses the data-class property names for the columns (not the SQL name)
                    assertThat(dao.getUsers()[0].createdAt).isEqualTo(100)
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migration3To4_containsCorrectData() {

        val helper3to4 = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            UserDatabase::class.java,
            listOf(
                UserDatabase.Migration2To3(),
                UserDatabase.Migration3To4(),
            ),
            FrameworkSQLiteOpenHelperFactory()
        )

        val testUserName = "Phillip"
        var db = helper3to4.createDatabase(DB_NAME, 3).apply {
            execSQL("INSERT INTO user VALUES('test@test.com', '$testUserName', 100)")  // note must use SQL

            println("SQL Columns before migration: " + query("Select * from user").columnNames.joinToString { it })

            // Get the tables before migration
            val tableNames = getDatabaseTables()
            println("SQL Tables before migration: " +  tableNames.joinToString { it })

            close()
        }

        db = helper3to4.runMigrationsAndValidate(DB_NAME, 4, true)

        db.query("SELECT * FROM user").apply {
            assertThat(moveToFirst()).isTrue()

            // Make sure the ColumnInfo(SQL) name is correct
            assertThat(getColumnIndex("username")).isNotEqualTo(-1)

            // ColumnIndex returns the ColumnInfo(SQL) name (not the data-class name)
            assertThat(getString(getColumnIndex("username"))).isEqualTo(testUserName)
        }


        // Test that the data class (for the dao) uses the new column name
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserDatabase::class.java,
            DB_NAME
        )
            .setTransactionExecutor(testDispatcher.asExecutor())
            .setQueryExecutor(testDispatcher.asExecutor())
            .fallbackToDestructiveMigrationFrom(4)
            .build()
            .apply {

                testScope.runTest {
                    println("SQL Columns After migration: " + query(SimpleSQLiteQuery("Select * from user", null)).columnNames.joinToString { it })

                    // Get the tables after migration
                    val tableNames = getDatabaseTables()
                    println("SQL Tables after migration: " +  tableNames.joinToString { it })

                    val studentTestName = "student test name"
                    dao.insertUser(User("test@email.com", studentTestName, 100))

                    // The dao uses the the data-class property for the column name, not ColumnInfo(SQL)
                    assertThat(dao.getUser(studentTestName).studentName).isEqualTo(studentTestName)

                    // Must run a raw query to see if the column name is correct in the SQL
                    val queryString = "SELECT * FROM user WHERE student_name=?"
                    val args = ArrayList<String>()
                    args.add(studentTestName)
                    assertThat(
                        dao.getUserRawQuery(SimpleSQLiteQuery(queryString, args.toArray()))[0].studentName
                    ).isEqualTo(studentTestName)

                    println("getRawQuery =" + dao.getUserRawQuery(SimpleSQLiteQuery(queryString, args.toArray())) )
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migration4To5_containsCorrectData() {

        val testUserName = "Phillip"
        var db = helper.createDatabase(DB_NAME, 4).apply {
            execSQL("INSERT INTO user VALUES('test@test.com', '$testUserName', 100)")  // note must use SQL

            println("Columns before migration: " + query("Select * from user").columnNames.joinToString { it })

            // Get the tables before migration
            val tableNames = getDatabaseTables()
            println("Tables before migration: " +  tableNames.joinToString { it })

            close()
        }

        // NOTE: this is not needed for custom migrations. (why?) Causes error.
        // db = helper.runMigrationsAndValidate(DB_NAME, 5, true)

        // Test that the data class (for the dao) uses the new column name
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserDatabase::class.java,
            DB_NAME
        )
            .addMigrations(
                UserDatabase.migration4To5
            )
            .setTransactionExecutor(testDispatcher.asExecutor())
            .setQueryExecutor(testDispatcher.asExecutor())
            .fallbackToDestructiveMigration()
            //.createFromAsset("database/testVersion4.db") // get from ./assets/ folder
            .build()
            .apply {

                testScope.runTest {

                    println("SQL Columns After migration: " + query(SimpleSQLiteQuery("Select * from user", null)).columnNames.joinToString { it })

                    // Get the tables after migration
                    val tableNames = getDatabaseTables()
                    println("SQL Tables after migration: " +  tableNames.joinToString { it })

                    val studentTestName = "student test name"
                    dao.insertUser(User("test@email.com", studentTestName, 100))

                    // The dao uses the new column name for the data-class property
                    assertThat(dao.getUser(studentTestName).studentName).isEqualTo(studentTestName)

                    // Must run a raw query to see if the column correct in the SQL (was `username`)
                    val queryString = "SELECT * FROM user WHERE student_name=?"
                    val args = ArrayList<String>()
                    args.add(studentTestName)
                    println("getRawQuery =" + dao.getUserRawQuery(SimpleSQLiteQuery(queryString, args.toArray())))
                    assertThat(dao.getUserRawQuery(SimpleSQLiteQuery(queryString, args.toArray()))).isNotEmpty()

                    println("Columns After migration: " + query(SimpleSQLiteQuery("Select * from user", null)).columnNames.joinToString { it })

                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun migration5To6_containsCorrectData() {

        val testUserName = "Phillip"
        var db = helper.createDatabase(DB_NAME, 4).apply {
            execSQL("INSERT INTO user VALUES('test@test.com', '$testUserName', 100)")  // note must use SQL

            // Get the tables before migration
            val tableNames = getDatabaseTables()
            println("SQL Tables before migration: " +  tableNames.joinToString { it })

            close()
        }

        // NOTE: this is not needed for custom migrations. (why?) Causes error.
        // db = helper.runMigrationsAndValidate(DB_NAME, 6, true)

        // Test that the data class (for the dao) uses the new column name
        val testDispatcher = StandardTestDispatcher()
        val testScope = TestScope(testDispatcher)
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserDatabase::class.java,
            DB_NAME
        )
            .addMigrations(
                UserDatabase.migration4To5,
                UserDatabase.migration5To6,
            )
            .setTransactionExecutor(testDispatcher.asExecutor())
            .setQueryExecutor(testDispatcher.asExecutor())
            .fallbackToDestructiveMigration()
            .build()
            .apply {

                // Get the tables after migration
                val tableNames = getDatabaseTables()
                println("Tables after migration: " +  tableNames.joinToString { it })

                testScope.runTest {

                    val schoolTestName2 = "UTSA test school"
                    dao.insertSchool(School(name = schoolTestName2))

                    // The dao uses data-class property name for column name
                    assertThat(dao.getSchools()[0].name).isEqualTo(schoolTestName2)

                    // Must run a raw query to see if the column correct in the SQL
                    val queryString = "SELECT * FROM school WHERE name=?"
                    val args = ArrayList<String>()
                    args.add(schoolTestName2)

                    println("getRawQuery =" + dao.getSchoolRawQuery(SimpleSQLiteQuery(queryString, args.toArray())))
                    assertThat(dao.getSchoolRawQuery(SimpleSQLiteQuery(queryString, args.toArray()))).isNotEmpty()
                }
            }
    }

    private fun SupportSQLiteDatabase.getDatabaseTables(): ArrayList<String> {
        val arrTblNames = ArrayList<String>()
        val c: Cursor = query("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                arrTblNames.add(c.getString(c.getColumnIndex("name")))
                c.moveToNext()
            }
        }
        return arrTblNames
    }

    private fun UserDatabase.getDatabaseTables(): ArrayList<String> {
        val arrTblNames = ArrayList<String>()
        val c: Cursor = query("SELECT name FROM sqlite_master WHERE type='table'", null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                arrTblNames.add(c.getString(c.getColumnIndex("name")))
                c.moveToNext()
            }
        }
        return arrTblNames
    }

    @Test
    fun testAllMigrations() {
        val helper2to4 = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            UserDatabase::class.java,
            listOf(
                UserDatabase.Migration2To3(),
                UserDatabase.Migration3To4(),
            ),
            FrameworkSQLiteOpenHelperFactory()
        )

        helper2to4.createDatabase(DB_NAME, 1).apply { close() }

        // The custom migrations must be tested one at a time, only the last migration will
        //  run properly. *CANNOT* run each incremental migration.

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserDatabase::class.java,
            DB_NAME
        ).addMigrations(
            UserDatabase.migration4To5,
        )
            .fallbackToDestructiveMigration()
            .build()
            .apply {
                openHelper.writableDatabase.close() // will throw error if it fails
            }

        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UserDatabase::class.java,
            DB_NAME
        ).addMigrations(
            UserDatabase.migration5To6
        )
            .fallbackToDestructiveMigration()
            .build()
            .apply {
                openHelper.writableDatabase.close() // will throw error if it fails
            }
    }
}