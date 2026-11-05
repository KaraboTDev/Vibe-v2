package com.karabotshivhase.vibe.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleSsoHelper(
    private val context: Context,
    private val webClientId: String = WEB_CLIENT_ID,
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun getGoogleIdToken(): Result<String> {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleIdTokenCredential.idToken)
            } else {
                Result.failure(IllegalStateException("Invalid credential type returned."))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e("GoogleSsoHelper", "GetCredentialException: ${e.type} - ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleSsoHelper", "Unexpected exception during Google Sign-In", e)
            Result.failure(e)
        }
    }

    companion object {
        const val WEB_CLIENT_ID = "158589057073-gs8p16m6thqpk2q40r8ktk5os3hiqsb5.apps.googleusercontent.com"
    }
}
