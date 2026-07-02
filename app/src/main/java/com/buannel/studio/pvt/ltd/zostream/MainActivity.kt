package com.buannel.studio.pvt.ltd.zostream

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import com.buannel.studio.pvt.ltd.zostream.ui.App
import com.buannel.studio.pvt.ltd.zostream.ui.screens.ParentalControlScreen
import com.buannel.studio.pvt.ltd.zostream.ui.theme.TvComposeIntroductionTheme
import com.buannel.studio.pvt.ltd.zostream.utils.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint

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

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Check login session
        if (!SessionManager.checkUserSession(this)) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

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
    var update by remember { mutableStateOf<TvUpdate?>(null) }

    DisposableEffect(Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("tv_update")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteVersion = snapshot.child("v").getValue(Long::class.java) ?: return
                val updateUrl = snapshot.child("url").getValue(String::class.java).orEmpty()
                val forceUpdate = snapshot.child("force").getValue(Boolean::class.java) ?: false

                update = if (remoteVersion > installedVersionCode) {
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

private fun getInstalledVersionCode(context: Context): Long {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return PackageInfoCompat.getLongVersionCode(packageInfo)
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
