package com.kupil.apexfetch

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.database.DriverFactory
import com.kupil.apexfetch.db.ApexDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ApexFetch",
    ) {
        val driver = DriverFactory().createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)
        val downloadPath = System.getProperty("user.home") + "/Downloads"

        // Masukkan ke App
        App(
            fetcher = fetcher,
            basePath = downloadPath
        )
    }
}