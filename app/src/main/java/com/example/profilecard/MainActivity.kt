package com.example.profilecard

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET

data class Profile(
    val name: String,
    val bio: String
)

@Immutable
data class Follower(
    val id: Int,
    val name: String,
    val isFollowing: Boolean
)

@Immutable
data class ProfileStats(
    val followersCount: Int,
    val followingCount: Int
)

@Entity(tableName = "followers")
data class FollowerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val isFollowing: Boolean
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val bio: String
)

@Dao
interface FollowerDao {
    @Query("SELECT * FROM followers ORDER BY id")
    fun getFollowersFlow(): Flow<List<FollowerEntity>>

    @Query("SELECT COUNT(*) FROM followers")
    suspend fun countFollowers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<FollowerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FollowerEntity)

    @Update
    suspend fun update(entity: FollowerEntity)

    @Delete
    suspend fun delete(entity: FollowerEntity)

    @Query("SELECT * FROM followers WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): FollowerEntity?

    @Query("DELETE FROM followers")
    suspend fun deleteAll()

    @Query("SELECT MAX(id) FROM followers")
    suspend fun getMaxId(): Int?
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun getProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
}

@Database(
    entities = [FollowerEntity::class, ProfileEntity::class],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followerDao(): FollowerDao
    abstract fun profileDao(): ProfileDao
}

data class ApiUser(
    val id: Int,
    val name: String
)

interface JsonPlaceholderApi {
    @GET("users")
    suspend fun getUsers(): List<ApiUser>
}

fun FollowerEntity.toDomain(): Follower =
    Follower(id = id, name = name, isFollowing = isFollowing)

fun Follower.toEntity(): FollowerEntity =
    FollowerEntity(id = id, name = name, isFollowing = isFollowing)

class ProfileRepository(
    private val followerDao: FollowerDao,
    private val profileDao: ProfileDao,
    private val api: JsonPlaceholderApi
) {
    val followersFlow: Flow<List<Follower>> =
        followerDao.getFollowersFlow().map { list -> list.map { it.toDomain() } }

    suspend fun ensureInitialData() {
        val profile = profileDao.getProfile()
        if (profile == null) {
            profileDao.upsert(
                ProfileEntity(
                    name = "Bolashak Kulmukhambetov",
                    bio = "Android Developer"
                )
            )
        }
        if (followerDao.countFollowers() == 0) {
            try {
                refreshFromRemote()
            } catch (e: Exception) {
                Log.e("ProfileRepository", "ensureInitialData refresh failed", e)
            }
        }
    }

    suspend fun loadProfile(): Profile {
        val entity = profileDao.getProfile()
        return if (entity != null) {
            Profile(entity.name, entity.bio)
        } else {
            Profile("Bolashak Kulmukhambetov", "Android Developer")
        }
    }

    suspend fun saveProfile(profile: Profile) {
        profileDao.upsert(
            ProfileEntity(
                id = 0,
                name = profile.name,
                bio = profile.bio
            )
        )
    }

    suspend fun refreshFromRemote() {
        val remote = api.getUsers()
        val entities = remote.map { u ->
            FollowerEntity(
                id = u.id,
                name = u.name,
                isFollowing = false
            )
        }
        followerDao.deleteAll()
        followerDao.insertAll(entities)
    }

    suspend fun toggleFollow(id: Int) {
        val entity = followerDao.getById(id) ?: return
        followerDao.update(entity.copy(isFollowing = !entity.isFollowing))
    }

    suspend fun removeFollower(f: Follower) {
        followerDao.delete(f.toEntity())
    }

    suspend fun insertFollower(f: Follower) {
        followerDao.insert(f.toEntity())
    }

    suspend fun addFollower() {
        val maxId = followerDao.getMaxId() ?: 0
        val newId = maxId + 1
        followerDao.insert(
            FollowerEntity(
                id = newId,
                name = "Follower $newId",
                isFollowing = false
            )
        )
    }
}

class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    var name by mutableStateOf("Bolashak Kulmukhambetov")
        private set

    var bio by mutableStateOf("Android Developer")
        private set

    var followers by mutableStateOf<List<Follower>>(emptyList())
        internal set

    var isLoading by mutableStateOf(true)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.followersFlow.collect { list ->
                followers = list
                isLoading = false
            }
        }
        viewModelScope.launch {
            try {
                repository.ensureInitialData()
                val profile = repository.loadProfile()
                name = profile.name
                bio = profile.bio
            } catch (e: Exception) {
                name = "Bolashak Kulmukhambetov"
                bio = "Android Developer"
            }
        }
    }

    suspend fun refreshFromRemote() {
        isSyncing = true
        try {
            repository.refreshFromRemote()
        } finally {
            isSyncing = false
        }
    }

    suspend fun toggleFollow(id: Int) = repository.toggleFollow(id)
    suspend fun removeFollower(f: Follower) = repository.removeFollower(f)
    suspend fun insertFollower(f: Follower) = repository.insertFollower(f)
    suspend fun addFollower() = repository.addFollower()

    suspend fun saveProfile(newName: String, newBio: String) {
        name = newName
        bio = newBio
        repository.saveProfile(Profile(newName, newBio))
    }

    class Factory(
        private val repository: ProfileRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "profile-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        val api = retrofit.create(JsonPlaceholderApi::class.java)

        val repository = ProfileRepository(
            followerDao = db.followerDao(),
            profileDao = db.profileDao(),
            api = api
        )

        profileViewModel = ViewModelProvider(
            this,
            ProfileViewModel.Factory(repository)
        )[ProfileViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                AppNavHost(nav, profileViewModel)
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
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Home") })
        }
    ) { p ->
        Box(
            Modifier
                .padding(p)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { nav.navigate("profile") }) { Text("Go to Profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavHostController, vm: ProfileViewModel) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val stats by remember {
        derivedStateOf {
            val list = vm.followers
            ProfileStats(
                followersCount = list.size,
                followingCount = list.count { it.isFollowing }
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    vm.refreshFromRemote()
                                    snackbarHost.showSnackbar(
                                        "Refreshed from network, followers=${vm.followers.size}"
                                    )
                                } catch (e: Exception) {
                                    snackbarHost.showSnackbar(
                                        "Refresh failed: ${e::class.simpleName} ${e.message}"
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    TextButton(onClick = { nav.navigate("editProfile") }) { Text("Edit") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { scope.launch { vm.addFollower() } }
            ) { Text("Add follower") }
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(
                name = vm.name,
                bio = vm.bio,
                stats = stats,
                isLoading = vm.isLoading,
                isSyncing = vm.isSyncing
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = vm.followers,
                    key = { _, f -> f.id }
                ) { index, f ->

                    var itemVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(f.id) {
                        delay(index * 40L)
                        itemVisible = true
                    }

                    AnimatedVisibility(
                        visible = itemVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { v ->
                                if (v == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        vm.removeFollower(f)
                                        val res = snackbarHost.showSnackbar(
                                            "${f.name} removed",
                                            "Undo"
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            vm.insertFollower(f)
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
                                        scope.launch {
                                            vm.toggleFollow(f.id)
                                            snackbarHost.showSnackbar(
                                                if (f.isFollowing) "Unfollowed ${f.name}"
                                                else "Followed ${f.name}"
                                            )
                                        }
                                    },
                                    isLoading = vm.isLoading
                                )
                            }
                        )
                    }
                }

                if (vm.isLoading) {
                    items(6) { _: Int ->
                        ShimmerFollowerRow()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    bio: String,
    stats: ProfileStats,
    isLoading: Boolean,
    isSyncing: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                isLoading = isLoading,
                isSyncing = isSyncing
            )

            Spacer(Modifier.width(16.dp))

            ProfileInfo(
                name = name,
                bio = bio,
                stats = stats,
                isLoading = isLoading,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    isLoading: Boolean,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val avatarScale by animateFloatAsState(
        targetValue = if (isSyncing) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessLow
        ),
        label = "avatarScale"
    )

    val infinite = rememberInfiniteTransition(label = "onlinePulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = modifier) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(64.dp)
                .scale(avatarScale)
                .clip(CircleShape)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
    }
}

@Composable
private fun ProfileInfo(
    name: String,
    bio: String,
    stats: ProfileStats,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var statsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        if (!isLoading) statsVisible = true
    }
    val statsAlpha by animateFloatAsState(
        targetValue = if (statsVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "statsAlpha"
    )

    Column(modifier) {
        Text(
            name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.placeholder(
                visible = isLoading,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
        Text(
            bio,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.placeholder(
                visible = isLoading,
                highlight = PlaceholderHighlight.shimmer()
            )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${stats.followersCount} followers • ${stats.followingCount} following",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .alpha(statsAlpha)
                .placeholder(
                    visible = isLoading,
                    highlight = PlaceholderHighlight.shimmer()
                )
        )
        Spacer(Modifier.height(4.dp))
        RecompositionCounter(label = "Header")
    }
}

@Composable
fun FollowerRow(
    follower: Follower,
    onFollowToggle: () -> Unit,
    isLoading: Boolean
) {
    val container by animateColorAsState(
        if (follower.isFollowing) Color(0xFF81C784) else Color(0xFFE0E0E0),
        animationSpec = tween(250),
        label = "followColorAnim"
    )

    val context = LocalContext.current
    val avatarUrl = remember(follower.id) {
        "https://api.dicebear.com/9.x/identicon/png?seed=${follower.id}"
    }
    val imageRequest = remember(avatarUrl) {
        ImageRequest.Builder(context)
            .data(avatarUrl)
            .crossfade(true)
            .build()
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .placeholder(
                                visible = isLoading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            follower.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.placeholder(
                                visible = isLoading,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                        )
                        RecompositionCounter(
                            label = "Row ${follower.id}"
                        )
                    }
                }

                val scale by animateFloatAsState(
                    targetValue = if (follower.isFollowing) 1.03f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "followBtnScale"
                )

                Button(
                    onClick = onFollowToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = container),
                    modifier = Modifier
                        .scale(scale)
                        .height(40.dp)
                        .defaultMinSize(minWidth = 110.dp)
                ) {
                    Text(if (follower.isFollowing) "Following" else "Follow")
                }
            }
        }
    }
}

@Composable
private fun ShimmerFollowerRow() {
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
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .height(18.dp)
                        .width(140.dp)
                        .placeholder(
                            visible = true,
                            highlight = PlaceholderHighlight.shimmer()
                        )
                )
            }

            Box(
                Modifier
                    .height(36.dp)
                    .width(90.dp)
                    .clip(MaterialTheme.shapes.small)
                    .placeholder(
                        visible = true,
                        highlight = PlaceholderHighlight.shimmer()
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(nav: NavHostController, vm: ProfileViewModel) {
    var name by remember { mutableStateOf(TextFieldValue(vm.name)) }
    var bio by remember { mutableStateOf(TextFieldValue(vm.bio)) }
    val scope = rememberCoroutineScope()

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
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        vm.saveProfile(name.text, bio.text)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("Save") }
        }
    }
}

@Composable
fun RecompositionCounter(
    label: String,
    modifier: Modifier = Modifier
) {
    var count by remember { mutableIntStateOf(0) }
    SideEffect { count++ }
    Text(
        text = "$label recompositions: $count",
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        val nav = rememberNavController()
        HomeScreen(nav)
    }
}
