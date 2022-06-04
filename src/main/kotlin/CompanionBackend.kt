import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.renderComposeScene
import androidx.compose.ui.unit.IntSize
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import layoutEditor.DraggableEditor
import layoutEditor.PhotoLayer
import layoutEditor.PhotoLayerWithPhoto
import org.apache.commons.validator.routines.InetAddressValidator
import org.jetbrains.exposed.sql.transactions.transaction
import settings.getSupportedMediaSizeNames
import java.awt.GraphicsEnvironment
import java.io.File
import java.net.InetAddress
import javax.imageio.ImageIO
import javax.print.PrintService
import javax.print.PrintServiceLookup
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import kotlin.math.floor


fun Application.module() {
    install(ContentNegotiation) {
        gson { }
        json()
    }
    install(PartialContent)
    install(AutoHeadResponse)
}

fun getPrintServices(): List<PrintService> =
    PrintServiceLookup.lookupPrintServices(null, null)?.toList() ?: emptyList()

fun findPrintService(name: String): PrintService? = getPrintServices().find { it.name == name }

fun findMediaSizeName(mediaSizeName: String, printServiceName: String): MediaSizeName? =
    findPrintService(printServiceName)?.getSupportedMediaSizeNames()?.find { it.toString() == mediaSizeName }


@OptIn(ExperimentalComposeUiApi::class)
fun Application.configureRouting() {

    routing {
        get("/api/get/settings") {
            val settings = transaction { Settings.all().firstOrNull() }
            if (settings == null || !settings.isValid()){
                call.respond(HttpStatusCode.InternalServerError)
                return@get
            }
            call.respond(settings.toSettingsData()!!)
        }

        get("/api/get/print_services") {
            call.respond(getPrintServices().map { it.name })
        }

        get("/api/get/media_size_names") {
            if (call.request.queryParameters["print_service"] == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val printServiceName = call.request.queryParameters["print_service"]!!

            findPrintService(printServiceName)?.let { printService ->
                printService.getSupportedMediaSizeNames().map { it.toString() }.also { call.respond(it) }
            } ?: call.respond(HttpStatusCode.BadRequest)
        }

        get("/api/get/layouts") {
            with(call.request) {
                if (queryParameters["print_service"] == null || queryParameters["media_size_name"] == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val printServiceName = queryParameters["print_service"]!!
                val mediaSizeNameName = queryParameters["media_size_name"]!!

                findMediaSizeName(mediaSizeNameName, printServiceName)?.let { mediaSizeName ->
                    transaction { Layout.all() }.filter {
                        with(MediaSize.getMediaSizeForName(mediaSizeName).getSize(MediaSize.MM)) {
                            floor(get(0) / it.ratioWidth.toFloat()) == floor(get(1) / it.ratioHeight.toFloat()) ||
                                    floor(get(1) / it.ratioWidth.toFloat()) == floor(get(0) / it.ratioHeight.toFloat())
                        }
                    }.map { it.name }.also { call.respond(it) }
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/api/get/layouts") {
            transaction { Layout.all() }.map { it.name }.also { call.respond(it) }
        }

        get("/api/get/layout_with_photos") {
            with(call.request) {
                if (queryParameters["layout"] == null ||
                    queryParameters["print_service"] == null ||
                    queryParameters["media_size_name"] == null
                ) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val layoutName = queryParameters["layout"]!!
                val printServiceName = queryParameters["print_service"]!!
                val mediaSizeNameName = queryParameters["media_size_name"]!!

                val layout = transaction { Layout.find { Layouts.name eq layoutName }.first() }

                if (layout.width == null || layout.height == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val width = layout.width!!
                val height = layout.height!!

                val image = renderComposeScene(width, height) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(with(
                                MediaSize.getMediaSizeForName(
                                    findMediaSizeName(mediaSizeNameName, printServiceName)
                                ).getSize(MediaSize.MM)
                            ) {
                                if (width / height >= 1)
                                    get(1) / get(0)
                                else
                                    get(0) / get(1)
                            })
                    ) {
                        DraggableEditor(
                            layout.getLayers().map {
                                if (it is PhotoLayer) {
                                    PhotoLayerWithPhoto(
                                        it.name,
                                        it.offset,
                                        it.scale,
                                        it.rotation,
                                        it.photoId,
                                        it.width,
                                        it.height,
                                        File(this.javaClass.classLoader.getResource("sample.png")!!.toURI())
                                    )
                                } else it
                            },
                            IntSize(width, height),
                            null,
                            {},
                            {}
                        )
                    }
                }

                call.respondOutputStream {
                    this.also {
                        withContext(Dispatchers.IO) {
                            ImageIO.write(
                                image.toComposeImageBitmap().toAwtImage(), "jpg", it
                            )
                        }
                    }
                }
            }
        }

        get("/api/check/inet_address") {
            with(call.request) {
                if (queryParameters["inet_address"] == null ||
                    !InetAddressValidator.getInstance().isValid(queryParameters["inet_address"])
                ) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val inetAddress =
                    InetAddress
                        .getByAddress(queryParameters["inet_address"]!!
                            .split(".")
                            .map { it.toInt().toByte() }
                            .toByteArray())

                call.respond(inetAddress.isReachable(5000))
            }
        }

        get("/api/get/fonts") {
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.also { call.respond(it) }
        }

        get("/api/get/fonts") {
            listOf(
                "Камера 1",
                "Камера 2",
                "Камера 3"
            ).also { call.respond(it) }
        }
    }
}


