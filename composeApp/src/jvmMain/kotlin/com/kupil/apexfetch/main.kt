package com.kupil.apexfetch

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kupil.apexfetch.db.ApexDatabase

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ApexFetch",
    ) {
        val driver = DriverFactory().createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)

        // Masukkan ke App
        App(fetcher = fetcher)
    }
}