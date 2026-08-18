package com.tanzir.diabo.ui.ide

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tanzir.diabo.data.local.entity.BuildRecord
import com.tanzir.diabo.data.local.entity.CloudBuildStatus
import java.io.File

/**
 * ▶ Real Build — shows live cloud-build progress (Queued → Building → Capturing → Done),
 * the emulator screenshot once ready, and Download/Install actions for the resulting APK.
 */
@Composable
fun RealBuildSheet(
    state: CodeIdeViewModel.RealBuildUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Real Build", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            when {
                state.triggerError != null -> ErrorRow(state.triggerError)
                state.build == null -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                else -> BuildStatusView(state.build, context)
            }
        }
    }
}

@Composable
private fun BuildStatusView(build: BuildRecord, context: android.content.Context) {
    when (build.status) {
        CloudBuildStatus.QUEUED, CloudBuildStatus.BUILDING, CloudBuildStatus.INSTALLING, CloudBuildStatus.CAPTURING -> {
            ProgressStages(build.status)
            Spacer(Modifier.height(8.dp))
            Text(
                "This can take a few minutes — you can close this sheet, the build keeps running in the background and you'll get a notification.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        CloudBuildStatus.SUCCESS -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Build succeeded", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))

            build.screenshotPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = "Build screenshot",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                build.apkPath?.let { apkPath ->
                    Button(onClick = { installApk(context, apkPath) }) { Text("Install Now") }
                    OutlinedButton(onClick = { shareApk(context, apkPath) }) { Text("Share APK") }
                }
            }
        }
        CloudBuildStatus.FAILED -> {
            ErrorRow(build.errorMessage ?: "Build failed")
            build.githubRunUrl?.let { url ->
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }) {
                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("View full log on GitHub")
                }
            }
        }
        CloudBuildStatus.CANCELLED -> ErrorRow("Build was cancelled")
    }
}

@Composable
private fun ProgressStages(status: CloudBuildStatus) {
    val stages = listOf(
        CloudBuildStatus.QUEUED to "Queued",
        CloudBuildStatus.BUILDING to "Building",
        CloudBuildStatus.INSTALLING to "Installing",
        CloudBuildStatus.CAPTURING to "Capturing"
    )
    val currentIndex = stages.indexOfFirst { it.first == status }.coerceAtLeast(0)

    Column {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stages.forEachIndexed { index, (_, label) ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (index <= currentIndex) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(message: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun installApk(context: android.content.Context, apkPath: String) {
    val apkFile = File(apkPath)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun shareApk(context: android.content.Context, apkPath: String) {
    val apkFile = File(apkPath)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.android.package-archive"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share APK"))
}
