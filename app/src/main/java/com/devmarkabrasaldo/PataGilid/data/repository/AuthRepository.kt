package com.devmarkabrasaldo.PataGilid.data.repository

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val adminEmails = setOf(
        "abrasaldomark@gmail.com"
    )

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    val isAdmin: Boolean
        get() {
            val email = auth.currentUser?.email?.lowercase() ?: return false
            return adminEmails.contains(email)
        }

    val userDisplayName: String
        get() = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@") ?: "Mountaineer"

    val userPhotoUrl: String?
        get() = auth.currentUser?.photoUrl?.toString()

    fun getGoogleSignInClient(clientContext: Context = context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId())
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        return GoogleSignIn.getClient(clientContext, gso)
    }

    private fun getWebClientId(): String {
        // Retrieve default Web Client ID generated from google-services.json string resources
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (id != 0) context.getString(id) else "123456789012-00000000000000000000000000000000.apps.googleusercontent.com"
    }

    suspend fun signInWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).await()
    }

    suspend fun signOut(clientContext: Context = context) {
        getGoogleSignInClient(clientContext).signOut().await()
        auth.signOut()
    }

    suspend fun fetchDriveOAuthToken(): String? = withContext(Dispatchers.IO) {
        try {
            val email = auth.currentUser?.email ?: GoogleSignIn.getLastSignedInAccount(context)?.email
            if (email != null) {
                val account = Account(email, "com.google")
                return@withContext GoogleAuthUtil.getToken(
                    context,
                    account,
                    "oauth2:https://www.googleapis.com/auth/drive.file"
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to retrieve Google Drive OAuth token: ${e.message}", e)
        }
        null
    }
}
