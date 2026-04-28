package com.example.finanzas.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val PREFIX = "pbkdf2"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    @JvmStatic
    fun hashPassword(rawPassword: String): String {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(rawPassword, salt, ITERATIONS, KEY_LENGTH)

        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hashB64 = Base64.getEncoder().encodeToString(hash)
        return "$PREFIX$$ITERATIONS$$saltB64$$hashB64"
    }

    @JvmStatic
    fun verifyPassword(rawPassword: String, storedValue: String?): Boolean {
        if (storedValue.isNullOrBlank()) return false
        if (!looksLikeHashed(storedValue)) {
            return rawPassword == storedValue
        }

        return try {
            val parts = storedValue.split('$')
            if (parts.size != 4) return false

            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[3])
            val actual = pbkdf2(rawPassword, salt, iterations, expected.size * 8)
            expected.contentEquals(actual)
        } catch (_: Exception) {
            false
        }
    }

    @JvmStatic
    fun looksLikeHashed(value: String?): Boolean {
        return !value.isNullOrBlank() && value.startsWith("$PREFIX$")
    }

    private fun pbkdf2(password: String, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }
}
