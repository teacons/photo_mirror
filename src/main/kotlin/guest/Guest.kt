package guest

import Settings
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.delay
import loadImageBitmap
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.io.File

enum class MainState {
    Welcome,
    Timer,
    Shoot,
    ShootEnd
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Guest(settings: Settings) {

    val guestHelloText by remember { mutableStateOf(settings.guestHelloText!!) }
    val guestShootText by remember { mutableStateOf(settings.guestShootText!!) }
    val guestWaitText by remember { mutableStateOf(settings.guestWaitText!!) }
    val guestBackgroundFilepath by remember { mutableStateOf(settings.guestBackgroundFilepath!!) }
    val guestShootTimer by remember { mutableStateOf(settings.guestShootTimer!!) }
    val guestTextFontFamily by remember { mutableStateOf(settings.guestTextFontFamily!!) }
    val guestTextFontSize by remember { mutableStateOf(settings.guestTextFontSize!!) }
    val fontColor by remember { mutableStateOf(settings.guestTextFontColor!!) }


    val fontFamily = FontFamily(
        Typeface(
            Typeface.makeFromName(
                guestTextFontFamily,
                FontStyle.NORMAL
            )
        )
    )


    var count by remember { mutableStateOf(guestShootTimer) }

    var state by remember { mutableStateOf(MainState.Welcome) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (state == MainState.Welcome) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { state = MainState.Timer } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Image(
            loadImageBitmap(File(guestBackgroundFilepath)),
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
                text = guestHelloText,
                fontSize = guestTextFontSize.sp,
                fontFamily = fontFamily,
                color = Color(fontColor.toULong()),
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
                    count = guestShootTimer
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
                    fontSize = guestTextFontSize.sp,
                    fontFamily = fontFamily,
                    color = Color(fontColor.toULong()),
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
                text = guestShootText,
                fontSize = guestTextFontSize.sp,
                fontFamily = fontFamily,
                color = Color(fontColor.toULong()),
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
                text = guestWaitText,
                fontSize = guestTextFontSize.sp,
                fontFamily = fontFamily,
                color = Color(fontColor.toULong()),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(600.dp)
            )
        }
    }

}
