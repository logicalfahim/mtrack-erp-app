package com.mtrack.installer.util

/**
 * Smart OCR Parser for Bangladeshi Smart NID & Classic NID Cards
 */
object NidOcrParser {

    data class NidFrontResult(
        val fullName: String = "",
        val nidNumber: String = ""
    )

    data class NidBackResult(
        val address: String = ""
    )

    /**
     * Parse Front Side: Extracts English Full Name and NID Number (10, 13, or 17 digits)
     */
    fun parseFront(text: String): NidFrontResult {
        var name = ""
        var nid = ""

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Find NID number (Smart card: 10 digits; Old NID: 17 or 13 digits)
        val nidRegex = Regex("""\b(\d{17}|\d{13}|\d{10})\b""")
        for (line in lines) {
            val match = nidRegex.find(line)
            if (match != null) {
                nid = match.value
                break
            }
        }

        // Find Name: Look for "Name:" or line in English capital letters
        for (i in lines.indices) {
            val line = lines[i]
            if (line.startsWith("Name:", ignoreCase = true) || line.startsWith("Name", ignoreCase = true)) {
                val candidate = line.substringAfter("Name").removePrefix(":").trim()
                if (candidate.length >= 3 && !candidate.contains("Bangladesh", ignoreCase = true)) {
                    name = candidate
                    break
                } else if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    if (nextLine.matches(Regex("""^[A-Za-z\s\.\-]{3,}$""")) && !nextLine.contains("Government", ignoreCase = true)) {
                        name = nextLine
                        break
                    }
                }
            } else if (name.isEmpty() && line.matches(Regex("""^[A-Z\s\.\-]{4,}$""")) && 
                !line.contains("BANGLADESH") && !line.contains("NATIONAL") && !line.contains("CARD") && 
                !line.contains("GOVERNMENT") && !line.contains("REPUBLIC") && !line.contains("PEOPLE")) {
                name = line
            }
        }

        return NidFrontResult(fullName = name, nidNumber = nid)
    }

    /**
     * Parse Back Side: Extracts Full Billing Address (ঠিকানা / Address lines)
     */
    fun parseBack(text: String): NidBackResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val addressLines = mutableListOf<String>()
        var recordingAddress = false

        for (line in lines) {
            if (line.contains("ঠিকানা", ignoreCase = true) || line.contains("Address", ignoreCase = true)) {
                recordingAddress = true
                val remaining = line.substringAfter("ঠিকানা").substringAfter("Address").removePrefix(":").trim()
                if (remaining.isNotEmpty()) {
                    addressLines.add(remaining)
                }
            } else if (recordingAddress) {
                if (line.contains("রক্তের গ্রুপ", ignoreCase = true) || line.contains("Blood Group", ignoreCase = true) || 
                    line.contains("প্রদানের তারিখ", ignoreCase = true) || line.contains("Date of Issue", ignoreCase = true)) {
                    recordingAddress = false
                } else {
                    addressLines.add(line)
                }
            }
        }

        val compiledAddress = if (addressLines.isNotEmpty()) {
            addressLines.joinToString(", ")
        } else {
            // Heuristic fallback for common Bangladeshi address terms
            lines.filter { 
                it.contains("ঢাকা") || it.contains("গ্রাম") || it.contains("রোড") || 
                it.contains("ডাকঘর") || it.contains("থানা") || it.contains("জেলা") ||
                it.contains("Road") || it.contains("House") || it.contains("Sector") || it.contains("Dhaka") 
            }.joinToString(", ")
        }

        return NidBackResult(address = compiledAddress)
    }
}
