// This file handles all user authentication for the app.
// Any screen that needs to log in or log out a user will use this class.

package com.liquor.ledger.firebase

// FirebaseUser is the currently logged-in user
import com.google.firebase.auth.FirebaseUser

// Used to suspend functions for async Firebase operations
import kotlinx.coroutines.tasks.await

class AuthRepository
{

    // Gets the auth instance from FirebaseManager
    private val auth = FirebaseManager.auth

    // Returns the currently logged-in user
    // Returns null if no one is logged in
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // Logs in a user with their email and password
    // suspend runs asynchronously without freezing the UI
    suspend fun login(email: String, password: String): Result<FirebaseUser>
    {

        return try
        {

            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()
            Result.success(result.user!!)

        }

        catch (e: Exception)
        {

            Result.failure(e)

        }
    }

    // Logs out the currently logged-in user
    fun logout() = auth.signOut()

}
