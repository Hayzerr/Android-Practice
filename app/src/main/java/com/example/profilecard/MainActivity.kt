package com.example.profilecard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

data class Follower(val id: Int, val name: String, val isFollowing: Boolean)

@Composable
fun ProfileScreen(
    screenTitle: String,
    name: String,
    bio: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isFollowing by rememberSaveable { mutableStateOf(false) }
    var followersCount by rememberSaveable { mutableStateOf(100) }
    var showUnfollowDialog by rememberSaveable { mutableStateOf(false) }

    var followerList by rememberSaveable {
        mutableStateOf(List(10) { i -> Follower(i, "Follower $i", isFollowing = false) })
    }

    if (showUnfollowDialog) {
        AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = { Text("Unfollow $name?") },
            text = { Text("You will stop seeing updates from this profile.") },
            confirmButton = {
                TextButton(onClick = {
                    if (isFollowing) {
                        isFollowing = false
                        followersCount = (followersCount - 1).coerceAtLeast(0)
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
            CenterAlignedTopAppBar(title = { Text(screenTitle) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            StoriesCarousel()

            ProfileCard(
                name = name,
                bio = bio,
                isFollowing = isFollowing,
                followers = followersCount,
                onFollowClick = {
                    isFollowing = true
                    followersCount += 1
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "Now following $name",
                            actionLabel = "Undo"
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            isFollowing = false
                            followersCount = (followersCount - 1).coerceAtLeast(0)
                        }
                    }
                },
                onUnfollowRequest = { showUnfollowDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            FollowersList(
                followers = followerList,
                onFollowToggle = { id ->
                    followerList = followerList.map {
                        if (it.id == id) it.copy(isFollowing = !it.isFollowing) else it
                    }
                },
                onRemoveFollower = { removed ->
                    val oldList = followerList
                    followerList = followerList - removed

                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "${removed.name} removed",
                            actionLabel = "Undo"
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            followerList = oldList
                        }
                    }
                }
            )
        }
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
            .fillMaxWidth()
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

@Composable
fun StoriesCarousel() {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(10) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = "Story $index",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                )
                Text("Story $index", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersList(
    followers: List<Follower>,
    onFollowToggle: (Int) -> Unit,
    onRemoveFollower: (Follower) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        items(
            items = followers,
            key = { it.id }
        ) { follower ->

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        scope.launch { onRemoveFollower(follower) }
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.White,
                            modifier = Modifier.padding(end = 24.dp)
                        )
                    }
                },
                content = {
                    FollowerItem(
                        follower = follower,
                        onFollowToggle = { onFollowToggle(follower.id) }
                    )
                }
            )
        }
    }
}

@Composable
fun FollowerItem(
    follower: Follower,
    onFollowToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Follower avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(follower.name)
        }

        OutlinedButton(onClick = onFollowToggle) {
            Text(if (follower.isFollowing) "Unfollow" else "Follow")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewAll() {
    ProfileCardTheme {
        ProfileScreen(
            screenTitle = "Profile",
            name = "Bolashak Kulmukhambetov",
            bio = "Android Developer"
        )
    }
}
