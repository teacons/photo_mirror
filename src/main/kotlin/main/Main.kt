package main

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.delay
import loadImageBitmap
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.io.File


fun main() = singleWindowApplication {
    MaterialTheme {
        Main(
            textWelcome = "Бесплатное фото на память",
            initialTimer = 5,
            shootText = "Сыр",
            shootEndText = "Фото будет готово через 15 секунд",
            fontSize = 120,
            fontFamily = FontFamily(Typeface(
                Typeface.makeFromName(
                    "Comic Sans MS",
                    FontStyle.NORMAL
                )
            )),
            fontColor = Color.White,
            File("background.png")
        )
    }
}

enum class MainState {
    Welcome,
    Timer,
    Shoot,
    ShootEnd
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Main(
    textWelcome: String,
    initialTimer: Int,
    shootText: String,
    shootEndText: String,
    fontSize: Int,
    fontFamily: FontFamily,
    fontColor: Color,
    backgroundFile: File
) {
//    var countDownEnabled by remember { mutableStateOf(false) }

    var count by remember { mutableStateOf(initialTimer) }

    var state by remember { mutableStateOf(MainState.Welcome) }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (state == MainState.Welcome) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { state = MainState.Timer } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            loadImageBitmap(backgroundFile),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color = Color.LightGray,
                blendMode = BlendMode.Darken
            )
        )

        AnimatedVisibility(
            visible = state == MainState.Welcome,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = textWelcome,
                fontSize = fontSize.sp,
                fontFamily = fontFamily,
                color = fontColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(600.dp)
            )
        }

        AnimatedVisibility(
            visible = state == MainState.Timer,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (state == MainState.Timer) {
                LaunchedEffect(Unit) {
                    while (count > 1) {
                        delay(1000L)
                        count--
                    }
                    delay(1000L)
                    count = initialTimer
                    state = MainState.Shoot
                }
            }
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    (slideInVertically { height -> -height } + fadeIn()
                            with slideOutVertically { height -> height } + fadeOut())
                        .using(SizeTransform(clip = false))
                }
            ) { targetCount ->
                Text(
                    text = "$targetCount",
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    color = fontColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(600.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = state == MainState.Shoot,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (state == MainState.Shoot) {
                LaunchedEffect(Unit) {
                    delay(5000L)
                    state = MainState.ShootEnd
                }
            }
            Text(
                text = shootText,
                fontSize = fontSize.sp,
                fontFamily = fontFamily,
                color = fontColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(600.dp)
            )
        }

        AnimatedVisibility(
            visible = state == MainState.ShootEnd,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (state == MainState.ShootEnd) {
                LaunchedEffect(Unit) {
                    delay(10000L)
                    state = MainState.Welcome
                }
            }
            Text(
                text = shootEndText,
                fontSize = fontSize.sp,
                fontFamily = fontFamily,
                color = fontColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(600.dp)
            )
        }
    }

}