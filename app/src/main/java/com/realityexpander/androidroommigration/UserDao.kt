package com.realityexpander.androidroommigration

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user")
    suspend fun getUsers(): List<User>

//    @Query("SELECT * FROM user WHERE username = :studentName") // version 1 of db
    @Query("SELECT * FROM user WHERE student_name = :studentName") // version 5 of db
    suspend fun getUser(studentName: String): User

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: School)  // version 6 of db

    @Query("SELECT * FROM school")
    suspend fun getSchools(): List<School>  // version 6 of db


    ///////////////////
    // Used for testing
    @RawQuery
    suspend fun getUserRawQuery(query: SupportSQLiteQuery): List<User>

    @RawQuery
    suspend fun getSchoolRawQuery(query: SupportSQLiteQuery): List<School>
}