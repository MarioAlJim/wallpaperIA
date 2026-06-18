package com.wolf.wallpaper.core

enum class WeatherType(val index: Int, val displayName: String) {
    STORM(0, "Tormenta"),
    SUNNY(1, "Soleado");

    companion object {
        fun fromIndex(index: Int): WeatherType {
            return values().firstOrNull { it.index == index } ?: STORM
        }
    }
}
