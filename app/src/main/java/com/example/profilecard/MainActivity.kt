package com.example.profilecard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

data class Follower(val id: Int, val name: String, val isFollowing: Boolean)

class ProfileViewModel : androidx.lifecycle.ViewModel() {
    var name by mutableStateOf("Bolashak Kulmukhambetov")
    var bio by mutableStateOf("Android Developer")

    var followers by mutableStateOf(List(8) { i -> Follower(i, "Follower $i", false) })
        internal set

    fun toggleFollow(id: Int) {
        followers = followers.map { if (it.id == id) it.copy(isFollowing = !it.isFollowing) else it }
    }

    fun removeFollower(f: Follower) { followers = followers - f }

    fun addFollower() {
        val nextId = (followers.maxOfOrNull { it.id } ?: -1) + 1
        followers = followers + Follower(nextId, "Follower $nextId", false)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                val vm: ProfileViewModel = viewModel()
                AppNavHost(nav, vm)
            }
        }
    }
}

@Composable
fun AppNavHost(nav: NavHostController, vm: ProfileViewModel) {
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("profile") { ProfileScreen(nav, vm) }
        composable("editProfile") { EditProfileScreen(nav, vm) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Home") }) }
    ) { p ->
        Box(Modifier.padding(p).fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { nav.navigate("profile") }) { Text("Go to Profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController, vm: ProfileViewModel) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                actions = {
                    TextButton(onClick = { nav.navigate("editProfile") }) { Text("Edit") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { vm.addFollower() }) { Text("Add follower") }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(name = vm.name, bio = vm.bio, followersCount = vm.followers.size)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = vm.followers,
                    key = { it.id }
                ) { f ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { v ->
                            if (v == SwipeToDismissBoxValue.EndToStart) {
                                val old = vm.followers
                                vm.removeFollower(f)
                                scope.launch {
                                    val res = snackbarHost.showSnackbar("${f.name} removed", "Undo")
                                    if (res == SnackbarResult.ActionPerformed) {
                                        vm.followers = old
                                    }
                                }
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
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
                                    modifier = Modifier.padding(end = 20.dp)
                                )
                            }
                        },
                        content = {
                            FollowerRow(
                                follower = f,
                                onFollowToggle = {
                                    vm.toggleFollow(f.id)
                                    scope.launch {
                                        snackbarHost.showSnackbar(
                                            if (f.isFollowing) "Unfollowed ${f.name}"
                                            else "Followed ${f.name}"
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, bio: String, followersCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(bio, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("$followersCount followers", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(nav: NavHostController, vm: ProfileViewModel) {
    var name by remember { mutableStateOf(TextFieldValue(vm.name)) }
    var bio by remember { mutableStateOf(TextFieldValue(vm.bio)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    vm.name = it.text
                },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = {
                    bio = it
                    vm.bio = it.text
                },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { nav.popBackStack() },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save") }
        }
    }
}

@Composable
fun FollowerRow(
    follower: Follower,
    onFollowToggle: () -> Unit
) {
    val container by animateColorAsState(
        if (follower.isFollowing) Color(0xFFE57373) else Color(0xFFE0E0E0),
        label = "followColorAnim"
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(follower.name, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onFollowToggle,
                colors = ButtonDefaults.buttonColors(containerColor = container)
            ) {
                Text(if (follower.isFollowing) "Unfollow" else "Follow")
            }
        }
    }
}
