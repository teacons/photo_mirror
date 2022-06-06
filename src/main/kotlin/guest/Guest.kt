package guest

import ImagePrintable
import ViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import loadImageBitmap
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.io.File
import javax.print.DocFlavor
import javax.print.PrintException
import javax.print.SimpleDoc
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.MediaPrintableArea
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.OrientationRequested

enum class MainState {
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

    val fontFamily = FontFamily(
        Typeface(
            Typeface.makeFromName(
                guestSettings.guestTextFontFamily!!,
                FontStyle.NORMAL
            )
        )
    )


    var count by remember(settings) { mutableStateOf(guestSettings.guestShootTimer!!) }

    var state by remember { mutableStateOf(MainState.Welcome) }

    val images = remember { mutableListOf<File>() }


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
            if (state == MainState.Timer) {
                LaunchedEffect(Unit) {
                    while (count > 1) {
                        delay(1000L)
                        count--
                    }
                    delay(1000L)
                    count = guestSettings.guestShootTimer!!
                    state = MainState.Shoot
                }
            }
            if (state == MainState.Shoot) {
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        ViewModel.captureImage()?.let { imageFile -> images.add(imageFile) }
                        state = if (printerSettings.layout!!.getCaptureCount() > images.size) MainState.Timer
                        else {
                            ViewModel.cameraRelease()
                            MainState.ShootEnd
                        }
                    }
                }
            }
            if (state == MainState.ShootEnd) {
                LaunchedEffect(Unit) {
                    launch {
                        delay(10000L)
                        state = MainState.Welcome
                    }

                    val print = ViewModel.generateLayout(printerSettings.layout!!, images)

                    images.clear()

                    val job = printerSettings.printer!!.createPrintJob()
                    val printAttributes = HashPrintRequestAttributeSet().apply {
                        if (print.width >= print.height) add(OrientationRequested.LANDSCAPE)
                        else add(OrientationRequested.PORTRAIT)
                        add(printerSettings.mediaSizeName!!)

                        MediaSize.getMediaSizeForName(printerSettings.mediaSizeName).getSize(MediaSize.INCH).let {
                            add(MediaPrintableArea(0f, 0f, it[0], it[1], MediaPrintableArea.INCH))
                        }
                    }
                    val doc = SimpleDoc(
                        ImagePrintable(print),
                        DocFlavor.SERVICE_FORMATTED.PRINTABLE,
                        null
                    )
                    try {
                        job.print(doc, printAttributes)
                    } catch (e: PrintException) {
                        e.printStackTrace()
                    }
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
                        MainState.Welcome -> guestSettings.guestHelloText!!
                        MainState.Shoot -> guestSettings.guestShootText!!
                        MainState.ShootEnd -> guestSettings.guestWaitText!!
                        MainState.Timer -> "$targetCount"
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
}
