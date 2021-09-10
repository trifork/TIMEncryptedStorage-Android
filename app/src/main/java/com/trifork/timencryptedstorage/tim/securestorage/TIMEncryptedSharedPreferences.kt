package com.trifork.timencryptedstorage.tim.securestorage

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.trifork.timencryptedstorage.models.TIMResult
import com.trifork.timencryptedstorage.models.TIMSecureStorageError
import com.trifork.timencryptedstorage.shared.bll.asPreservedByteArray
import com.trifork.timencryptedstorage.shared.bll.asPreservedString
import java.io.IOException
import java.security.GeneralSecurityException

private const val TIMEncryptedSharedPreferencesName = "TIMESP"

class TIMEncryptedSharedPreferences(context: Context) : TIMSecureStorage {

    /**
     * An instance of [EncryptedSharedPreferences]
     */
    private val sharedPreferences: SharedPreferences

    init {
        val appContext = context.applicationContext

        // If Tink decides to generate a brand new keyset, it will throw an Exception when trying to create the EncryptedSharedPrefs
        // ("AndroidKeysetManager: keyset not found, will generate a new one")
        // See: https://github.com/google/tink/issues/436
        sharedPreferences = try {
            getEncryptedPrefs(appContext)
        } catch (e: Throwable) {
            Log.e(
                TIMEncryptedSharedPreferences::class.java.simpleName,
                "Failed to get encrypted shared preferences, attempting to recover with recreation",
                e
            )
            // If this happens, we clear the old storage and recreate it, there is no recovery
            recreateEncryptedPrefs(appContext)
        }
    }

    private fun performEditAndCommit(action: (SharedPreferences.Editor) -> Unit) {
        sharedPreferences.edit(commit = true, action)
    }

    override fun remove(entryKey: String) =
        performEditAndCommit { it.remove(entryKey) }

    override fun store(
        data: ByteArray,
        entryKey: String
    ) = performEditAndCommit {
        it.putString(entryKey, data.asPreservedString)
    }

    override fun get(entryKey: String): TIMResult<ByteArray, TIMSecureStorageError> {
        val dataString = sharedPreferences.getString(entryKey, null)
        return when (dataString) {
            null -> TIMResult.Failure(TIMSecureStorageError.FailedToLoadData(Throwable("No data found for key $entryKey")))
            else -> TIMResult.Success(dataString.asPreservedByteArray)
        }
    }

    override fun hasValue(entryKey: String): Boolean = sharedPreferences.contains(entryKey)


    //region EncryptedSharedPreferences
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getEncryptedPrefs(appContext: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            appContext,
            TIMEncryptedSharedPreferencesName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    @SuppressLint("ApplySharedPref")
    private fun recreateEncryptedPrefs(appContext: Context): SharedPreferences {
        // Clear the data, since the data is either tampered, broken or Tink has created a new KeySet and we cannot recover
        appContext.getSharedPreferences(TIMEncryptedSharedPreferencesName, Context.MODE_PRIVATE)
            .edit().clear().commit()
        return getEncryptedPrefs(appContext)
    }
    //endregion

}