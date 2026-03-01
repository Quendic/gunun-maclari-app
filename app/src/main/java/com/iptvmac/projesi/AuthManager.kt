package com.iptvmac.projesi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

/**
 * AuthManager — Firebase Authentication Singleton
 * 
 * Google Sign-In ile Firebase Auth entegrasyonu,
 * session yönetimi ve kullanıcı profili yönetimi.
 */
object AuthManager {
    private const val TAG = "AuthManager"
    
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private var googleSignInClient: GoogleSignInClient? = null

    /**
     * Google Sign-In client'ını başlat
     * Bu fonksiyon Activity veya Application context'i ile çağrılmalı
     */
    fun initialize(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Mevcut kullanıcıyı döndürür (giriş yapmamışsa null)
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Kullanıcının giriş yapıp yapmadığını kontrol eder
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Google Sign-In intent'ini döndürür
     * LoginActivity'den startActivityForResult ile çağrılacak
     */
    fun getSignInIntent(): Intent? = googleSignInClient?.signInIntent

    /**
     * Google Sign-In token'ını Firebase Auth ile doğrular
     */
    fun firebaseAuthWithGoogle(
        context: Context,
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Log.d(TAG, "Firebase Auth başarılı: ${user.email}")
                        // Profil kaydetme işlemini context ile yapıyoruz
                        saveUserProfile(context, user)
                        onSuccess(user)
                    } else {
                        onError("Firebase kullanıcısı boş döndü")
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Giriş başarısız"
                    Log.e(TAG, "Firebase Auth Hatası: $errorMsg")
                    onError(errorMsg)
                }
            }
    }

    /**
     * Kullanıcı profilini Firestore'a kaydet
     */
    private fun saveUserProfile(context: Context, user: FirebaseUser) {
        try {
            val userDoc = firestore.collection("users").document(user.uid)
            
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown"

            val profileData = hashMapOf(
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to com.google.firebase.Timestamp.now(),
                "deviceId" to deviceId
            )

            Log.d(TAG, "Firestore'a profil kaydediliyor: ${user.uid}")
            userDoc.set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "Profil başarıyla güncellendi") }
                .addOnFailureListener { e -> Log.e(TAG, "Firestore kayıt hatası (Firestore aktif mi?)", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Profil kaydetme sırasında hata oluştu", e)
        }
    }

    /**
     * Kullanıcı profilini Firestore'a kaydet (Context ile — deviceId desteği)
     */
    fun saveUserProfileWithContext(context: Context, user: FirebaseUser) {
        val userDoc = firestore.collection("users").document(user.uid)

        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        val profileData = hashMapOf(
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "lastLogin" to com.google.firebase.Timestamp.now(),
            "deviceId" to deviceId
        )

        userDoc.set(profileData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Profil kaydedildi: ${user.uid}") }
            .addOnFailureListener { Log.e(TAG, "Profil kaydetme hatası", it) }
    }

    /**
     * Çıkış yap
     */
    fun signOut(context: Context, onComplete: () -> Unit = {}) {
        auth.signOut()
        googleSignInClient?.signOut()?.addOnCompleteListener {
            onComplete()
        }
    }

    /**
     * Kullanıcının tüm profil verilerini Firestore'dan getirir
     */
    fun getUserFullProfile(onResult: (Map<String, Any>?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(null)
            return
        }

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.data)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Profil verisi alınamadı", e)
                onResult(null)
            }
    }

    /**
     * Kullanıcının abonelik durumunu Firestore'dan kontrol eder
     */
    fun checkSubscriptionStatus(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false)
            return
        }

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val isPremium = doc.getBoolean("isPremium") ?: false
                Log.d(TAG, "Abonelik durumu: $isPremium")
                onResult(isPremium)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Abonelik kontrolü başarısız", e)
                onResult(false)
            }
    }

    /**
     * Kullanıcının M3U URL'sini Firestore'dan al
     */
    fun getUserM3UUrl(onResult: (String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(null)
            return
        }

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("m3uUrl"))
            }
            .addOnFailureListener {
                Log.e(TAG, "M3U URL alınamadı", it)
                onResult(null)
            }
    }

    /**
     * Kullanıcının M3U URL'sini Firestore'a kaydet
     */
    fun setUserM3UUrl(url: String, onComplete: (Boolean) -> Unit = {}) {
        val user = auth.currentUser ?: run {
            onComplete(false)
            return
        }

        firestore.collection("users").document(user.uid)
            .set(hashMapOf("m3uUrl" to url), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                Log.e(TAG, "M3U URL kaydedilemedi", it)
                onComplete(false)
            }
    }

    /**
     * Ayıklanmış kanal listesini Firestore'a kaydet
     */
    fun saveChannels(channelsJson: String, onComplete: (Boolean) -> Unit = {}) {
        val user = auth.currentUser ?: return
        
        firestore.collection("users").document(user.uid)
            .update("channels", channelsJson)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener {
                // Eğer döküman yoksa veya field yoksa update hata verebilir, set merge kullanalım
                firestore.collection("users").document(user.uid)
                    .set(hashMapOf("channels" to channelsJson), com.google.firebase.firestore.SetOptions.merge())
                    .addOnCompleteListener { onComplete(it.isSuccessful) }
            }
    }

    /**
     * Kayıtlı kanalları Firestore'an getir
     */
    fun getChannels(onResult: (String?) -> Unit) {
        val user = auth.currentUser ?: return
        
        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("channels"))
            }
            .addOnFailureListener { onResult(null) }
    }
}
