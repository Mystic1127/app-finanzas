package com.example.finanzas.data.cloud

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.finanzas.data.local.LocalRepository
import com.example.finanzas.util.Prefs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    const val ACTION_SYNC_RESTORED = "com.example.finanzas.cloud.ACTION_SYNC_RESTORED"
    private const val PREFS = "spendly_cloud_sync"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LAST_REMOTE_PREFIX = "last_remote_"
    private const val KEY_LAST_VERSION_PREFIX = "last_version_"
    private const val FIELD_PAYLOAD = "payload"
    private const val FIELD_UPDATED_AT = "updatedAtMillis"
    private const val FIELD_DEVICE_ID = "deviceId"
    private const val FIELD_EMAIL = "email"
    private const val FIELD_DISPLAY_NAME = "displayName"
    private const val FIELD_SCHEMA_VERSION = "schemaVersion"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class Result(
        val ok: Boolean,
        val restoredFromCloud: Boolean = false,
        val uploadedToCloud: Boolean = false,
        val message: String = ""
    )

    @JvmStatic
    fun scheduleUpload(context: Context) {
        val appContext = context.applicationContext
        if (FirebaseAuth.getInstance().currentUser == null || !Prefs.isLoggedIn(appContext)) return
        scope.launch {
            runCatching { uploadNow(appContext) }
                .onFailure { Log.w(TAG, "No se pudo subir snapshot a Firestore", it) }
        }
    }

    @JvmStatic
    fun scheduleSync(context: Context) {
        val appContext = context.applicationContext
        if (FirebaseAuth.getInstance().currentUser == null || !Prefs.isLoggedIn(appContext)) return
        scope.launch {
            runCatching { syncAfterLogin(appContext) }
                .onFailure { Log.w(TAG, "No se pudo sincronizar con Firestore", it) }
        }
    }

    suspend fun syncAfterLogin(context: Context): Result = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val user = FirebaseAuth.getInstance().currentUser
            ?: return@withContext Result(false, message = "No hay sesión de Google activa")
        val repo = LocalRepository.getInstance(appContext)
        val docRef = syncDocument(user.uid)
        val remote = docRef.get().await()
        val remoteUpdatedAt = remote.getLong(FIELD_UPDATED_AT) ?: 0L
        val localHasData = repo.currentUserHasSyncableData()
        val localVersion = LocalRepository.getDataVersion()
        val syncPrefs = prefs(appContext)
        val lastSyncedVersion = syncPrefs.getLong(lastVersionKey(user.uid), -1L)
        val lastRemote = syncPrefs.getLong(lastRemoteKey(user.uid), 0L)
        val localDirty = localVersion != lastSyncedVersion

        if (remote.exists() && remoteUpdatedAt > 0L && (!localHasData || (!localDirty && remoteUpdatedAt > lastRemote))) {
            val payload = remote.getString(FIELD_PAYLOAD).orEmpty()
            if (payload.isNotBlank()) {
                repo.importCurrentUserSyncSnapshot(JSONObject(decode(payload)))
                syncPrefs.edit()
                    .putLong(lastRemoteKey(user.uid), remoteUpdatedAt)
                    .putLong(lastVersionKey(user.uid), LocalRepository.getDataVersion())
                    .apply()
                appContext.sendBroadcast(Intent(ACTION_SYNC_RESTORED).setPackage(appContext.packageName))
                return@withContext Result(true, restoredFromCloud = true, message = "Datos restaurados desde la nube")
            }
        }

        if (localHasData) {
            uploadNow(appContext)
            Result(true, uploadedToCloud = true, message = "Datos sincronizados con la nube")
        } else {
            Result(true, message = "Sesión conectada")
        }
    }

    suspend fun uploadNow(context: Context): Result = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val user = FirebaseAuth.getInstance().currentUser
            ?: return@withContext Result(false, message = "No hay sesión de Google activa")
        if (Prefs.getCurrentUserId(appContext) <= 0) {
            return@withContext Result(false, message = "No hay usuario local activo")
        }
        val snapshot = LocalRepository.getInstance(appContext).exportCurrentUserSyncSnapshot()
        val now = System.currentTimeMillis()
        syncDocument(user.uid).set(
            mapOf(
                FIELD_PAYLOAD to encode(snapshot.toString()),
                FIELD_UPDATED_AT to now,
                FIELD_DEVICE_ID to deviceId(appContext),
                FIELD_EMAIL to (user.email ?: Prefs.getCurrentUserEmail(appContext)),
                FIELD_DISPLAY_NAME to (user.displayName ?: Prefs.getCurrentUserName(appContext)),
                FIELD_SCHEMA_VERSION to 1
            )
        ).await()
        prefs(appContext).edit()
            .putLong(lastRemoteKey(user.uid), now)
            .putLong(lastVersionKey(user.uid), LocalRepository.getDataVersion())
            .apply()
        Result(true, uploadedToCloud = true, message = "Datos subidos a Firestore")
    }

    private fun syncDocument(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("sync")
            .document("current")

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun deviceId(context: Context): String {
        val sp = prefs(context)
        val existing = sp.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        sp.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }

    private fun lastRemoteKey(uid: String) = KEY_LAST_REMOTE_PREFIX + uid

    private fun lastVersionKey(uid: String) = KEY_LAST_VERSION_PREFIX + uid

    private fun encode(raw: String): String {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw.toByteArray(Charsets.UTF_8)) }
        return Base64.getEncoder().encodeToString(out.toByteArray())
    }

    private fun decode(payload: String): String {
        val bytes = Base64.getDecoder().decode(payload)
        return GZIPInputStream(ByteArrayInputStream(bytes)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}
