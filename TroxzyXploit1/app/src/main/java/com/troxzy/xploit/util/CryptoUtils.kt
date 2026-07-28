package com.troxzy.xploit.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    fun md5(input: String): String = hash(input, "MD5")
    fun sha1(input: String): String = hash(input, "SHA-1")
    fun sha256(input: String): String = hash(input, "SHA-256")
    fun sha512(input: String): String = hash(input, "SHA-512")
    fun sha3_256(input: String): String = hash(input, "SHA3-256")

    fun hash(input: String, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun hashBytes(input: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun identifyHashType(hash: String): List<String> {
        val length = hash.length
        val isHex = hash.matches(Regex("^[a-fA-F0-9]+$"))
        val possibleTypes = mutableListOf<String>()
        if (isHex) {
            when (length) {
                32 -> possibleTypes.add("MD5")
                40 -> possibleTypes.add("SHA-1")
                56 -> possibleTypes.add("SHA-224 / SHA3-224")
                64 -> possibleTypes.add("SHA-256 / SHA3-256")
                96 -> possibleTypes.add("SHA-384 / SHA3-384")
                128 -> possibleTypes.add("SHA-512 / SHA3-512")
            }
        }
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            possibleTypes.add("bcrypt")
        }
        if (possibleTypes.isEmpty()) possibleTypes.add("Unknown")
        return possibleTypes
    }

    fun base64Encode(input: String): String =
        android.util.Base64.encodeToString(input.toByteArray(), android.util.Base64.NO_WRAP)

    fun base64Decode(input: String): String =
        String(android.util.Base64.decode(input, android.util.Base64.NO_WRAP))

    fun urlEncode(input: String): String = java.net.URLEncoder.encode(input, "UTF-8")
    fun urlDecode(input: String): String = java.net.URLDecoder.decode(input, "UTF-8")

    fun hexEncode(input: String): String =
        input.toByteArray().joinToString("") { "%02x".format(it) }

    fun hexDecode(input: String): String {
        val result = ByteArray(input.length / 2)
        for (i in result.indices) {
            result[i] = ((input[i * 2].digitToInt(16) shl 4) + input[i * 2 + 1].digitToInt(16)).toByte()
        }
        return String(result)
    }

    fun binaryEncode(input: String): String =
        input.toByteArray().joinToString(" ") { Integer.toBinaryString(it.toInt() and 0xFF).padStart(8, '0') }

    fun binaryDecode(input: String): String {
        val bytes = input.trim().split(" ").map { bin ->
            Integer.parseInt(bin, 2).toByte()
        }.toByteArray()
        return String(bytes)
    }

    fun htmlEncode(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun htmlDecode(input: String): String = input
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")

    fun rot13(input: String): String = input.map { ch ->
        when (ch) {
            in 'a'..'m' -> ch + 13; in 'n'..'z' -> ch - 13
            in 'A'..'M' -> ch + 13; in 'N'..'Z' -> ch - 13
            else -> ch
        }
    }.joinToString("")

    fun morseEncode(input: String): String {
        val morseMap = mapOf(
            'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
            'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
            'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
            'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
            'Y' to "-.--", 'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
            '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
            '8' to "---..", '9' to "----.", ' ' to "/"
        )
        return input.uppercase().map { ch -> morseMap[ch] ?: ch.toString() }.joinToString(" ")
    }

    fun morseDecode(input: String): String {
        val morseMap = mapOf(
            ".-" to 'A', "-..." to 'B', "-.-." to 'C', "-.." to 'D', "." to 'E', "..-." to 'F',
            "--." to 'G', "...." to 'H', ".." to 'I', ".---" to 'J', "-.-" to 'K', ".-.." to 'L',
            "--" to 'M', "-." to 'N', "---" to 'O', ".--." to 'P', "--.-" to 'Q', ".-." to 'R',
            "..." to 'S', "-" to 'T', "..-" to 'U', "...-" to 'V', ".--" to 'W', "-..-" to 'X',
            "-.--" to 'Y', "--.." to 'Z', "-----" to '0', ".----" to '1', "..---" to '2',
            "...--" to '3', "....-" to '4', "....." to '5', "-...." to '6', "--..." to '7',
            "---.." to '8', "----." to '9', "/" to ' '
        )
        return input.split(" ").map { code -> morseMap[code]?.toString() ?: code }.joinToString("")
    }

    fun aesEncrypt(plaintext: String, key: String, mode: String = "AES/CBC/PKCS5Padding"): String {
        val keyBytes = key.toByteArray().let { if (it.size < 16) it.copyOf(16) else it.copyOfRange(0, 16) }
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance(mode)
        if (mode.contains("CBC")) {
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(iv))
            val encrypted = cipher.doFinal(plaintext.toByteArray())
            val combined = iv + encrypted
            return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(plaintext.toByteArray())
            return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        }
    }

    fun aesDecrypt(ciphertext: String, key: String, mode: String = "AES/CBC/PKCS5Padding"): String {
        val keyBytes = key.toByteArray().let { if (it.size < 16) it.copyOf(16) else it.copyOfRange(0, 16) }
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val combined = android.util.Base64.decode(ciphertext, android.util.Base64.NO_WRAP)
        val cipher = Cipher.getInstance(mode)
        if (mode.contains("CBC")) {
            val iv = combined.copyOfRange(0, 16)
            val encrypted = combined.copyOfRange(16, combined.size)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
            return String(cipher.doFinal(encrypted))
        } else {
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            return String(cipher.doFinal(combined))
        }
    }

    fun generatePassword(
        length: Int = 16,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = false
    ): String {
        val upper = if (excludeAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = if (excludeAmbiguous) "abcdefghjkmnpqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
        val numbers = if (excludeAmbiguous) "23456789" else "0123456789"
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val charset = StringBuilder()
        if (includeUpper) charset.append(upper)
        if (includeLower) charset.append(lower)
        if (includeNumbers) charset.append(numbers)
        if (includeSymbols) charset.append(symbols)
        if (charset.isEmpty()) charset.append(lower)
        val random = SecureRandom()
        return (1..length).map { charset[random.nextInt(charset.length)] }.joinToString("")
    }

    fun calculatePasswordStrength(password: String): Int {
        var score = 0
        if (password.length >= 8) score += 1
        if (password.length >= 12) score += 1
        if (password.length >= 16) score += 1
        if (password.any { it.isUpperCase() }) score += 1
        if (password.any { it.isLowerCase() }) score += 1
        if (password.any { it.isDigit() }) score += 1
        if (password.any { !it.isLetterOrDigit() }) score += 1
        if (password.distinct().size > password.length / 2) score += 1
        return minOf(score, 10)
    }

    fun decodeJwt(token: String): Map<String, String> {
        val parts = token.split(".")
        if (parts.size != 3) return mapOf("error" to "Invalid JWT format")
        val result = mutableMapOf<String, String>()
        try {
            val header = String(android.util.Base64.decode(parts[0], android.util.Base64.URL_SAFE))
            result["header"] = header
            val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
            result["payload"] = payload
            result["signature"] = parts[2]
        } catch (e: Exception) {
            result["error"] = "JWT decode failed: ${e.message}"
        }
        return result
    }

    fun autoDetectAndDecode(input: String): Map<String, String> {
        val results = mutableMapOf<String, String>()
        // Try Base64
        try {
            val decoded = base64Decode(input)
            if (decoded.isNotEmpty() && decoded.all { it.code < 128 || it.code > 32 }) {
                results["Base64"] = decoded
            }
        } catch (_: Exception) {}
        // Try URL encoding
        try {
            val decoded = urlDecode(input)
            if (decoded != input) results["URL"] = decoded
        } catch (_: Exception) {}
        // Try Hex
        try {
            if (input.matches(Regex("^[a-fA-F0-9]+$")) && input.length % 2 == 0) {
                results["Hex"] = hexDecode(input)
            }
        } catch (_: Exception) {}
        // Try ROT13
        results["ROT13"] = rot13(input)
        // Try Morse
        if (input.contains(".") || input.contains("-")) {
            try { results["Morse"] = morseDecode(input) } catch (_: Exception) {}
        }
        // Try JWT
        if (input.split(".").size == 3) {
            try { results["JWT"] = decodeJwt(input)["payload"] ?: "" } catch (_: Exception) {}
        }
        // Try HTML
        if (input.contains("&") && input.contains(";")) {
            results["HTML"] = htmlDecode(input)
        }
        return results
    }
}
