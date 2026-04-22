package com.kupil.apexfetch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.kupil.apexfetch.core.ApexFetcher
import com.kupil.apexfetch.database.DriverFactory
import com.kupil.apexfetch.db.ApexDatabase

/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(applicationContext).createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)
        val downloadPath = applicationContext.filesDir.absolutePath


        setContent {

            App(
                fetcher = fetcher,
                basePath = downloadPath

            )
        }
    }
}
*/


/*
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val driver = DriverFactory(applicationContext).createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)
        val downloadPath = applicationContext.filesDir.absolutePath

        setContent {
            App(
                fetcher = fetcher,
                basePath = downloadPath,
                onNavigateToXml = {
                    startActivity(Intent(this@MainActivity, XmlSampleActivity::class.java))
                }
            )
        }
    }
}
*/


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val driver = DriverFactory(applicationContext).createDriver()
        val database = ApexDatabase(driver)
        val fetcher = ApexFetcher(database)
        val downloadPath = applicationContext.filesDir.absolutePath

        setContent {
            var currentScreen by remember { mutableStateOf("DASHBOARD_CMP") }
            when (currentScreen) {
                "DASHBOARD_CMP" -> {
                    App(
                        fetcher = fetcher,
                        basePath = downloadPath,
                        onNavigateToXml = {
                            startActivity(Intent(this@MainActivity, XmlSampleActivity::class.java))
                        },
                        onNavigateToNativeCompose = {
                            currentScreen = "NATIVE_COMPOSE"
                        },
                        namePlatform = "CMP Android"
                    )
                }
                "NATIVE_COMPOSE" -> {
                    ComposeNativeSampleScreen(
                        fetcher = fetcher,
                        basePath = downloadPath,
                        onBackPressed = { currentScreen = "DASHBOARD_CMP" }
                    )
                }
            }
        }
    }
}
