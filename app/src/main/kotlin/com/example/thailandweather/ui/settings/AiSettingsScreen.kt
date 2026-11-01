package com.example.thailandweather.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thailandweather.core.model.ConnectionStatus
import com.example.thailandweather.core.model.GroqModelInfo
import com.example.thailandweather.ui.theme.DribbblePalette
import com.example.thailandweather.ui.theme.NomadKitPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    viewModel: AiSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showModelSelector by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = "AI Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NomadKitPalette.PrimarySand
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
        ) {
            // 1. AI Provider Section
            item {
                SectionHeader(title = "AI Provider", icon = Icons.Default.SmartToy)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DribbblePalette.Accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Groq",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Initial supported provider (LPU inference engine)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Active") },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = NomadKitPalette.SuccessContainer,
                                    labelColor = NomadKitPalette.Success
                                ),
                                shape = RoundedCornerShape(percent = 50),
                                border = null
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Generate consolidated weather, news and alert briefings using your own API key.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = NomadKitPalette.OutlineVariant
                        )

                        // Model selection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NomadKitPalette.SurfaceVariant.copy(alpha = 0.5f))
                                .clickable { showModelSelector = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Model Selection",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = uiState.supportedModels.firstOrNull { it.id == uiState.selectedModel }?.displayName
                                        ?: uiState.selectedModel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change model",
                                tint = NomadKitPalette.PrimarySand
                            )
                        }

                        DropdownMenu(
                            expanded = showModelSelector,
                            onDismissRequest = { showModelSelector = false }
                        ) {
                            uiState.supportedModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = model.displayName, fontWeight = FontWeight.Bold)
                                                if (model.isRecommended) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Recommended",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = NomadKitPalette.TertiaryForest
                                                    )
                                                }
                                            }
                                            Text(
                                                text = model.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectModel(model.id)
                                        showModelSelector = false
                                    },
                                    leadingIcon = {
                                        if (model.id == uiState.selectedModel) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = NomadKitPalette.PrimarySand
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Groq API Key Section
            item {
                SectionHeader(title = "Groq API Key", icon = Icons.Default.Key)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (uiState.hasKey && uiState.maskedKeyDisplay != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(DribbblePalette.NeutralRose)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = NomadKitPalette.PrimarySandDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Saved Key",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NomadKitPalette.PrimarySandDeep
                                    )
                                    Text(
                                        text = uiState.maskedKeyDisplay ?: "••••••••",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NomadKitPalette.PrimarySandDeep
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = uiState.enteredKey,
                            onValueChange = { viewModel.onKeyInputChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (uiState.hasKey) "Update API Key" else "Enter Groq API Key") },
                            placeholder = { Text("gsk_...") },
                            singleLine = true,
                            visualTransformation = if (uiState.isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                viewModel.saveKey()
                            }),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleKeyVisibility() }) {
                                    Icon(
                                        imageVector = if (uiState.isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (uiState.isKeyVisible) "Hide key" else "Show key",
                                        tint = NomadKitPalette.PrimarySand
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NomadKitPalette.PrimarySand,
                                unfocusedBorderColor = NomadKitPalette.Outline,
                                focusedLabelColor = NomadKitPalette.PrimarySand
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.saveKey()
                                },
                                enabled = uiState.enteredKey.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NomadKitPalette.PrimarySand,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(percent = 50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Key")
                            }

                            if (uiState.hasKey) {
                                OutlinedButton(
                                    onClick = { viewModel.removeKey() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = NomadKitPalette.Error
                                    ),
                                    border = BorderStroke(1.dp, NomadKitPalette.Error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(percent = 50)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Link to Groq Console
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.groq.com/keys"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = NomadKitPalette.SecondaryOcean,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get an API key from console.groq.com/keys",
                                style = MaterialTheme.typography.bodySmall,
                                color = NomadKitPalette.SecondaryOcean,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "The API key is provided directly by you. Groq account limits, token quotas, and terms apply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Connection Status Section
            item {
                SectionHeader(title = "Connection Status", icon = Icons.Default.Sensors)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ConnectionStatusBadge(status = uiState.connectionStatus)

                        uiState.statusFeedback?.let { feedback ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.testConnection() },
                            enabled = uiState.hasKey && !uiState.isTesting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NomadKitPalette.PrimarySand,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(percent = 50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Testing Authenticated Connection...")
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Connection")
                            }
                        }
                    }
                }
            }

            // 4. Privacy Disclosure Section
            item {
                SectionHeader(title = "Privacy & Data Storage", icon = Icons.Default.Shield)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NomadKitPalette.SurfaceVariant),
                    border = BorderStroke(1.dp, NomadKitPalette.Outline),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = NomadKitPalette.PrimarySandDeep,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Your API key is stored on this device and is used to authenticate requests to Groq. Information included in AI prompts is sent to Groq for processing. Avoid including sensitive personal information.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI processing is performed by Groq via encrypted HTTPS requests and is not entirely on-device. You can remove your key at any time to immediately disable AI features without affecting live weather, radar, news, or official alerts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NomadKitPalette.PrimarySand,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NomadKitPalette.PrimarySandDeep
        )
    }
}

@Composable
private fun ConnectionStatusBadge(status: ConnectionStatus) {
    val config = when (status) {
        ConnectionStatus.NOT_CONFIGURED -> StatusBadgeConfig(
            icon = Icons.Default.KeyOff,
            label = "Not configured",
            containerColor = NomadKitPalette.SurfaceVariant,
            textColor = NomadKitPalette.Outline
        )
        ConnectionStatus.KEY_SAVED -> StatusBadgeConfig(
            icon = Icons.Default.Lock,
            label = "Key saved (unverified)",
            containerColor = NomadKitPalette.PrimarySandLight,
            textColor = NomadKitPalette.PrimarySandDeep
        )
        ConnectionStatus.CHECKING_CONNECTION -> StatusBadgeConfig(
            icon = Icons.Default.Sync,
            label = "Checking connection...",
            containerColor = NomadKitPalette.InfoContainer,
            textColor = NomadKitPalette.SecondaryOcean
        )
        ConnectionStatus.CONNECTED -> StatusBadgeConfig(
            icon = Icons.Default.CheckCircle,
            label = "Connected",
            containerColor = NomadKitPalette.SuccessContainer,
            textColor = NomadKitPalette.Success
        )
        ConnectionStatus.INVALID_KEY -> StatusBadgeConfig(
            icon = Icons.Default.Cancel,
            label = "Invalid key",
            containerColor = NomadKitPalette.ErrorContainer,
            textColor = NomadKitPalette.Error
        )
        ConnectionStatus.RATE_LIMIT_EXCEEDED -> StatusBadgeConfig(
            icon = Icons.Default.Warning,
            label = "Rate limit exceeded",
            containerColor = NomadKitPalette.WarningContainer,
            textColor = NomadKitPalette.Warning
        )
        ConnectionStatus.CONNECTION_FAILED -> StatusBadgeConfig(
            icon = Icons.Default.CloudOff,
            label = "Connection failed",
            containerColor = NomadKitPalette.ErrorContainer,
            textColor = NomadKitPalette.Error
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(config.containerColor)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = config.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = config.textColor
        )
    }
}

private data class StatusBadgeConfig(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val textColor: Color
)
