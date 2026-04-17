package com.kupil.apexfetch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kupil.apexfetch.db.ApexDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(applicationContext).createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)

        setContent {
            App(fetcher = fetcher)
        }
    }
}
