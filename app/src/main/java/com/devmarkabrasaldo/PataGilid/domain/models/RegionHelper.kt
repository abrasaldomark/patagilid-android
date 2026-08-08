package com.devmarkabrasaldo.PataGilid.domain.models

object RegionHelper {
    val canonicalRegionsByIslandGroup = mapOf(
        IslandGroup.LUZON to listOf(
            "CAR (Cordillera Administrative Region)",
            "NCR (National Capital Region)",
            "Region 1 (Ilocos Region)",
            "Region 2 (Cagayan Valley)",
            "Region 3 (Central Luzon)",
            "Region 4A (CALABARZON)",
            "Region 4B (MIMAROPA)",
            "Region 5 (Bicol Region)"
        ),
        IslandGroup.VISAYAS to listOf(
            "Region 6 (Western Visayas)",
            "Region 7 (Central Visayas)",
            "Region 8 (Eastern Visayas)"
        ),
        IslandGroup.MINDANAO to listOf(
            "BARMM (Bangsamoro)",
            "Region 9 (Zamboanga Peninsula)",
            "Region 10 (Northern Mindanao)",
            "Region 11 (Davao Region)",
            "Region 12 (SOCCSKSARGEN)",
            "Region 13 (Caraga)"
        )
    )

    val allCanonicalRegions: List<String> = canonicalRegionsByIslandGroup.values.flatten()

    fun sortRegions(regions: Iterable<String>): List<String> {
        return regions
            .filter { it.trim().isNotBlank() && !it.trim().equals("Philippines", ignoreCase = true) && !it.trim().equals("All", ignoreCase = true) }
            .distinct()
            .sortedWith { r1, r2 ->
            val idx1 = indexOfRegion(r1)
            val idx2 = indexOfRegion(r2)
            if (idx1 != Int.MAX_VALUE && idx2 != Int.MAX_VALUE) {
                if (idx1 != idx2) idx1.compareTo(idx2) else r1.compareTo(r2, ignoreCase = true)
            } else if (idx1 != Int.MAX_VALUE) {
                -1
            } else if (idx2 != Int.MAX_VALUE) {
                1
            } else {
                compareNatural(r1, r2)
            }
        }
    }

    private fun indexOfRegion(region: String): Int {
        val cleanInput = region.trim()
        val inputShort = cleanInput.substringBefore(" (").trim()

        for (i in allCanonicalRegions.indices) {
            val canonical = allCanonicalRegions[i]
            val canonicalShort = canonical.substringBefore(" (").trim()
            if (canonical.equals(cleanInput, ignoreCase = true) ||
                canonicalShort.equals(cleanInput, ignoreCase = true) ||
                canonical.equals(inputShort, ignoreCase = true) ||
                canonicalShort.equals(inputShort, ignoreCase = true)
            ) {
                return i
            }
        }
        return Int.MAX_VALUE
    }

    private fun compareNatural(s1: String, s2: String): Int {
        val regex = "\\d+".toRegex()
        val match1 = regex.find(s1)
        val match2 = regex.find(s2)
        if (match1 != null && match2 != null &&
            s1.substringBefore(match1.value).equals(s2.substringBefore(match2.value), ignoreCase = true)
        ) {
            val num1 = match1.value.toIntOrNull() ?: 0
            val num2 = match2.value.toIntOrNull() ?: 0
            if (num1 != num2) return num1.compareTo(num2)
        }
        return s1.compareTo(s2, ignoreCase = true)
    }
}
