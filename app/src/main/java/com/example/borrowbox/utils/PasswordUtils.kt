package com.example.borrowbox.utils

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordVerificationResult(
    val isValid: Boolean,
    val needsUpgrade: Boolean
)

object PasswordUtils {
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_PREFIX = "\$pbkdf2\$120000\$"

    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = hexToByteArray(saltHex)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashBytes = factory.generateSecret(spec).encoded
        val hashHex = hashBytes.joinToString("") { "%02x".format(it) }
        return "$PBKDF2_PREFIX$hashHex"
    }

    fun hashPasswordSha256Legacy(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = password + saltHex
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, storedHash: String, saltHex: String): PasswordVerificationResult {
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            val calculated = hashPassword(password, saltHex)
            val isValid = calculated.equals(storedHash, ignoreCase = true)
            return PasswordVerificationResult(isValid = isValid, needsUpgrade = false)
        } else {
            // Legacy SHA-256 check
            val legacyCalculated = hashPasswordSha256Legacy(password, saltHex)
            val isValid = legacyCalculated.equals(storedHash, ignoreCase = true)
            return PasswordVerificationResult(isValid = isValid, needsUpgrade = isValid)
        }
    }

    fun hashPin(pin: String, saltHex: String): String {
        return hashPassword(pin, saltHex)
    }

    fun verifyPin(enteredPin: String, storedPinOrHash: String?, saltHex: String): Boolean {
        if (storedPinOrHash == null) return false
        if (storedPinOrHash.length <= 6 && storedPinOrHash.all { it.isDigit() }) {
            // Plaintext legacy PIN
            return enteredPin == storedPinOrHash
        }
        val result = verifyPassword(enteredPin, storedPinOrHash, saltHex)
        return result.isValid
    }
}
