package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.data.model.DEFAULT_TERMS_AND_CONDITIONS
import com.example.data.model.UserProfile
import com.example.ui.components.ImageCodecHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun attemptSilentGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit = {}
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun launchInteractiveGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = (context as? Activity) ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received from Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please check your network and try again.")
        }
    }
}

fun performGoogleSignOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun NoNetworkStatusBanner(isNetworkConnected: Boolean) {
    AnimatedVisibility(
        visible = !isNetworkConnected,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            color = Color(0xFF991B1B),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("no_network_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SignalWifiOff,
                    contentDescription = "No Network Connected",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No Network Connected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Turn on mobile data or Wi-Fi to receive instant Trading Zone updates.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFECACA)
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInWelcomeScreen(
    deviceId: String,
    ipAddress: String,
    isNetworkConnected: Boolean = true,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptSilentGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = onAuthSuccess
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                NoNetworkStatusBanner(isNetworkConnected = isNetworkConnected)

                com.example.ui.components.TradingZonePremiumLogo(
                    size = 88.dp,
                    animated = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Trading Zone",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Operated by Atharva Garg • Guide: Mr. Vikash Chandra Garg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Verified Trade Broadcasts, Marked Chart Setups & Moderated Commodity Feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "HARDWARE SECURITY TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Device ID: $deviceId • IP: $ipAddress",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        if (!isNetworkConnected) {
                            errorMessage = "No network connected. Please turn on mobile data or Wi-Fi first."
                            return@Button
                        }
                        errorMessage = null
                        isLoading = true
                        launchInteractiveGoogleSignIn(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockSetupScreen(
    userEmail: String,
    isAdmin: Boolean = false,
    onCompleteSetup: (
        pin: String,
        enableBiometric: Boolean,
        securityQuestion: String,
        securityAnswer: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var appPassword by remember { mutableStateOf("") }
    var confirmAppPassword by remember { mutableStateOf("") }
    var enableTwoStepVerification by remember { mutableStateOf(isAdmin) }
    var maxDevicesLimit by remember { mutableStateOf(if (isAdmin) 3 else 2) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What is my favorite commodity or stock?") }
    var securityAnswer by remember { mutableStateOf("") }
    var enableBiometric by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "🔒 ACCOUNT PASSWORD & OPTIONAL 2-STEP AUTH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Create Account Password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Account: $userEmail",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Like Instagram, an Account Password is required so nobody can log in with just your email. You can also optionally enable 2-Step Verification (Step 2 App PIN) and choose your Max Login Devices limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = appPassword,
                    onValueChange = { appPassword = it; errorText = null },
                    label = { Text("Account Password (Required • Min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_app_password_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmAppPassword,
                    onValueChange = { confirmAppPassword = it; errorText = null },
                    label = { Text("Confirm Account Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_confirm_app_password_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAdmin) "2-Step Verification (Mandatory for Admin)" else "Enable 2-Step Verification (Optional)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Step 1: Account Password • Step 2: 4–8 Digit App PIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableTwoStepVerification || isAdmin,
                                onCheckedChange = {
                                    if (!isAdmin) {
                                        enableTwoStepVerification = it
                                    }
                                }
                            )
                        }

                        if (enableTwoStepVerification || isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { if (it.length <= 8) pin = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Step 2: Create 4 to 8 Digit 2FA PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_pin_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = confirmPin,
                                onValueChange = { if (it.length <= 8) confirmPin = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Confirm 4 to 8 Digit 2FA PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_confirm_pin_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Max Login Devices Allowed on Your Account",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Limit how many mobile/web devices can log into your account simultaneously.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(1, 2, 3, 5).forEach { limitOpt ->
                                val selected = maxDevicesLimit == limitOpt
                                OutlinedButton(
                                    onClick = { maxDevicesLimit = limitOpt },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (selected) "✓ ${limitOpt}x" else "${limitOpt}x",
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = securityQuestion,
                    onValueChange = { securityQuestion = it },
                    label = { Text("Custom Security Question (For Password Recovery)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_security_question_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { securityAnswer = it },
                    label = { Text("Your Secret Recovery Answer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_security_answer_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Fingerprint / Face Biometric Unlock",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = enableBiometric,
                        onCheckedChange = { enableBiometric = it }
                    )
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val cleanPass = appPassword.trim()
                        val twoStepActive = enableTwoStepVerification || isAdmin
                        when {
                            cleanPass.length < 6 -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Account Password must be at least 6 characters."
                            }
                            cleanPass != confirmAppPassword.trim() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Account Password and Confirm Password do not match."
                            }
                            twoStepActive && pin.length < 4 -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "2-Step PIN must be at least 4 digits."
                            }
                            twoStepActive && pin != confirmPin -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "2-Step PIN and Confirm PIN do not match."
                            }
                            securityQuestion.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Please enter a custom security question."
                            }
                            securityAnswer.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Please enter an answer for your security question."
                            }
                            else -> {
                                com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                val effectivePin = if (twoStepActive) pin.trim() else "0000"
                                val encodedQuestionWithPass = UserProfile.encodeSecurityMetadata(
                                    password = cleanPass,
                                    twoStepEnabled = twoStepActive,
                                    maxDevicesLimit = maxDevicesLimit,
                                    plainQuestion = securityQuestion
                                )
                                onCompleteSetup(effectivePin, enableBiometric, encodedQuestionWithPass, securityAnswer.trim())
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("complete_pin_setup_button")
                ) {
                    Text(
                        if (enableTwoStepVerification || isAdmin) "Save Password + 2-Step Verification" else "Save Password & Continue",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun AppLockUnlockScreen(
    userEmail: String,
    isAdmin: Boolean = false,
    isTwoStepEnabled: Boolean = false,
    isBiometricEnabled: Boolean,
    securityQuestion: String,
    onVerifyAppPassword: ((String) -> Boolean)? = null,
    onVerifyPin: (String) -> Boolean,
    onRecoverPin: (answer: String, newPin: String) -> Boolean,
    onBiometricUnlockSuccess: () -> Unit,
    onSignOut: () -> Unit,
    myAccessRequests: List<com.example.data.model.AccessRequest> = emptyList(),
    currentDeviceId: String = "",
    onRequestCrossDeviceOtp: (code4Digit: String) -> Unit = {}
) {
    val context = LocalContext.current
    val generatedOtpCode = remember { (1000..9999).random().toString() }
    var hasSentOtpToPreviousDevice by remember { mutableStateOf(false) }
    var usePasswordFallbackMode by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryAnswer by remember { mutableStateOf("") }
    var recoveryNewPin by remember { mutableStateOf("") }

    val dummyProfile = remember(securityQuestion) {
        UserProfile(securityQuestion = securityQuestion)
    }
    val expectedAppPassword = remember(securityQuestion) {
        dummyProfile.extractAppPassword()
    }
    val displaySecurityQuestion = remember(securityQuestion) {
        dummyProfile.displaySecurityQuestion()
    }

    // Automatically send the 4-digit quick login code to any previously logged-in device on first display
    LaunchedEffect(generatedOtpCode) {
        if (!hasSentOtpToPreviousDevice) {
            hasSentOtpToPreviousDevice = true
            onRequestCrossDeviceOtp(generatedOtpCode)
        }
    }

    // Listen in real time if the user's previous logged-in device approved this 4-digit code
    val isApprovedByPreviousDevice = remember(myAccessRequests, generatedOtpCode) {
        myAccessRequests.any { req ->
            req.utrReference.startsWith("LOGIN_OTP_ACK|$generatedOtpCode") ||
                (req.isCrossDeviceLoginOtpRequest() &&
                    req.extractCrossDeviceLoginOtpCode() == generatedOtpCode &&
                    req.status == "APPROVED")
        }
    }

    LaunchedEffect(isApprovedByPreviousDevice) {
        if (isApprovedByPreviousDevice) {
            com.example.util.SoundAndHapticFeedback.playTradeSignal(context, "TARGET HIT")
            onBiometricUnlockSuccess()
        }
    }

    fun triggerHardwareBiometric() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    com.example.util.SoundAndHapticFeedback.playSuccess(context)
                    onBiometricUnlockSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Trading Zone")
            .setSubtitle(userEmail)
            .setNegativeButtonText("Use 4-Digit Code")
            .build()
        try {
            prompt.authenticate(info)
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled) {
            triggerHardwareBiometric()
        }
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text("Account Password / 2FA Recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Security Question: $displaySecurityQuestion",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = { recoveryAnswer = it },
                        label = { Text("Your Secret Recovery Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recoveryNewPin,
                        onValueChange = { if (it.length <= 8) recoveryNewPin = it.filter { ch -> ch.isDigit() } },
                        label = { Text("New 4–8 Digit Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onRecoverPin(recoveryAnswer, recoveryNewPin)) {
                            com.example.util.SoundAndHapticFeedback.playSuccess(context)
                            showRecoveryDialog = false
                        } else {
                            com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                            errorText = "Incorrect security answer or invalid new PIN."
                        }
                    }
                ) {
                    Text("Verify & Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (usePasswordFallbackMode) {
                            "🔑 QUICK PASSWORD LOGIN"
                        } else {
                            "📲 2-STEP 4-DIGIT DEVICE VERIFICATION"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (usePasswordFallbackMode) "Security Password Login" else "Quick Device Verification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Switcher Pills: 1) 4-Digit Code on Previous Device  2) Security Password Fallback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            usePasswordFallbackMode = false
                            errorText = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "📲 4-Digit Code",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            com.example.util.SoundAndHapticFeedback.playTap(context)
                            usePasswordFallbackMode = true
                            errorText = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "🔑 Password Fallback",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AnimatedVisibility(visible = !usePasswordFallbackMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📲 4-Digit Code Sent to Logged-In Device",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter the 4-digit code shown on your previously logged-in mobile/web session (or your 4-digit PIN), or tap 'Approve Login' on your previous device.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = {
                                        com.example.util.SoundAndHapticFeedback.playClick(context)
                                        onRequestCrossDeviceOtp(generatedOtpCode)
                                    }
                                ) {
                                    Text("🔄 Resend 4-Digit Code to Logged-In Device")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 8) {
                                    pinInput = it.filter { ch -> ch.isDigit() }
                                    errorText = null
                                }
                            },
                            label = { Text("Enter 4-Digit Code or App PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_pin_input")
                        )
                    }
                }

                AnimatedVisibility(visible = usePasswordFallbackMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔑 Previous device not nearby? Enter your Security Password for instant quick login.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorText = null
                            },
                            label = { Text("Enter Security Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_app_password_input")
                        )
                    }
                }

                if (!errorText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (usePasswordFallbackMode) {
                            val cleanPass = passwordInput.trim()
                            if (cleanPass.length < 4) {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Please enter your Security Password."
                                return@Button
                            }
                            val passOk = when {
                                onVerifyAppPassword != null -> onVerifyAppPassword(cleanPass)
                                expectedAppPassword.isNotBlank() -> cleanPass == expectedAppPassword
                                else -> false
                            } || onVerifyPin(cleanPass)
                            if (passOk) {
                                com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                onBiometricUnlockSuccess()
                            } else {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Incorrect Security Password."
                            }
                        } else {
                            val cleanCode = pinInput.trim()
                            if (cleanCode.length < 4) {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Enter the 4-digit code shown on your logged-in device (or your 4-digit PIN)."
                                return@Button
                            }
                            if (cleanCode == generatedOtpCode || onVerifyPin(cleanCode)) {
                                com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                onBiometricUnlockSuccess()
                            } else {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                errorText = "Invalid 4-digit code or PIN. Check your logged-in device or use Security Password fallback."
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("unlock_pin_button")
                ) {
                    Text(
                        if (usePasswordFallbackMode) "Unlock with Security Password" else "Verify 4-Digit Code & Login",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = {
                        com.example.util.SoundAndHapticFeedback.playTap(context)
                        usePasswordFallbackMode = !usePasswordFallbackMode
                        errorText = null
                    }
                ) {
                    Text(
                        if (usePasswordFallbackMode) {
                            "📲 Use 4-Digit Code from Logged-In Device Instead"
                        } else {
                            "🔑 Previous device unavailable? Use Security Password"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isBiometricEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { triggerHardwareBiometric() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock with Fingerprint / Biometrics")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { showRecoveryDialog = true }) {
                        Text("Forgot Password / PIN?")
                    }
                    TextButton(onClick = onSignOut) {
                        Text("Switch Account")
                    }
                }
            }
        }
    }
}

@Composable
fun MandatoryPhotoRegistrationScreen(
    userEmail: String,
    initialDisplayName: String,
    initialSecurityQuestion: String,
    deviceId: String,
    deviceModel: String,
    ipAddress: String,
    isBusy: Boolean,
    statusMessage: String?,
    onSubmitRegistration: (
        fullName: String,
        phoneNumber: String,
        city: String,
        photoBase64: String,
        securityQuestion: String,
        securityAnswer: String,
        termsAccepted: Boolean
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(initialDisplayName) }
    var phoneNumber by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    val existingMetaProfile = remember(initialSecurityQuestion) {
        UserProfile(securityQuestion = initialSecurityQuestion)
    }
    var securityQuestion by remember(initialSecurityQuestion) {
        mutableStateOf(
            existingMetaProfile.displaySecurityQuestion()
        )
    }
    var securityAnswer by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCodecHelper.encodeUriToBase64Jpeg(
                context = context,
                uri = uri,
                maxDimension = 480,
                quality = 72
            )
            if (encoded != null) {
                photoBase64 = encoded
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            photoBase64 = ImageCodecHelper.encodeBitmapToBase64Jpeg(
                bitmap = bitmap,
                maxDimension = 480,
                quality = 72
            )
        }
    }

    val previewBitmap = remember(photoBase64) {
        ImageCodecHelper.decodeBase64ToBitmap(photoBase64)
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms, Non-Liability & No-Refund Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = DEFAULT_TERMS_AND_CONDITIONS.trim(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    termsAccepted = true
                    showTermsDialog = false
                }) {
                    Text("I Accept")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Mandatory Member Verification",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upload your clear profile photo and complete registration for Admin approval.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Selected profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Pick Photo")
                    }
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) }
                    ) {
                        Text("Take Selfie")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Mobile / WhatsApp Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City / State *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = securityQuestion,
                    onValueChange = { securityQuestion = it },
                    label = { Text("Custom Security Question *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = securityAnswer,
                    onValueChange = { securityAnswer = it },
                    label = { Text("Security Answer *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I accept the Non-Liability Disclaimer & Strict No-Identity-Disclosure Policy (No Refund on suspension).",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(onClick = { showTermsDialog = true }) {
                            Text("Read Full Terms & Conditions")
                        }
                    }
                }

                val displayError = localError ?: statusMessage
                if (!displayError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displayError,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        when {
                            photoBase64.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please upload or capture your profile photo."
                            }
                            fullName.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please enter your full name."
                            }
                            phoneNumber.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please enter your phone number."
                            }
                            city.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please enter your city."
                            }
                            securityQuestion.isBlank() || securityAnswer.isBlank() -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please complete your custom security question and answer."
                            }
                            !termsAccepted -> {
                                com.example.util.SoundAndHapticFeedback.playAlertOrError(context)
                                localError = "Please accept the Terms & Conditions."
                            }
                            else -> {
                                com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                localError = null
                                val encodedSecurityQuestion = UserProfile.encodeSecurityMetadata(
                                    password = existingMetaProfile.extractAppPassword(),
                                    twoStepEnabled = existingMetaProfile.isTwoStepEnabled(false),
                                    maxDevicesLimit = existingMetaProfile.maxDevicesLimit(2),
                                    plainQuestion = securityQuestion.trim()
                                )
                                onSubmitRegistration(
                                    fullName.trim(),
                                    phoneNumber.trim(),
                                    city.trim(),
                                    photoBase64,
                                    encodedSecurityQuestion,
                                    securityAnswer.trim(),
                                    true
                                )
                            }
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_registration_button")
                ) {
                    Text(if (isBusy) "Submitting..." else "Submit for Admin Approval")
                }

                Spacer(modifier = Modifier.height(6.dp))
                TextButton(onClick = onSignOut) {
                    Text("Sign Out ($userEmail • $deviceId • $deviceModel • $ipAddress)")
                }
            }
        }
    }
}

@Composable
fun AccountStatusGateScreen(
    profile: UserProfile,
    isDeviceBanned: Boolean,
    onSignOut: () -> Unit,
    myAccessRequests: List<com.example.data.model.AccessRequest> = emptyList(),
    onSubmitUnbanAppeal: (appealReason: String) -> Unit = {}
) {
    val context = LocalContext.current
    var appealReason by remember {
        mutableStateOf("Banned by mistake — requesting a second chance to access Trading Zone.")
    }
    var localAppealSubmitted by remember { mutableStateOf(false) }
    val hasPendingUnbanAppeal = remember(myAccessRequests, localAppealSubmitted) {
        localAppealSubmitted || myAccessRequests.any { it.isUnbanAppealRequest() && it.status == "PENDING" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isDeviceBanned) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDeviceBanned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isDeviceBanned) {
                        "Hardware Device & Account Suspended"
                    } else {
                        "Pending Admin Verification"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isDeviceBanned) {
                        "Your account (${profile.email}) and hardware Device ID (${profile.deviceId}) were suspended. If this happened by mistake, you can request a Second Chance Unban from Admin below."
                    } else {
                        "Thank you, ${profile.fullName}. Your photo and profile details have been submitted. Your workspace will unlock automatically once Admin approves your account."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (isDeviceBanned) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🙏 Banned by Mistake? Request a Second Chance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hasPendingUnbanAppeal) {
                                Text(
                                    text = "✅ Your Second-Chance Unban Request has been sent to Admin! Once Admin clicks 'Unban & Give 2nd Chance', your screen will unlock automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                OutlinedTextField(
                                    value = appealReason,
                                    onValueChange = { appealReason = it },
                                    label = { Text("Note to Admin for Second Chance") },
                                    minLines = 2,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("unban_appeal_reason_input")
                                )
                                Button(
                                    onClick = {
                                        com.example.util.SoundAndHapticFeedback.playSuccess(context)
                                        localAppealSubmitted = true
                                        onSubmitUnbanAppeal(appealReason.trim())
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("request_unban_second_chance_button")
                                ) {
                                    Text("🙏 Request Unban / Second Chance", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Device ID: ${profile.deviceId} • IP: ${profile.ipAddress}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

@Composable
fun CrossDeviceLoginCodeBanner(
    pendingOtpRequests: List<com.example.data.model.AccessRequest>,
    currentDeviceId: String,
    onApproveOtpRequest: (com.example.data.model.AccessRequest) -> Unit
) {
    val context = LocalContext.current
    var dismissedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val activeOtpRequest = remember(pendingOtpRequests, currentDeviceId, dismissedIds) {
        pendingOtpRequests.firstOrNull { req ->
            req.status == "PENDING" &&
                req.isCrossDeviceLoginOtpRequest() &&
                req.deviceId != currentDeviceId &&
                !dismissedIds.contains(req.id) &&
                !pendingOtpRequests.any { ack ->
                    ack.utrReference.startsWith("LOGIN_OTP_ACK|${req.extractCrossDeviceLoginOtpCode()}")
                }
        }
    }

    AnimatedVisibility(
        visible = activeOtpRequest != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        activeOtpRequest?.let { otpReq ->
            val code = otpReq.extractCrossDeviceLoginOtpCode()
            val model = otpReq.extractCrossDeviceLoginModel()
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .testTag("cross_device_login_otp_banner")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📲 New Device Login • 4-Digit Code: $code",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Device '$model' is trying to sign in. Enter code $code on that device or tap Approve below for instant login:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                com.example.util.SoundAndHapticFeedback.playTradeSignal(context, "BUY")
                                dismissedIds = dismissedIds + otpReq.id
                                onApproveOtpRequest(otpReq)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("approve_cross_device_otp_button")
                        ) {
                            Text("✅ Approve Login ($code)", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                dismissedIds = dismissedIds + otpReq.id
                            }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

