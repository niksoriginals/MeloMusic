package com.niksoriginals.melo.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.niksoriginals.melo.R
import com.niksoriginals.melo.constants.ProfileCustomImageUriKey
import com.niksoriginals.melo.constants.ProfileCustomStatusKey
import com.niksoriginals.melo.constants.ProfileNameKey
import com.niksoriginals.melo.utils.rememberPreference
import java.io.File

@Composable
fun ProfileHeader(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val defaultName = stringResource(R.string.default_profile_name)
    val (profileName, onProfileNameChange) = rememberPreference(ProfileNameKey, defaultValue = "")
    val displayProfileName = remember(profileName, defaultName) {
        if (profileName.isBlank() || profileName.trim().startsWith("Ashuuu", ignoreCase = true)) {
            defaultName
        } else {
            profileName
        }
    }
    val (profileImageUri, onProfileImageUriChange) = rememberPreference(ProfileCustomImageUriKey, defaultValue = "")
    val (profileStatus, onProfileStatusChange) = rememberPreference(ProfileCustomStatusKey, defaultValue = "")
    val cleanStatus = if (profileStatus == "0 followers • 0 following") "" else profileStatus

    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top row with Settings icon on the top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.settings),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Avatar + Name & Followers Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Circular Avatar
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { showEditDialog = true },
                contentAlignment = Alignment.Center
            ) {
                val avatarFile = remember(profileImageUri) {
                    if (profileImageUri.isNotBlank()) File(profileImageUri) else null
                }
                if (avatarFile != null && avatarFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarFile)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = "Default Avatar",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // Name and Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = displayProfileName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (cleanStatus.isNotBlank()) {
                    Text(
                        text = cleanStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions: Stadium-pill Edit button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { showEditDialog = true },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.edit_profile),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showEditDialog) {
        EditProfileDialog(
            currentName = if (displayProfileName == defaultName) "" else displayProfileName,
            currentStatus = cleanStatus,
            currentImageUri = profileImageUri,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newStatus, newImageUri ->
                if (newImageUri.isBlank() && profileImageUri.isNotBlank()) {
                    try {
                        File(profileImageUri).delete()
                    } catch (_: Exception) {}
                }
                onProfileNameChange(newName)
                onProfileStatusChange(newStatus)
                onProfileImageUriChange(newImageUri)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentName: String,
    currentStatus: String,
    currentImageUri: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var tempName by rememberSaveable { mutableStateOf(currentName) }
    var tempStatus by rememberSaveable { mutableStateOf(currentStatus) }
    var tempImageUri by rememberSaveable { mutableStateOf(currentImageUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.filesDir.listFiles { _, name -> name.startsWith("user_profile_avatar") }?.forEach { f ->
                    f.delete()
                }
                val avatarFile = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempImageUri = avatarFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarFile = remember(tempImageUri) {
                        if (tempImageUri.isNotBlank()) File(tempImageUri) else null
                    }
                    if (avatarFile != null && avatarFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(avatarFile)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = "Profile Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Photo actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Text(stringResource(R.string.change_photo))
                    }
                    if (tempImageUri.isNotBlank()) {
                        TextButton(
                            onClick = { tempImageUri = "" },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.remove_photo))
                        }
                    }
                }

                // Name TextField
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    placeholder = { Text(stringResource(R.string.default_profile_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status / Subtext TextField
                OutlinedTextField(
                    value = tempStatus,
                    onValueChange = { tempStatus = it },
                    label = { Text("Status / Subtext (Optional)") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        tempName.trim().ifEmpty { "User" },
                        tempStatus.trim(),
                        tempImageUri
                    )
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50)
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
