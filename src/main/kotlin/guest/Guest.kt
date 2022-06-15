package guest

import ViewModel
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import loadImageBitmap
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.io.File

enum class GuestState {
    Welcome,
    Timer,
    Shoot,
    ShootEnd
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Guest() {

    val settings by ViewModel.settings.collectAsState()

    val guestSettings = settings.guestSettings
    val printerSettings = settings.printerSettings
    val photoserverSettings = settings.photoserverSettings

    val fontFamily = FontFamily(
        Typeface(
            Typeface.makeFromName(
                guestSettings.guestTextFontFamily!!,
                FontStyle.NORMAL
            )
        )
    )


    var count by remember(settings) { mutableStateOf(guestSettings.guestShootTimer!!) }

    var state by remember { mutableStateOf(GuestState.Welcome) }

    val images = remember { mutableListOf<File>() }

    val isLocked by ViewModel.isLocked.collectAsState()


    if (!isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state == GuestState.Welcome) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { state = GuestState.Timer } else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Image(
                loadImageBitmap(File(guestSettings.guestBackgroundFilepath!!)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    color = Color.LightGray,
                    blendMode = BlendMode.Darken
                )
            )

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) {
                if (state == GuestState.Timer) {
                    LaunchedEffect(Unit) {
                        while (count > 1) {
                            delay(1000L)
                            count--
                        }
                        delay(1000L)
                        count = guestSettings.guestShootTimer!!
                        state = GuestState.Shoot
                    }
                }
                if (state == GuestState.Shoot) {
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            ViewModel.captureImage()?.let { imageFile ->
                                images.add(imageFile)
                                if (photoserverSettings.photoserverEnabled) ViewModel.sendPhotoToPhotoserver(imageFile)
                            } ?: run {
                                state = GuestState.Welcome
                            }
                            delay(1000L)
                            state = if (printerSettings.layout!!.getCaptureCount() > images.size) GuestState.Timer
                            else {
                                ViewModel.capturesToZero()
                                ViewModel.cameraRelease()
                                GuestState.ShootEnd
                            }
                        }
                    }
                }
                if (state == GuestState.ShootEnd) {
                    LaunchedEffect(Unit) {
                        launch {
                            delay(10000L)
                            state = GuestState.Welcome
                        }

                        val print = ViewModel.generateLayout(printerSettings.layout!!, images)

                        images.clear()

                        ViewModel.print(print)
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
                        text = when (it) {
                            GuestState.Welcome -> guestSettings.guestHelloText!!
                            GuestState.Shoot -> guestSettings.guestShootText!!
                            GuestState.ShootEnd -> guestSettings.guestWaitText!!
                            GuestState.Timer -> "$targetCount"
                        },
                        fontSize = guestSettings.guestTextFontSize!!.sp,
                        fontFamily = fontFamily,
                        color = Color(guestSettings.guestTextFontColor!!),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(600.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Gray),
        ) {
            Image(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
