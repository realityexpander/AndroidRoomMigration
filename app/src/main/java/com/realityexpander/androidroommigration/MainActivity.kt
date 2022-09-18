package com.realityexpander.androidroommigration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

// Source material: https://www.youtube.com/watch?v=hrJZIF7qSSw&t=1s


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
                applicationContext,
                UserDatabase::class.java,
                "users.db"
            )
            .addMigrations(
                UserDatabase.migration4To5,
                UserDatabase.migration5To6
            )
            .build()

        // Show users in debug console
        lifecycleScope.launch {
            db.dao.getUsers().forEach(::println)
        }

        // Show schools in debug console
        lifecycleScope.launch {
            db.dao.getSchools().forEach(::println)
        }

        // Generate new data
        (1..10).forEach {
            lifecycleScope.launch {
                // Add students/users
//                db.dao.insertUser(
//                    User(
//                        email = "test$it@test.com",
//                        username = "User test $it",
//                        created = System.currentTimeMillis(),
//                    )
//                )

                // Add schools
//                db.dao.insertSchool(
//                    School(
//                        name = "School test$it"
//                    )
//                )
            }
        }
    }
}