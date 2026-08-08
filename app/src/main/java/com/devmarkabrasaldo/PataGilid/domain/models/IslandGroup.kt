package com.devmarkabrasaldo.PataGilid.domain.models

enum class IslandGroup(val displayName: String) {
    LUZON("Luzon"),
    VISAYAS("Visayas"),
    MINDANAO("Mindanao");

    companion object {
        fun fromDisplayName(name: String?): IslandGroup {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: LUZON
        }
    }
}
