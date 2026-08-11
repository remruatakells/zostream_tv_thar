package com.buannel.studio.pvt.ltd.zostream

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.view.WindowCompat
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.buannel.studio.pvt.ltd.zostream.api.Api
import com.buannel.studio.pvt.ltd.zostream.ui.App
import com.buannel.studio.pvt.ltd.zostream.ui.screens.ParentalControlScreen
import com.buannel.studio.pvt.ltd.zostream.ui.theme.TvComposeIntroductionTheme
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import java.security.MessageDigest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var updateDownloadId: Long = -1L
    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId == updateDownloadId) {
                installDownloadedUpdate(downloadId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        applyCachedOfficialConfig()
        setContent {
            TvComposeIntroductionTheme {
                SilentSplashScreen()
            }
        }

        verifyOfficialClientFromFirebase(silent = false) { result ->
            if (!result.accepted) {
                if (!result.networkIssue) {
                    clearOfficialConfigCache()
                }
                setContent {
                    TvComposeIntroductionTheme {
                        if (result.networkIssue) {
                            VerificationNetworkIssueScreen(result.message) {
                                setContent {
                                    TvComposeIntroductionTheme {
                                        SilentSplashScreen()
                                    }
                                }
                                verifyOfficialClientFromFirebase(silent = false) { retryResult ->
                                    handleOfficialVerificationResult(retryResult)
                                }
                            }
                        } else {
                            VerificationBlockedScreen(result.message)
                        }
                    }
                }
                return@verifyOfficialClientFromFirebase
            }

            handleOfficialVerificationResult(result)
        }
    }

    private fun handleOfficialVerificationResult(result: OfficialClientVerificationResult) {
        if (!result.accepted) {
            if (!result.networkIssue) {
                clearOfficialConfigCache()
            }
            setContent {
                TvComposeIntroductionTheme {
                    if (result.networkIssue) {
                        VerificationNetworkIssueScreen(result.message) {
                            setContent {
                                TvComposeIntroductionTheme {
                                    SilentSplashScreen()
                                }
                            }
                            verifyOfficialClientFromFirebase(silent = false) { retryResult ->
                                handleOfficialVerificationResult(retryResult)
                            }
                        }
                    } else {
                        VerificationBlockedScreen(result.message)
                    }
                }
            }
            return
        }

        val resolvedApiBaseUrl = resolveApiBaseUrl(result.apiBaseUrl)
        if (resolvedApiBaseUrl.isNullOrBlank()) {
            clearOfficialConfigCache()
            setContent {
                TvComposeIntroductionTheme {
                    VerificationBlockedScreen("OFFICIAL_CONFIG_MISSING_API_BASE_URL")
                }
            }
            return
        }

        storeOfficialConfigCache(resolvedApiBaseUrl, result.apiVersion)
        Api.setBaseUrl(this, resolvedApiBaseUrl)

        if (!SessionManager.checkUserSession(this)) {
            return
        }

        renderMainContent()
    }

    private fun applyCachedOfficialConfig(): Boolean {
        val prefs = getSharedPreferences(OFFICIAL_PREFS, MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACCEPTED, false)) return false

        val cachedApiBaseUrl = resolveApiBaseUrl(
            prefs.getString(KEY_API_BASE_URL, null)
        )
        cachedApiBaseUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { Api.setBaseUrl(this, it) }

        return true
    }

    private fun storeOfficialConfigCache(apiBaseUrl: String?, apiVersion: String?) {
        getSharedPreferences(OFFICIAL_PREFS, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putString(KEY_API_BASE_URL, apiBaseUrl.orEmpty())
            .putString(KEY_API_VERSION, apiVersion ?: "4")
            .apply()
    }

    private fun clearOfficialConfigCache() {
        getSharedPreferences(OFFICIAL_PREFS, MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun resolveApiBaseUrl(apiBaseUrl: String?): String? {
        val configured = apiBaseUrl?.takeIf { it.isNotBlank() }
        if (configured != null) return configured

        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable && isProbablyEmulator()) DEBUG_EMULATOR_API_BASE_URL else null
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.BRAND.startsWith("generic", ignoreCase = true) ||
            Build.DEVICE.startsWith("generic", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    private fun renderMainContent() {
        setContent {
            TvComposeIntroductionTheme {
                var showParentalControl by remember { mutableStateOf(true) }

                Surface(
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (showParentalControl) {
                        ParentalControlScreen(
                            onAdultSelected = {
                                SessionManager.setParentalMode(
                                    this@MainActivity,
                                    SessionManager.MODE_ADULT
                                )
                                showParentalControl = false
                            },
                            onKidsSelected = {
                                SessionManager.setParentalMode(
                                    this@MainActivity,
                                    SessionManager.MODE_KIDS
                                )
                                showParentalControl = false
                            }
                        )
                    } else {
                        App()
                        AppUpdateWatcher(
                            onUpdateNow = { startUpdateDownload(it) }
                        )
                    }
                }
            }
        }
    }

    private fun verifyOfficialClientFromFirebase(
        silent: Boolean,
        onResult: (OfficialClientVerificationResult) -> Unit
    ) {
        FirebaseDatabase.getInstance()
            .getReference("official_client_configs/android-tv")
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(evaluateOfficialConfigs(snapshot))
            }
            .addOnFailureListener { error ->
                onResult(
                    OfficialClientVerificationResult(
                        accepted = false,
                        message = networkIssueMessage(error),
                        networkIssue = true
                    )
                )
            }
    }

    private fun hasUsableNetwork(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun networkIssueMessage(error: Exception): String {
        val message = error.localizedMessage.orEmpty()
        return if (
            message.contains("network", ignoreCase = true) ||
            message.contains("internet", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true) ||
            message.contains("host", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true)
        ) {
            "No internet connection. Please check your network and try again."
        } else {
            "Unable to connect to Zo Stream verification service. Please check your network and try again."
        }
    }

    private fun evaluateOfficialConfigs(snapshot: DataSnapshot): OfficialClientVerificationResult {
        val versionName = getInstalledVersionName(this)
        val certificateSha256 = getSigningCertificateSha256()
        var failureReason = "NO_ENABLED_CONFIG_MATCHED"

        for (child in snapshot.children) {
            val enabled = child.child("enabled").getValue(Boolean::class.java) ?: true
            if (!enabled) continue

            val verificationEnabled =
                child.child("verification_enabled").getValue(Boolean::class.java) ?: true
            val appIdentifier = child.child("app_identifier").safeString()
            val minVersion = child.child("min_version").safeString()
            val latestVersion = child.child("latest_version").safeString()
            val apiBaseUrl = child.child("api_base_url").safeString()
            val apiVersion = child.child("api_version").safeString()

            if (!verificationEnabled) {
                return OfficialClientVerificationResult(
                    accepted = true,
                    apiBaseUrl = apiBaseUrl,
                    apiVersion = apiVersion,
                    message = "VERIFICATION_DISABLED",
                    latestVersion = latestVersion
                )
            }

            if (!appIdentifier.isNullOrBlank() && !appIdentifier.equals(packageName, ignoreCase = true)) {
                failureReason = "APP_IDENTIFIER_MISMATCH"
                continue
            }

            val certificateSnapshot = child.child("certificate_sha256")
            if (!certificateSnapshot.exists()) {
                failureReason = "CONFIG_HAS_NO_CERTIFICATE_SHA256"
                continue
            }

            if (!certificateMatches(certificateSnapshot, certificateSha256)) {
                failureReason = "CERTIFICATE_SHA256_MISMATCH"
                continue
            }

            if (appIdentifier.isNullOrBlank()) {
                failureReason = "CONFIG_HAS_NO_VERIFICATION_REQUIREMENTS"
                continue
            }

            val outdated = !minVersion.isNullOrBlank() &&
                compareVersion(versionName, minVersion) < 0

            return OfficialClientVerificationResult(
                accepted = !outdated,
                apiBaseUrl = apiBaseUrl,
                apiVersion = apiVersion,
                message = if (outdated) {
                    "Minimum required version is $minVersion."
                } else {
                    "OFFICIAL_CLIENT"
                },
                latestVersion = latestVersion
            )
        }

        return OfficialClientVerificationResult(
            accepted = false,
            message = failureReason
        )
    }

    override fun onStart() {
        super.onStart()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                updateDownloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                updateDownloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(updateDownloadReceiver) }
    }

    private fun startUpdateDownload(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "Update link is missing", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = url.substringAfterLast('/').substringBefore('?').ifBlank {
            "zostream-update.apk"
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Zo Stream TV update")
            .setDescription("Downloading the latest version")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        updateDownloadId = downloadManager.enqueue(request)
        Toast.makeText(this, "Update download started", Toast.LENGTH_SHORT).show()
    }

    private fun installDownloadedUpdate(downloadId: Long) {
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
        if (apkUri == null) {
            Toast.makeText(this, "Update download failed", Toast.LENGTH_SHORT).show()
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(installIntent)
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val OFFICIAL_PREFS = "official_client_cache"
        const val KEY_ACCEPTED = "accepted"
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_API_VERSION = "api_version"
        const val DEBUG_EMULATOR_API_BASE_URL = "http://10.0.2.2:8000/"
    }
}

private data class OfficialClientVerificationResult(
    val accepted: Boolean,
    val apiBaseUrl: String? = null,
    val apiVersion: String? = null,
    val message: String,
    val latestVersion: String? = null,
    val networkIssue: Boolean = false
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SilentSplashScreen() {
    Surface(
        shape = RectangleShape,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05070D)),
            contentAlignment = Alignment.Center
        ) {
            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerificationBlockedScreen(message: String) {
    val userMessage = if (message.startsWith("Minimum required version")) {
        message
    } else {
        "This app cannot be opened right now. Please install the latest official Zo Stream TV app."
    }

    Surface(
        shape = RectangleShape,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05070D))
                .padding(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .width(560.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .padding(42.dp)
            ) {
                Text(
                    text = "App unavailable",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userMessage,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerificationNetworkIssueScreen(message: String, onRetry: () -> Unit) {
    Surface(
        shape = RectangleShape,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF05070D))
                .padding(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .width(560.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .padding(42.dp)
            ) {
                Text(
                    text = "Network required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}

private data class TvUpdate(
    val force: Boolean,
    val url: String,
    val versionCode: Long
)

@Composable
private fun AppUpdateWatcher(
    onUpdateNow: (String) -> Unit
) {
    val context = LocalContext.current
    val installedVersionCode = remember { getInstalledVersionCode(context) }
    val allowInAppUpdatePrompt = remember {
        val debuggable =
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        debuggable && !context.isPlayStoreInstall()
    }
    var update by remember { mutableStateOf<TvUpdate?>(null) }

    DisposableEffect(allowInAppUpdatePrompt) {
        if (!allowInAppUpdatePrompt) {
            update = null
            onDispose { }
        } else {
            val reference = FirebaseDatabase.getInstance().getReference("tv_update")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child("enabled").getValue(Boolean::class.java) == false) {
                        update = null
                        return
                    }

                    val remoteVersion = snapshot.child("v").getValue(Long::class.java) ?: return
                    val updateUrl = snapshot.child("url").safeString().orEmpty().trim()
                    val forceUpdate = snapshot.child("force").getValue(Boolean::class.java) ?: false

                    update = if (updateUrl.isNotBlank() && remoteVersion > installedVersionCode) {
                        TvUpdate(
                            force = forceUpdate,
                            url = updateUrl,
                            versionCode = remoteVersion
                        )
                    } else {
                        null
                    }
                }

                override fun onCancelled(error: DatabaseError) = Unit
            }

            reference.addValueEventListener(listener)
            onDispose { reference.removeEventListener(listener) }
        }
    }

    update?.let {
        UpdateDialog(
            update = it,
            onUpdateNow = { onUpdateNow(it.url) },
            onDismiss = {
                if (!it.force) {
                    update = null
                }
            }
        )
    }
}

private fun Context.isPlayStoreInstall(): Boolean {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    }.getOrNull() == "com.android.vending"
}

private fun getInstalledVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(packageInfo)
}

private fun getInstalledVersionName(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName ?: "1.0"
}

private fun compareVersion(left: String, right: String): Int {
    val leftParts = left.split(".", "-").map { it.toIntOrNull() ?: 0 }
    val rightParts = right.split(".", "-").map { it.toIntOrNull() ?: 0 }
    val count = maxOf(leftParts.size, rightParts.size)

    repeat(count) { index ->
        val delta = (leftParts.getOrNull(index) ?: 0) - (rightParts.getOrNull(index) ?: 0)
        if (delta != 0) return delta
    }

    return 0
}

private fun Context.getSigningCertificateSha256(): Set<String> {
    return runCatching {
        val signatures: Array<out Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val signingInfo = packageInfo.signingInfo ?: return@runCatching emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners ?: emptyArray()
            } else {
                signingInfo.signingCertificateHistory ?: emptyArray()
            }
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            packageInfo.signatures ?: emptyArray()
        }

        if (signatures.isEmpty()) return@runCatching emptySet()

        val digest = MessageDigest.getInstance("SHA-256")
        signatures.map { signature ->
            digest.digest(signature.toByteArray())
                .joinToString(separator = "") { "%02X".format(it) }
        }.toSet()
    }.getOrDefault(emptySet())
}

private fun normalizeFingerprint(value: String?): String {
    return value.orEmpty().replace(Regex("[^a-fA-F0-9]"), "").uppercase()
}

private fun DataSnapshot.safeString(): String? {
    return when (val rawValue = value) {
        is String -> rawValue
        is Number -> rawValue.toString()
        is Boolean -> rawValue.toString()
        else -> null
    }
}

private fun certificateMatches(snapshot: DataSnapshot, installedSha256: Set<String>): Boolean {
    val normalizedInstalled = installedSha256.map(::normalizeFingerprint).filter { it.isNotBlank() }.toSet()
    if (normalizedInstalled.isEmpty()) return false

    snapshot.safeString()?.let {
        if (normalizeFingerprint(it) in normalizedInstalled) return true
    }

    return snapshot.children.any { child ->
        normalizeFingerprint(child.safeString()) in normalizedInstalled
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpdateDialog(
    update: TvUpdate,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(enabled = update.force) {}

    Dialog(onDismissRequest = {
        if (!update.force) {
            onDismiss()
        }
    }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .background(Color(0xFF101827), RoundedCornerShape(8.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Update Available",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A newer Zo Stream TV version is ready. Install version ${update.versionCode} to continue with the latest app.",
                    color = Color(0xFFD1D5DB)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!update.force) {
                        Button(onClick = onDismiss) {
                            Text("Later")
                        }
                    }
                    Button(onClick = onUpdateNow) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
