package com.liquor.ledger.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.liquor.ledger.Employee
import kotlinx.coroutines.tasks.await

class AuthRepository {

    // Get auth and db instances from FirebaseManager
    private val auth: FirebaseAuth = FirebaseManager.auth
    private val db: FirebaseFirestore = FirebaseManager.db

    // Returns the currently logged-in user or null
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // Login using Employee ID and password
    suspend fun loginWithEmployeeId(
        employeeId: String,
        password: String
    ): Result<Employee> {
        return try {
            // Look up employee by ID in Firestore
            val snapshot = db
                .collection("employees")
                .get()
                .await()

            val matchingDoc = snapshot.documents.firstOrNull { doc ->
                val fullId = doc.getString("employeeId") ?: ""
                fullId.endsWith(employeeId)
            }

            if (matchingDoc == null) {
                return Result.failure(Exception("Employee ID not found"))
            }

            val doc = matchingDoc
            val email = doc.getString("email")
                ?: return Result.failure(Exception("No email on file"))

            val employee = Employee(
                employeeId = doc.getString("employeeId") ?: "",
                name = doc.getString("name") ?: "",
                position = doc.getString("position") ?: "Cashier",
                email = email,
                uid = doc.getString("uid") ?: ""
            )

            // Sign in with Firebase Auth
            auth.signInWithEmailAndPassword(email, password).await()

            Result.success(employee)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}
