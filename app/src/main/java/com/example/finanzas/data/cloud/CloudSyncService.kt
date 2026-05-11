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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CloudSyncService {
    private const val TAG = "CloudSyncService"
    const val ACTION_SYNC_RESTORED = "com.example.finanzas.cloud.ACTION_SYNC_RESTORED"
    const val EXTRA_SHOW_RESTORE_NOTICE = "show_restore_notice"
    private const val PREFS = "spendly_cloud_sync"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_LAST_REMOTE_PREFIX = "last_remote_"
    private const val KEY_LAST_VERSION_PREFIX = "last_version_"
    private const val KEY_DIRTY_PREFIX = "dirty_"
    private const val KEY_RESTORE_NOTICE_REMOTE_PREFIX = "restore_notice_remote_"
    private const val FIELD_PAYLOAD = "payload"
    private const val FIELD_UPDATED_AT = "updatedAtMillis"
    private const val FIELD_DEVICE_ID = "deviceId"
    private const val FIELD_EMAIL = "email"
    private const val FIELD_DISPLAY_NAME = "displayName"
    private const val FIELD_SCHEMA_VERSION = "schemaVersion"
    private const val FIELD_UID = "uid"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncInFlight = AtomicBoolean(false)

    data class Result(
        val ok: Boolean,
        val restoredFromCloud: Boolean = false,
        val uploadedToCloud: Boolean = false,
        val message: String = ""
    )

    const val MESSAGE_REMOTE_HAS_DATA = "REMOTE_HAS_DATA"

    @JvmStatic
    @JvmOverloads
    fun scheduleUpload(context: Context, markDirty: Boolean = true) {
        val appContext = context.applicationContext
        val uid = activeFirebaseUid(appContext) ?: return
        if (markDirty) {
            prefs(appContext).edit().putBoolean(dirtyKey(uid), true).apply()
        }
        scope.launch {
            runCatching { uploadNow(appContext) }
                .onFailure { Log.w(TAG, "No se pudo subir snapshot a Firestore", it) }
        }
    }

    @JvmStatic
    fun scheduleSync(context: Context) {
        val appContext = context.applicationContext
        activeFirebaseUid(appContext) ?: return
        scope.launch {
            runCatching { syncAfterLogin(appContext) }
                .onFailure { Log.w(TAG, "No se pudo sincronizar con Firestore", it) }
        }
    }

    @JvmStatic
    fun isSyncInProgress(): Boolean = syncInFlight.get()

    suspend fun syncAfterLogin(context: Context): Result = withContext(Dispatchers.IO) {
        if (!syncInFlight.compareAndSet(false, true)) {
            return@withContext Result(true, message = "Sincronización ya en curso")
        }
        val startedAt = System.currentTimeMillis()
        try {
            val appContext = context.applicationContext
            val uid = activeFirebaseUid(appContext)
                ?: return@withContext Result(false, message = "No hay sesión de Google activa")
            val user = FirebaseAuth.getInstance().currentUser
                ?: return@withContext Result(false, message = "No hay sesión de Google activa")
            val repo = LocalRepository.getInstance(appContext)
            val docRef = syncDocument(uid)
            val remote = docRef.get().await()
            val remoteUpdatedAt = remote.getLong(FIELD_UPDATED_AT) ?: 0L
            val localHasData = repo.currentUserHasSyncableData()
            val syncPrefs = prefs(appContext)
            val lastRemote = syncPrefs.getLong(lastRemoteKey(uid), 0L)
            val localDirty = syncPrefs.getBoolean(dirtyKey(uid), false) || (lastRemote <= 0L && localHasData)

            if (remote.exists() && remoteUpdatedAt > 0L && (!localHasData || (!localDirty && remoteUpdatedAt > lastRemote))) {
                val payload = remote.getString(FIELD_PAYLOAD).orEmpty()
                if (payload.isNotBlank()) {
                    val remoteSnapshot = JSONObject(decode(payload))
                    val localSnapshot = repo.exportCurrentUserSyncSnapshot()
                    val hasLocalChanges = snapshotsDiffer(localSnapshot, remoteSnapshot)
                    if (!hasLocalChanges) {
                        syncPrefs.edit()
                            .putLong(lastRemoteKey(uid), remoteUpdatedAt)
                            .putLong(lastVersionKey(uid), LocalRepository.getDataVersion())
                            .putBoolean(dirtyKey(uid), false)
                            .apply()
                        Log.d(TAG, "Sync remote unchanged uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
                        return@withContext Result(true, message = "Datos locales al dia")
                    }
                    val remoteDeviceId = remote.getString(FIELD_DEVICE_ID).orEmpty()
                    val showNotice = shouldShowRestoreNotice(syncPrefs, uid, remoteUpdatedAt, lastRemote, remoteDeviceId, deviceId(appContext))
                    repo.importCurrentUserSyncSnapshot(remoteSnapshot)
                    syncPrefs.edit()
                        .putLong(lastRemoteKey(uid), remoteUpdatedAt)
                        .putLong(restoreNoticeRemoteKey(uid), if (showNotice) remoteUpdatedAt else syncPrefs.getLong(restoreNoticeRemoteKey(uid), 0L))
                        .putLong(lastVersionKey(uid), LocalRepository.getDataVersion())
                        .putBoolean(dirtyKey(uid), false)
                        .apply()
                    appContext.sendBroadcast(
                        Intent(ACTION_SYNC_RESTORED)
                            .setPackage(appContext.packageName)
                            .putExtra(EXTRA_SHOW_RESTORE_NOTICE, showNotice)
                    )
                    Log.d(TAG, "Sync restore uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
                    return@withContext Result(true, restoredFromCloud = true, message = "Datos restaurados desde la nube")
                }
            }

            if (remote.exists() && remoteUpdatedAt > 0L && !localDirty && remoteUpdatedAt == lastRemote) {
                Log.d(TAG, "Sync cached uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
                return@withContext Result(true, message = "Datos locales al día")
            }

            if (localHasData) {
                uploadNow(appContext)
                Log.d(TAG, "Sync upload uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
                Result(true, uploadedToCloud = true, message = "Datos sincronizados con la nube")
            } else {
                Log.d(TAG, "Sync noop uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
                Result(true, message = "Sesión conectada")
            }
        } finally {
            syncInFlight.set(false)
        }
    }

    suspend fun uploadNow(context: Context): Result = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val appContext = context.applicationContext
        val uid = activeFirebaseUid(appContext)
            ?: return@withContext Result(false, message = "No hay sesión de Google activa")
        val user = FirebaseAuth.getInstance().currentUser
            ?: return@withContext Result(false, message = "No hay sesión de Google activa")
        if (Prefs.getCurrentUserId(appContext) <= 0) {
            return@withContext Result(false, message = "No hay usuario local activo")
        }
        val snapshot = LocalRepository.getInstance(appContext).exportCurrentUserSyncSnapshot()
        val now = System.currentTimeMillis()
        syncDocument(uid).set(
            mapOf(
                FIELD_PAYLOAD to encode(snapshot.toString()),
                FIELD_UPDATED_AT to now,
                FIELD_DEVICE_ID to deviceId(appContext),
                FIELD_UID to uid,
                FIELD_EMAIL to (user.email ?: Prefs.getCurrentUserEmail(appContext)),
                FIELD_DISPLAY_NAME to (user.displayName ?: Prefs.getCurrentUserName(appContext)),
                FIELD_SCHEMA_VERSION to 1
            )
        ).await()
        prefs(appContext).edit()
            .putLong(lastRemoteKey(uid), now)
            .putLong(lastVersionKey(uid), LocalRepository.getDataVersion())
            .putBoolean(dirtyKey(uid), false)
            .apply()
        Log.d(TAG, "Upload uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
        Result(true, uploadedToCloud = true, message = "Datos subidos a Firestore")
    }

    suspend fun uploadLocalSnapshotForLink(
        context: Context,
        uid: String,
        email: String?,
        displayName: String?,
        allowOverwrite: Boolean
    ): Result = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val appContext = context.applicationContext
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext Result(false, message = "No hay sesion de Google activa")
        if (firebaseUid != uid) {
            return@withContext Result(false, message = "La sesion de Google no coincide")
        }
        if (Prefs.getCurrentUserId(appContext) <= 0) {
            return@withContext Result(false, message = "No hay usuario local activo")
        }

        val remote = try {
            syncDocument(uid).get().await()
        } catch (_: Exception) {
            return@withContext Result(false, message = "No se pudo conectar con Firestore. Tus datos locales se conservan.")
        }
        val remoteHasData = remote.exists() && remoteSnapshotHasSyncableData(remote.getString(FIELD_PAYLOAD).orEmpty())
        if (remoteHasData && !allowOverwrite) {
            Log.w(TAG, "Link blocked because remote snapshot has data uidHash=${uid.hashCode()}")
            return@withContext Result(false, message = MESSAGE_REMOTE_HAS_DATA)
        }

        prefs(appContext).edit().putBoolean(dirtyKey(uid), true).apply()
        val snapshot = LocalRepository.getInstance(appContext).exportCurrentUserSyncSnapshot()
        val now = System.currentTimeMillis()
        try {
            syncDocument(uid).set(
                mapOf(
                    FIELD_PAYLOAD to encode(snapshot.toString()),
                    FIELD_UPDATED_AT to now,
                    FIELD_DEVICE_ID to deviceId(appContext),
                    FIELD_UID to uid,
                    FIELD_EMAIL to (email ?: Prefs.getCurrentUserEmail(appContext)),
                    FIELD_DISPLAY_NAME to (displayName ?: Prefs.getCurrentUserName(appContext)),
                    FIELD_SCHEMA_VERSION to 1
                )
            ).await()
        } catch (_: Exception) {
            return@withContext Result(false, message = "No se pudo subir a Firestore. Tus datos locales se conservan.")
        }
        prefs(appContext).edit()
            .putLong(lastRemoteKey(uid), now)
            .putLong(lastVersionKey(uid), LocalRepository.getDataVersion())
            .putBoolean(dirtyKey(uid), false)
            .apply()
        Log.d(TAG, "Link upload uidHash=${uid.hashCode()} elapsedMs=${System.currentTimeMillis() - startedAt}")
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

    private fun dirtyKey(uid: String) = KEY_DIRTY_PREFIX + uid

    private fun restoreNoticeRemoteKey(uid: String) = KEY_RESTORE_NOTICE_REMOTE_PREFIX + uid

    private fun shouldShowRestoreNotice(
        syncPrefs: android.content.SharedPreferences,
        uid: String,
        remoteUpdatedAt: Long,
        lastRemote: Long,
        remoteDeviceId: String,
        localDeviceId: String
    ): Boolean {
        if (remoteUpdatedAt <= 0L) return false
        if (remoteUpdatedAt <= syncPrefs.getLong(restoreNoticeRemoteKey(uid), 0L)) return false
        return lastRemote <= 0L || remoteDeviceId.isBlank() || remoteDeviceId != localDeviceId
    }

    private fun snapshotsDiffer(local: JSONObject, remote: JSONObject): Boolean {
        local.remove("exportedAt")
        remote.remove("exportedAt")
        return canonical(local) != canonical(remote)
    }

    private fun canonical(value: Any?): String {
        return when (value) {
            null -> "null"
            JSONObject.NULL -> "null"
            is JSONObject -> {
                val keys = value.keys().asSequence().toList().sorted()
                keys.joinToString(prefix = "{", postfix = "}") { key ->
                    "\"$key\":${canonical(value.opt(key))}"
                }
            }
            is JSONArray -> {
                (0 until value.length()).joinToString(prefix = "[", postfix = "]") { index ->
                    canonical(value.opt(index))
                }
            }
            is String -> JSONObject.quote(value)
            is Number, is Boolean -> value.toString()
            else -> JSONObject.quote(value.toString())
        }
    }

    private fun activeFirebaseUid(context: Context): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val uid = user.uid
        val usesPasswordProvider = user.providerData.any { it.providerId == "password" }
        if (usesPasswordProvider && !user.isEmailVerified) return null
        val token = Prefs.getToken(context.applicationContext) ?: return null
        val localUserId = Prefs.getCurrentUserId(context.applicationContext)
        if (localUserId <= 0) return null
        val linkedUid = Prefs.getFirebaseUidForUser(context.applicationContext, localUserId)
        return if (token == "firebase:$uid" && linkedUid == uid) uid else null
    }

    private fun remoteSnapshotHasSyncableData(payload: String): Boolean {
        if (payload.isBlank()) return false
        return runCatching {
            val snapshot = JSONObject(decode(payload))
            val tables = snapshot.optJSONObject("tables") ?: JSONObject()
            val dataTables = listOf(
                "transacciones",
                "transacciones_recurrentes",
                "presupuestos",
                "presupuestos_categoria",
                "metas",
                "metas_hitos",
                "recordatorios",
                "import_jobs",
                "import_rules"
            )
            if (dataTables.any { (tables.optJSONArray(it)?.length() ?: 0) > 0 }) return true

            val categories = tables.optJSONArray("categorias") ?: JSONArray()
            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue
                if (category.optInt("userId", 0) != 0) return true
            }

            val settings = snapshot.optJSONObject("settings") ?: return false
            if ((settings.optJSONObject("categoryMeta")?.length() ?: 0) > 0) return true
            if (settingsObjectHasEntries(settings.optJSONObject("initialBalances"))) return true
            if (settingsObjectHasEntries(settings.optJSONObject("financialAccounts"))) return true
            val labels = settings.optJSONObject("transactionLabels")
            if ((labels?.optJSONArray("labels")?.length() ?: 0) > 0) return true
            if ((labels?.optJSONObject("assignments")?.length() ?: 0) > 0) return true
            false
        }.getOrDefault(true)
    }

    private fun settingsObjectHasEntries(value: JSONObject?): Boolean {
        if (value == null || value.length() == 0) return false
        if (value.optBoolean("configured", false)) return true
        val keys = value.keys()
        while (keys.hasNext()) {
            val item = value.opt(keys.next())
            when (item) {
                is JSONArray -> if (item.length() > 0) return true
                is JSONObject -> if (item.length() > 0) return true
                is Number -> if (item.toDouble() != 0.0) return true
                is String -> if (item.isNotBlank()) return true
                is Boolean -> if (item) return true
            }
        }
        return false
    }

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
