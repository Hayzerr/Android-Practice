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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfileCardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProfileCard(
                        name = "Bolashak Kulmukhambetov",
                        bio = "Android Developer",
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCard(
    name: String,
    bio: String,
    modifier: Modifier = Modifier
) {

    var isFollowing by rememberSaveable { mutableStateOf(false) }
    var followers by rememberSaveable { mutableStateOf(100) }
    val followColor by animateColorAsState(
        targetValue = if (isFollowing) Color.Red else Color.White
        , label = "followColor"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,

    ) {
        Column(
            modifier = Modifier.padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Cyan)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Avatar",
                modifier = Modifier.size(100.dp)
                    .clip(CircleShape)
            )

            Spacer(Modifier.height(8.dp))

            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = bio, style = MaterialTheme.typography.bodyMedium)

            Text(modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                text = "$followers Followers")

            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                if (isFollowing)
                    followers--
                else followers++
                isFollowing = !isFollowing},
                colors = ButtonDefaults.buttonColors(containerColor = followColor)
            ) {
                Text(if (isFollowing) "Unfollow" else "Follow",
                    color = (if (isFollowing) Color.White else Color.Black))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProfileCardTheme {
        ProfileCard(
            name = "Bolashak Kulmukhambetov",
            bio = "Android Developer"
        )
    }
}
