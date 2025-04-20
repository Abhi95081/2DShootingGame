package com.example.bubbleshooter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BubbleShooterGame()
        }
    }
}

@Composable
fun BubbleShooterGame() {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Use remember to keep the screen size consistent
    val screenWidthPx by remember(configuration, density) {
        mutableFloatStateOf(with(density) { configuration.screenWidthDp.dp.toPx() })
    }
    val screenHeightPx by remember(configuration, density) {
        mutableFloatStateOf(with(density) { configuration.screenHeightDp.dp.toPx() })
    }

    val bullets = remember { mutableStateListOf<Bullet>() }
    val enemies = remember { mutableStateListOf<Enemy>() }
    val explosions = remember { mutableStateListOf<Explosion>() }

    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var gameOver by remember { mutableStateOf(false) }

    LaunchedEffect(gameOver) {
        while (!gameOver) {
            delay(16L) // ~60 FPS

            if (Random.nextFloat() < (0.01f + 0.002f * level)) {
                enemies.add(Enemy.createRandom(screenWidthPx, level))
            }

            bullets.forEach { it.update() }
            enemies.forEach { it.update() }
            explosions.forEach { it.update() }

            bullets.removeAll { it.isOffScreen() }
            enemies.removeAll { it.isOffScreen(screenHeightPx) }
            explosions.removeAll { it.isFinished() }

            val hitBullets = mutableListOf<Bullet>()
            val hitEnemies = mutableListOf<Enemy>()

            for (bullet in bullets) {
                for (enemy in enemies) {
                    if (enemy.isHit(bullet.position)) {
                        hitBullets.add(bullet)
                        enemy.health -= 1
                        explosions.add(Explosion(enemy.position))
                        if (enemy.health <= 0) {
                            hitEnemies.add(enemy)
                            score += enemy.points
                        }
                        break
                    }
                }
            }
            bullets.removeAll(hitBullets)
            enemies.removeAll(hitEnemies)

            level = (score / 100) + 1

            enemies.forEach { enemy ->
                if (enemy.position.y >= screenHeightPx) {
                    gameOver = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (!gameOver) {
                        bullets.add(Bullet(position = Offset(offset.x, screenHeightPx - 100f)))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            bullets.forEach { bullet ->
                drawCircle(color = bullet.color, radius = 10f, center = bullet.position)
            }
            enemies.forEach { enemy ->
                drawCircle(color = enemy.color, radius = enemy.radius, center = enemy.position)
            }
            explosions.forEach { explosion ->
                drawCircle(color = explosion.color, radius = explosion.radius, center = explosion.position)
            }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Text("Score: $score", color = Color.White, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Level: $level", color = Color.White, fontSize = 16.sp)
        }

        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", color = Color.White, fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Final Score: $score", color = Color.White, fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reached Level: $level", color = Color.White, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            bullets.clear()
                            enemies.clear()
                            explosions.clear()
                            score = 0
                            level = 1
                            gameOver = false
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

// Bullet class
data class Bullet(
    var position: Offset,
    private val speed: Float = 14f,
    val color: Color = Color.Yellow
) {
    fun update() {
        position = position.copy(y = position.y - speed)
    }
    fun isOffScreen() = position.y < 0
}

// Enemy class
data class Enemy(
    var position: Offset,
    var health: Int,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val points: Int
) {
    fun update() {
        position = position.copy(y = position.y + speed)
    }

    fun isOffScreen(screenHeightPx: Float) = position.y - radius > screenHeightPx

    fun isHit(bulletPos: Offset): Boolean {
        return (position - bulletPos).getDistance() < radius + 10f
    }

    companion object {
        fun createRandom(screenWidthPx: Float, level: Int): Enemy {
            val xPos = Random.nextFloat() * screenWidthPx
            return when (Random.nextInt(3)) {
                0 -> Enemy(
                    position = Offset(xPos, 0f),
                    health = 1,
                    speed = 2f + level * 0.5f,
                    radius = 30f,
                    color = Color.Red,
                    points = 10
                )
                1 -> Enemy(
                    position = Offset(xPos, 0f),
                    health = 2,
                    speed = 1.8f + level * 0.4f,
                    radius = 35f,
                    color = Color.Magenta,
                    points = 20
                )
                else -> Enemy(
                    position = Offset(xPos, 0f),
                    health = 3,
                    speed = 1.5f + level * 0.3f,
                    radius = 40f,
                    color = Color.Cyan,
                    points = 30
                )
            }
        }
    }
}

// Explosion class
data class Explosion(
    var position: Offset,
    var radius: Float = 10f,
    val color: Color = Color.Yellow,
    var life: Int = 10
) {
    fun update() {
        radius += 4f
        life -= 1
    }
    fun isFinished() = life <= 0
}

// Helper function
fun Offset.getDistance(): Float = sqrt(x * x + y * y)
