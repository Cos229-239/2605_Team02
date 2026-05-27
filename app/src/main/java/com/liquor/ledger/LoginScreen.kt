package com.liquor.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquor.ledger.firebase.AuthRepository
import kotlinx.coroutines.launch

// LoginScreen is the first screen employees see when opening the app
// It takes an Employee ID and password instead of email and password
// On successful login it stores the employee in SessionManager

@Composable
fun LoginScreen(onLoginSuccess: (Employee) -> Unit) {

    // Variables that hold the current values typed into the input fields
    var employeeId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Used to launch the login coroutine when the button is clicked
    val scope = rememberCoroutineScope()

    // Create one instance of AuthRepository for this screen
    val authRepository = remember { AuthRepository() }

    // Full screen horizontal layout
    Row(modifier = Modifier.fillMaxSize())
    {

        Box(
            modifier = Modifier
                .width(350.dp)
                .fillMaxHeight()
                .background(Color(0xFF101E37)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally)
            {

                Text(
                    text = "Liquor Ledger",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Business Tracker",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
            }
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        )
        {
            Card(
                modifier = Modifier.width(400.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Login form title
                    Text(
                        text = "Employee Login",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Sign in with your Employee ID",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Employee ID input field
                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = { employeeId = it },
                        label = { Text("Employee ID") },
                        placeholder = { Text("e.g. EMP-2024-0001") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Password input field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        )
                    )

                    // Error message — only shows when there is an error
                    if (errorMessage.isNotEmpty())
                    {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sign In button
                    // Shows a loading spinner while login is in progress
                    // Disabled while loading to prevent double taps
                    Button(
                        onClick =
                            {
                            // Validate that fields are not empty
                            if (employeeId.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter your Employee ID and password"
                                return@Button
                            }

                            // Start the login process
                            isLoading = true
                            errorMessage = ""

                            // Launch login in a coroutine
                            scope.launch {
                                val result = authRepository.loginWithEmployeeId(
                                    employeeId.trim(),
                                    password
                                )
                                isLoading = false

                                // Handle success
                                result.onSuccess { employee ->
                                    // Save the logged in employee to SessionManager
                                    SessionManager.currentEmployee = employee
                                    onLoginSuccess(employee)
                                }

                                // Handle failure
                                result.onFailure { error ->
                                    errorMessage = when {
                                        error.message?.contains("Employee ID not found") == true ->
                                            "Employee ID not found"
                                        error.message?.contains("password") == true ->
                                            "Incorrect password"
                                        else ->
                                            "Login failed. Please try again."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D5FFF)
                        ),
                        enabled = !isLoading
                    ) {
                        // Show spinner while loading
                        if (isLoading)
                        {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        else
                        {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
