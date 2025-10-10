package com.example.profilecard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.profilecard.ui.theme.ProfileCardTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfileCardTheme {
                ProfileScreen(
                    screenTitle = "Profile",
                    name = "Bolashak Kulmukhambetov",
                    bio = "Android Developer"
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
    screenTitle: String,
    name: String,
    bio: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isFollowing by rememberSaveable { mutableStateOf(false) }
    var followers by rememberSaveable { mutableStateOf(100) }
    var showUnfollowDialog by rememberSaveable { mutableStateOf(false) }

    if (showUnfollowDialog) {
        AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = { Text("Unfollow $name?") },
            text = { Text("You will stop seeing updates from this profile.") },
            confirmButton = {
                TextButton(onClick = {
                    if (isFollowing) {
                        isFollowing = false
                        followers = (followers - 1).coerceAtLeast(0)
                    }
                    showUnfollowDialog = false
                }) { Text("Unfollow") }
            },
            dismissButton = {
                TextButton(onClick = { showUnfollowDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            CenterAlignedTopAppBar(
                title = { Text(screenTitle) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        ProfileCard(
            name = name,
            bio = bio,
            isFollowing = isFollowing,
            followers = followers,
            onFollowClick = {
                isFollowing = true
                followers += 1
                scope.launch {
                    val res = snackbarHostState.showSnackbar(
                        message = "Now following $name",
                        actionLabel = "Undo",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        isFollowing = false
                        followers = (followers - 1).coerceAtLeast(0)
                        snackbarHostState.showSnackbar("Undid follow")
                    }
                }
            },
            onUnfollowRequest = { showUnfollowDialog = true },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}


@Composable
fun ProfileCard(
    name: String,
    bio: String,
    isFollowing: Boolean,
    followers: Int,
    onFollowClick: () -> Unit,
    onUnfollowRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val followColor by animateColorAsState(
        targetValue = if (isFollowing) Color.Red else Color.White,
        label = "followColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Cyan),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.height(8.dp))

                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(bio, style = MaterialTheme.typography.bodyMedium)

                Text(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    text = "$followers Followers"
                )

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        if (isFollowing) onUnfollowRequest() else onFollowClick()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = followColor)
                ) {
                    Text(
                        if (isFollowing) "Unfollow" else "Follow",
                        color = if (isFollowing) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true,
    showSystemUi = true)
@Composable
fun GreetingPreview() {
    ProfileCardTheme {
        ProfileScreen(
            screenTitle = "Profile",
            name = "Bolashak Kulmukhambetov",
            bio = "Android Developer"
        )
    }
}
