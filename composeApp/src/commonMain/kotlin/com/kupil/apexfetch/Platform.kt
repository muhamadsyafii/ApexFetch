package com.kupil.apexfetch

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform