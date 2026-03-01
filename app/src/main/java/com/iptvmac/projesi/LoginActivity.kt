package com.iptvmac.projesi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalTvMaterial3Api::class)
class LoginActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zaten giriş yapmışsa direkt MainActivity'ye git
        AuthManager.initialize(this)
        if (AuthManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF020617),
                    surface = Color(0xFF1E293B),
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color.White
                )
            ) {
                LoginScreen(
                    onGoogleSignIn = { launchGoogleSignIn() }
                )
            }
        }
    }

    // ── Google Sign-In Launcher ──
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                AuthManager.firebaseAuthWithGoogle(
                    context = this,
                    idToken = token,
                    onSuccess = { user ->
                        navigateToMain()
                    },
                    onError = { error ->
                        Toast.makeText(this, "Giriş hatası: $error", Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Auth error: $error")
                        // Hata durumunda loading'i kapat
                    }
                )
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed: ${e.statusCode}", e)
            Toast.makeText(this, "Google giriş başarısız: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchGoogleSignIn() {
        val signInIntent = AuthManager.getSignInIntent()
        if (signInIntent != null) {
            signInLauncher.launch(signInIntent)
        } else {
            Toast.makeText(this, "Google Sign-In başlatılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// ═══════════════════════════════════════════════
//  LOGIN UI — Glassmorphism Premium Design
// ═══════════════════════════════════════════════

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LoginScreen(onGoogleSignIn: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }

    // Arka plan animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF020617),
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B)
                    )
                )
            )
    ) {
        // ── Dekoratif Parlak Toplar (Glassmorphism) ──
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-50).dp + (animatedOffset * 100).dp, y = (-80).dp)
                .blur(100.dp)
                .background(
                    Color(0xFF38BDF8).copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 50.dp + (animatedOffset * -60).dp)
                .blur(80.dp)
                .background(
                    Color(0xFF818CF8).copy(alpha = 0.12f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-120).dp + (animatedOffset * 40).dp, y = 100.dp)
                .blur(60.dp)
                .background(
                    Color(0xFF22D3EE).copy(alpha = 0.1f),
                    CircleShape
                )
        )

        // ── Ana İçerik ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Uygulama Başlığı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                // Süsleyici çubuk
                Box(
                    modifier = Modifier
                        .size(6.dp, 48.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
                            ),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "GÜNÜN MAÇLARI",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Premium Spor Yayınları",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detay text
            Text(
                text = "Tüm maçları tek ekranda izleyin.\nbeIN Sports, S Sport, Exxen ve daha fazlası.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // ── Glassmorphism Giriş Kartı ──
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Hoş Geldiniz",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Devam etmek için giriş yapın",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Google Sign-In Button
                    Surface(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                onGoogleSignIn()
                            }
                        },
                        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White,
                            focusedContainerColor = Color(0xFFF1F5F9)
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF4285F4),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            // Google "G" logosu (text olarak)
                            Text(
                                text = "G",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isLoading) "Giriş yapılıyor..." else "Google ile Devam Et",
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Alt bilgi
                    Text(
                        text = "Giriş yaparak kullanım koşullarını kabul edersiniz.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Versiyon bilgisi
            Text(
                text = "v1.9 • IPTV Mac Projesi",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF334155)
            )
        }
    }
}
