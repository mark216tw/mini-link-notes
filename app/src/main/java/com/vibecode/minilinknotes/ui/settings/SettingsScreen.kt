package com.vibecode.minilinknotes.ui.settings

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vibecode.minilinknotes.AppViewModel
import com.vibecode.minilinknotes.R
import com.vibecode.minilinknotes.data.AppSettings
import com.vibecode.minilinknotes.data.DisplayMode
import com.vibecode.minilinknotes.data.backup.ImportMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showReplaceConfirm by remember { mutableStateOf(false) }
    var replaceUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri?.let(viewModel::export)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showModeDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_appearance))

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_display_mode)) },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DisplayMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.displayMode == mode,
                        onClick = { viewModel.setDisplayMode(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        DisplayMode.SYSTEM -> R.string.settings_system
                                        DisplayMode.LIGHT -> R.string.settings_light
                                        DisplayMode.DARK -> R.string.settings_dark
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.settings_dynamic_color_desc)) },
                trailingContent = {
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    )
                },
            )

            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_behavior))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_auto_title)) },
                supportingContent = { Text(stringResource(R.string.settings_auto_title_desc)) },
                trailingContent = {
                    Switch(
                        checked = settings.autoFetchTitle,
                        onCheckedChange = viewModel::setAutoFetchTitle,
                    )
                },
            )

            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_data))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = { Text(stringResource(R.string.settings_export_desc)) },
                leadingContent = { Icon(Icons.Outlined.FileUpload, contentDescription = null) },
                modifier = Modifier.clickable {
                    val name = "mini-link-notes-" +
                        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
                    exportLauncher.launch(name)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                supportingContent = { Text(stringResource(R.string.settings_import_desc)) },
                leadingContent = { Icon(Icons.Outlined.FileDownload, contentDescription = null) },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("*/*"))
                },
            )

            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenAbout),
            )

            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (showModeDialog && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showModeDialog = false
                pendingImportUri = null
            },
            title = { Text(stringResource(R.string.settings_import_mode_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_import_merge_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            pendingImportUri?.let { viewModel.import(it, ImportMode.MERGE) }
                            showModeDialog = false
                            pendingImportUri = null
                        },
                    ) {
                        Text(stringResource(R.string.action_merge))
                    }
                    Text(
                        text = stringResource(R.string.settings_import_replace_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                        onClick = {
                            replaceUri = pendingImportUri
                            showModeDialog = false
                            pendingImportUri = null
                            showReplaceConfirm = true
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.action_overwrite),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showModeDialog = false
                        pendingImportUri = null
                    },
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showReplaceConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm = false },
            title = { Text(stringResource(R.string.settings_import_mode_title)) },
            text = { Text(stringResource(R.string.settings_import_replace_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        replaceUri?.let { viewModel.import(it, ImportMode.REPLACE) }
                        showReplaceConfirm = false
                        replaceUri = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_overwrite),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

