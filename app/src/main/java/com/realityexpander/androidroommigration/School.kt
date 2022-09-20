package com.realityexpander.androidroommigration

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity // Table name defaults to "School"
data class School(
    @PrimaryKey(autoGenerate = false)
    val name: String  // version 6 of db
)
