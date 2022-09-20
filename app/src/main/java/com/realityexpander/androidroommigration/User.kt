package com.realityexpander.androidroommigration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Note: @ColumnInfo refers to the SQL column name, not the data-class property name
// The dao will use the data-class property name.
// The SQL queries will use the SQL column name.

@Entity  // table name defaults to "User"
data class User(
    @PrimaryKey(autoGenerate = false)
    val email: String,    // version 1 of db

    //@ColumnInfo(name = "username", defaultValue = "0") // version 1 of db
    //val username: String, // version 1 of db
    @ColumnInfo(name = "student_name", defaultValue = "0") // version 5 of db
    val studentName: String, // version 4 of db

    @ColumnInfo(name = "created", defaultValue = "0") // note default value is inserted for migrated data, and not for new data.
    //val created: Long // version 2 of db
    val createdAt: Long = System.currentTimeMillis()  // Version 3 of db, Uses the current time as the default value for new data.
)
