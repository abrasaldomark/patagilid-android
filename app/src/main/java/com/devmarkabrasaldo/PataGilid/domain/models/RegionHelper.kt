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
            "Region 9 (Zamboanga Peninsula)",
            "Region 10 (Northern Mindanao)",
            "Region 11 (Davao Region)",
            "Region 12 (SOCCSKSARGEN)",
            "Region 13 (Caraga)",
            "BARMM (Bangsamoro)"
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

    fun mapToInternalRegion(adminArea: String, subAdminArea: String, locality: String): Pair<String?, IslandGroup?> {
        val areas = listOf(adminArea, subAdminArea, locality).filter { it.isNotBlank() }
        for (area in areas) {
            val result = checkRegion(area.lowercase())
            if (result.first != null) return result
        }
        // Fallback to combined string if individual parts fail
        val searchString = "$adminArea $subAdminArea $locality".lowercase()
        return checkRegion(searchString)
    }

    private fun checkRegion(searchString: String): Pair<String?, IslandGroup?> {
        // LUZON
        if (searchString.contains("ilocos") || searchString.contains("pangasinan") || searchString.contains("la union")) {
            return Pair("Region 1 (Ilocos Region)", IslandGroup.LUZON)
        } else if (searchString.contains("cagayan") || searchString.contains("isabela") || searchString.contains("nueva vizcaya") || searchString.contains("quirino") || searchString.contains("batanes")) {
            return Pair("Region 2 (Cagayan Valley)", IslandGroup.LUZON)
        } else if (searchString.contains("central luzon") || searchString.contains("aurora") || searchString.contains("bataan") || searchString.contains("bulacan") || searchString.contains("nueva ecija") || searchString.contains("pampanga") || searchString.contains("tarlac") || searchString.contains("zambales")) {
            return Pair("Region 3 (Central Luzon)", IslandGroup.LUZON)
        } else if (searchString.contains("calabarzon") || searchString.contains("batangas") || searchString.contains("cavite") || searchString.contains("laguna") || searchString.contains("quezon") || searchString.contains("rizal")) {
            return Pair("Region 4A (CALABARZON)", IslandGroup.LUZON)
        } else if (searchString.contains("mimaropa") || searchString.contains("marinduque") || searchString.contains("occidental mindoro") || searchString.contains("oriental mindoro") || searchString.contains("palawan") || searchString.contains("romblon")) {
            return Pair("Region 4B (MIMAROPA)", IslandGroup.LUZON)
        } else if (searchString.contains("bicol") || searchString.contains("albay") || searchString.contains("camarines") || searchString.contains("catanduanes") || searchString.contains("masbate") || searchString.contains("sorsogon")) {
            return Pair("Region 5 (Bicol Region)", IslandGroup.LUZON)
        } else if (searchString.contains("cordillera") || searchString.contains("abra") || searchString.contains("apayao") || searchString.contains("benguet") || searchString.contains("ifugao") || searchString.contains("kalinga") || searchString.contains("mountain province") || searchString.contains("car")) {
            return Pair("CAR (Cordillera Administrative Region)", IslandGroup.LUZON)
        } else if (searchString.contains("ncr") || searchString.contains("national capital") || searchString.contains("manila")) {
            return Pair("NCR (National Capital Region)", IslandGroup.LUZON)
        }
        
        // VISAYAS
        else if (searchString.contains("western visayas") || searchString.contains("aklan") || searchString.contains("antique") || searchString.contains("capiz") || searchString.contains("guimaras") || searchString.contains("iloilo") || searchString.contains("negros occidental")) {
            return Pair("Region 6 (Western Visayas)", IslandGroup.VISAYAS)
        } else if (searchString.contains("central visayas") || searchString.contains("bohol") || searchString.contains("cebu") || searchString.contains("negros oriental") || searchString.contains("siquijor")) {
            return Pair("Region 7 (Central Visayas)", IslandGroup.VISAYAS)
        } else if (searchString.contains("eastern visayas") || searchString.contains("biliran") || searchString.contains("leyte") || searchString.contains("samar")) {
            return Pair("Region 8 (Eastern Visayas)", IslandGroup.VISAYAS)
        }
        
        // MINDANAO
        else if (searchString.contains("zamboanga")) {
            return Pair("Region 9 (Zamboanga Peninsula)", IslandGroup.MINDANAO)
        } else if (searchString.contains("northern mindanao") || searchString.contains("bukidnon") || searchString.contains("camiguin") || searchString.contains("lanao del norte") || searchString.contains("misamis")) {
            return Pair("Region 10 (Northern Mindanao)", IslandGroup.MINDANAO)
        } else if (searchString.contains("davao") || searchString.contains("compostela")) {
            return Pair("Region 11 (Davao Region)", IslandGroup.MINDANAO)
        } else if (searchString.contains("soccsksargen") || searchString.contains("cotabato") || searchString.contains("sarangani") || searchString.contains("sultan kudarat")) {
            return Pair("Region 12 (SOCCSKSARGEN)", IslandGroup.MINDANAO)
        } else if (searchString.contains("caraga") || searchString.contains("agusan") || searchString.contains("dinagat") || searchString.contains("surigao")) {
            return Pair("Region 13 (Caraga)", IslandGroup.MINDANAO)
        } else if (searchString.contains("barmm") || searchString.contains("bangsamoro") || searchString.contains("basilan") || searchString.contains("lanao del sur") || searchString.contains("maguindanao") || searchString.contains("sulu") || searchString.contains("tawi-tawi")) {
            return Pair("BARMM (Bangsamoro)", IslandGroup.MINDANAO)
        }
        
        return Pair(null, null)
    }
}
