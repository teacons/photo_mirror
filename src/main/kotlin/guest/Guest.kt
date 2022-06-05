package guest

import ImagePrintable
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import findMediaSizeName
import findPrintService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import layoutEditor.DraggableEditor
import layoutEditor.PhotoLayer
import layoutEditor.PhotoLayerWithPhoto
import layoutEditor.TextLayer
import loadImageBitmap
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
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

    val layout by remember { mutableStateOf(transaction { settings.layout!! }) }


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
            loadImageBitmap(File(guestBackgroundFilepath)),
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
                    count = guestShootTimer
                    state = MainState.Shoot
                }
            }
            if (state == MainState.Shoot) {
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val t = settings.camera!!.camera!!.captureImage()
                        val l = settings.camera!!.camera!!.getCameraFileContents(t.path, t.name)
                        val sdf = SimpleDateFormat("dd-MM-yyyy")
                        val imageFile = File("${sdf.format(Date())}${File.separator}${t.name}").apply {
                            parentFile.mkdirs()
                            createNewFile()
                            outputStream().write(l)
                        }
                        images.add(imageFile)
                        state = if (layout.getPhotoLayersMaxId() > images.size) MainState.Timer
                        else {
                            settings.camera!!.camera!!.release()
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
                    val scale = layout.widthInPx / layout.layoutWidth!!
                    val print = renderComposeScene(layout.layoutWidth!! * scale, layout.layoutHeight!! * scale) {
                        DraggableEditor(
                            layout.getLayers().map {
                                when (it) {
                                    is PhotoLayer -> PhotoLayerWithPhoto(
                                        it.name,
                                        mutableStateOf(it.offset.value * scale.toFloat()),
                                        it.scale,
                                        it.rotation,
                                        it.photoId,
                                        it.width,
                                        it.height,
                                        images[it.photoId - 1]
                                    )
                                    is TextLayer -> it.apply {
                                        it.offset = mutableStateOf(it.offset.value * scale.toFloat())
                                        it.fontSize *= scale
                                    }

                                    else -> it.apply { it.offset = mutableStateOf(it.offset.value * scale.toFloat()) }
                                }
                            },
                            IntSize(layout.layoutWidth!! * scale, layout.layoutHeight!! * scale),
                            null,
                            {},
                            {}
                        )
                    }.toComposeImageBitmap().toAwtImage()

                    images.clear()

                    val printService = findPrintService(settings.printerName!!)!!
                    val job = printService.createPrintJob()
                    val mediaSizeName = findMediaSizeName(settings.printerMediaSizeName!!, settings.printerName!!)!!
                    val printAttributes = HashPrintRequestAttributeSet().apply {
                        if (print.width >= print.height) add(OrientationRequested.LANDSCAPE)
                        else add(OrientationRequested.PORTRAIT)
                        add(mediaSizeName)

                        MediaSize.getMediaSizeForName(mediaSizeName).getSize(MediaSize.INCH).let {
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
                        MainState.Welcome -> guestHelloText
                        MainState.Shoot -> guestShootText
                        MainState.ShootEnd -> guestWaitText
                        MainState.Timer -> "$targetCount"
                    },
                    fontSize = guestTextFontSize.sp,
                    fontFamily = fontFamily,
                    color = Color(fontColor.toULong()),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(600.dp)
                )
            }
        }
    }
}
