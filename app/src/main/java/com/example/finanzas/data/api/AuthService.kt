package com.example.finanzas.data.api

import android.content.Context
import com.example.finanzas.data.cloud.CloudSyncService
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.util.Prefs
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

data class AuthResult(
    val token: String,
    val userId: Int,
    val nombre: String?,
    val email: String?,
    val isNewUser: Boolean = false,
    val emailVerificationSent: Boolean = false,
    val emailVerified: Boolean = true
)

data class PasswordResetResult(
    val ok: Boolean,
    val message: String
)

class AuthFailure(message: String) : Exception(message)

object AuthService {

    suspend fun login(ctx: Context, email: String, pass: String): AuthResult? = withContext(Dispatchers.IO) {
        val firebaseUser = try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), pass).await().user
        } catch (_: Exception) {
            null
        } ?: return@withContext null

        runCatching { firebaseUser.reload().await() }
        if (!firebaseUser.isEmailVerified) {
            return@withContext AuthResult(
                token = "",
                userId = -1,
                nombre = firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                email = firebaseUser.email ?: email.trim(),
                emailVerified = false
            )
        }

        val cleanEmail = firebaseUser.email ?: email.trim()
        val localUser = getOrCreateFirebaseLocalUser(ctx, firebaseUser, cleanEmail)
        Prefs.setFirebaseLink(ctx.applicationContext, localUser.id.toLong(), firebaseUser.uid, cleanEmail)
        AuthResult(
            token = "firebase:${firebaseUser.uid}",
            userId = localUser.id,
            nombre = localUser.nombre,
            email = localUser.email,
            isNewUser = false,
            emailVerified = firebaseUser.isEmailVerified
        )
    }

    suspend fun continueWithoutAccount(ctx: Context, nombre: String): AuthResult? = withContext(Dispatchers.IO) {
        val cleanName = nombre.trim().ifBlank { "Invitado" }
        val repo = LocalRepository.getInstance(ctx)
        val existing = repo.listUsers().firstOrNull { it.nombre.equals(cleanName, ignoreCase = true) }
        if (existing != null) {
            return@withContext AuthResult("local-token", existing.id, existing.nombre, "")
        }

        val outId = IntArray(1)
        val ok = repo.registerUser(
            cleanName,
            "guest-${UUID.randomUUID()}@spendly.local",
            UUID.randomUUID().toString(),
            outId
        )
        if (!ok) null else AuthResult(
            token = "local-token",
            userId = outId[0],
            nombre = cleanName,
            email = "",
            isNewUser = true
        )
    }

    suspend fun register(ctx: Context, nombre: String, email: String, pass: String): AuthResult? = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()

        val firebaseUser = try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(cleanEmail, pass).await().user
        } catch (_: FirebaseAuthUserCollisionException) {
            throw AuthFailure(duplicateAccountMessage(cleanEmail))
        } catch (_: FirebaseAuthWeakPasswordException) {
            throw AuthFailure("Usa una contrasena mas segura.")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            throw AuthFailure("Ingresa un correo valido.")
        } catch (_: FirebaseNetworkException) {
            throw AuthFailure("No se pudo conectar con Firebase. Revisa tu conexion e intenta otra vez.")
        } catch (error: FirebaseAuthException) {
            throw AuthFailure(registerErrorMessage(error))
        } catch (error: Exception) {
            throw AuthFailure(error.message?.takeIf { it.isNotBlank() }
                ?: "No se pudo registrar. Revisa tu conexion e intenta otra vez.")
        } ?: throw AuthFailure("No se pudo registrar. Revisa tu conexion e intenta otra vez.")

        val verificationSent = runCatching { firebaseUser.sendEmailVerification().await() }.isSuccess
        AuthResult(
            token = "",
            userId = -1,
            nombre = nombre,
            email = cleanEmail,
            isNewUser = true,
            emailVerificationSent = verificationSent,
            emailVerified = firebaseUser.isEmailVerified
        )
    }

    suspend fun loginWithGoogle(ctx: Context, firebaseUser: FirebaseUser): AuthResult? = withContext(Dispatchers.IO) {
        val email = firebaseUser.email ?: return@withContext null
        val appContext = ctx.applicationContext
        val repo = LocalRepository.getInstance(ctx)
        val linkedUserId = Prefs.getLinkedUserIdForFirebaseUid(appContext, firebaseUser.uid)
        val linkedUser = if (linkedUserId > 0) {
            repo.getUserById(linkedUserId.toInt())
        } else {
            null
        }
        val localUser = linkedUser ?: repo.createFirebaseUserForUid(firebaseUser.displayName, email, firebaseUser.uid)
        Prefs.setFirebaseLink(appContext, localUser.id.toLong(), firebaseUser.uid, email)
        AuthResult("firebase:${firebaseUser.uid}", localUser.id, localUser.nombre, email, linkedUser == null)
    }

    suspend fun completeVerifiedEmailSession(ctx: Context, fallbackName: String? = null): AuthResult? = withContext(Dispatchers.IO) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return@withContext null
        runCatching { firebaseUser.reload().await() }
        if (!firebaseUser.isEmailVerified) {
            return@withContext AuthResult(
                token = "",
                userId = -1,
                nombre = fallbackName ?: firebaseUser.displayName ?: firebaseUser.email?.substringBefore('@'),
                email = firebaseUser.email,
                emailVerified = false
            )
        }
        val cleanEmail = firebaseUser.email ?: return@withContext null
        val localUser = getOrCreateFirebaseLocalUser(ctx, firebaseUser, cleanEmail, fallbackName)
        Prefs.setFirebaseLink(ctx.applicationContext, localUser.id.toLong(), firebaseUser.uid, cleanEmail)
        AuthResult(
            token = "firebase:${firebaseUser.uid}",
            userId = localUser.id,
            nombre = localUser.nombre,
            email = cleanEmail,
            isNewUser = false,
            emailVerified = true
        )
    }

    suspend fun syncGoogleAccount(ctx: Context): CloudSyncService.Result =
        CloudSyncService.syncAfterLogin(ctx)

    suspend fun sendPasswordResetEmail(email: String): PasswordResetResult = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return@withContext PasswordResetResult(false, "Ingresa tu correo.")
        }
        try {
            FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
            PasswordResetResult(true, "Te enviamos un correo para restablecer tu contrasena.")
        } catch (_: FirebaseAuthInvalidUserException) {
            PasswordResetResult(false, "Para recuperar por correo, primero debes tener una cuenta vinculada.")
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            PasswordResetResult(false, "Ingresa un correo valido.")
        } catch (_: Exception) {
            PasswordResetResult(false, "No se pudo enviar el correo. Revisa tu conexion e intenta otra vez.")
        }
    }

    suspend fun resendEmailVerification(): PasswordResetResult = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return@withContext PasswordResetResult(false, "No hay sesion de Firebase activa.")
        if (user.isEmailVerified) {
            return@withContext PasswordResetResult(true, "Tu correo ya esta verificado.")
        }
        try {
            user.sendEmailVerification().await()
            PasswordResetResult(true, "Te reenviamos el correo de verificacion.")
        } catch (_: Exception) {
            PasswordResetResult(false, "No se pudo reenviar el correo de verificacion.")
        }
    }

    suspend fun linkCurrentLocalUserWithGoogle(ctx: Context, firebaseUser: FirebaseUser): CloudSyncService.Result =
        withContext(Dispatchers.IO) {
            val appContext = ctx.applicationContext
            val currentUserId = Prefs.getCurrentUserId(appContext)
            if (currentUserId <= 0) {
                return@withContext CloudSyncService.Result(false, message = "No hay usuario local activo")
            }
            val currentLinkedUid = Prefs.getFirebaseUidForUser(appContext, currentUserId)
            val allowOverwrite = currentLinkedUid == firebaseUser.uid
            val uploadResult = CloudSyncService.uploadLocalSnapshotForLink(
                context = appContext,
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                allowOverwrite = allowOverwrite
            )
            if (!uploadResult.ok) return@withContext uploadResult

            Prefs.setFirebaseLink(appContext, currentUserId, firebaseUser.uid, firebaseUser.email)
            Prefs.setToken(appContext, "firebase:${firebaseUser.uid}")
            Prefs.setUserSession(
                appContext,
                currentUserId,
                Prefs.getCurrentUserEmail(appContext),
                Prefs.getCurrentUserName(appContext)
            )
            uploadResult
        }

    private suspend fun duplicateAccountMessage(email: String): String {
        val methods = runCatching {
            FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email).await().signInMethods.orEmpty()
        }.getOrDefault(emptyList())
        return if (methods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD)) {
            "Este correo ya esta vinculado con Google. Usa Continuar con Google."
        } else {
            "Este correo ya esta registrado. Inicia sesion o recupera tu contrasena."
        }
    }

    private suspend fun getOrCreateFirebaseLocalUser(
        ctx: Context,
        firebaseUser: FirebaseUser,
        email: String,
        fallbackName: String? = null
    ): com.example.finanzas.data.model.User {
        val appContext = ctx.applicationContext
        val repo = LocalRepository.getInstance(appContext)
        val linkedUserId = Prefs.getLinkedUserIdForFirebaseUid(appContext, firebaseUser.uid)
        val linkedUser = if (linkedUserId > 0) repo.getUserById(linkedUserId.toInt()) else null
        return linkedUser ?: repo.createFirebaseUserForUid(
            fallbackName ?: firebaseUser.displayName,
            email,
            firebaseUser.uid
        )
    }

    private fun registerErrorMessage(error: FirebaseAuthException): String {
        return when (error.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya esta registrado. Inicia sesion o recupera tu contrasena."
            "ERROR_INVALID_EMAIL" -> "Ingresa un correo valido."
            "ERROR_WEAK_PASSWORD" -> "Usa una contrasena mas segura."
            "ERROR_OPERATION_NOT_ALLOWED" -> "El registro con correo y contrasena no esta habilitado en Firebase."
            "ERROR_NETWORK_REQUEST_FAILED" -> "No se pudo conectar con Firebase. Revisa tu conexion e intenta otra vez."
            else -> error.localizedMessage?.takeIf { it.isNotBlank() }
                ?: "No se pudo registrar. Revisa tu conexion e intenta otra vez."
        }
    }
}
