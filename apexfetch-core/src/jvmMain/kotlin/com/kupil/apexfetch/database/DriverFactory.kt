/*
 * Created by Muhamad Syafii
 * 21/4/2026 - muhamadsyafii4@gmail.com
 * Copyright (c) 2026.
 * All Rights Reserved
 */

package com.kupil.apexfetch.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.kupil.apexfetch.db.ApexDatabase
import java.io.File

actual class DriverFactory {
  actual fun createDriver(): SqlDriver {
    val databaseName = "apexfetch.db"
    val dbFile = File(databaseName)
    val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    if (!dbFile.exists()) {
      ApexDatabase.Companion.Schema.create(driver)
    }
    return driver
  }
}