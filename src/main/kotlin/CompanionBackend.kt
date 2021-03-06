import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.apache.commons.validator.routines.InetAddressValidator
import java.awt.GraphicsEnvironment
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.print.attribute.standard.MediaSize


fun Application.module() {
    install(ContentNegotiation) {
        gson { }
        json()
    }
    install(PartialContent)
    install(AutoHeadResponse)
}


fun Application.configureRouting() {

    routing {
        get("/api/get/check/connection") {
            call.respond(HttpStatusCode.OK)
        }
        get("/api/get/settings") {
            call.respond(ViewModel.settings.value.toSettingsData())
        }

        get("/api/get/print_services") {
            call.respond(ViewModel.getPrintServices().map { it.name })
        }

        get("/api/get/media_size_names") {
            if (call.request.queryParameters["print_service"] == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val printServiceName = call.request.queryParameters["print_service"]!!

            ViewModel.findPrintService(printServiceName)?.let { printService ->
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

                ViewModel.findMediaSizeName(mediaSizeNameName, printServiceName)?.let { mediaSizeName ->
                    MediaSize.getMediaSizeForName(mediaSizeName)?.let { mediaSize ->
                        ViewModel.getLayouts().findRelevant(mediaSize).map { it.name }.also { call.respond(it) }
                    } ?: call.respond(HttpStatusCode.BadRequest)
                } ?: call.respond(HttpStatusCode.BadRequest)
            }
        }

        get("/api/get/layout_with_photos") {
            with(call.request) {
                if (queryParameters["layout"] == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val layoutName = queryParameters["layout"]!!

                val layout = ViewModel.getLayoutByName(layoutName)

                if (layout.layoutSize == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val image = ViewModel.generateLayout(
                    layout,
                    List(layout.getCaptureCount()) {
                        File(this.javaClass.classLoader.getResource("sample.png")!!.toURI())
                    },
                    1f
                )

                val baos = ByteArrayOutputStream()
                ImageIO.write(
                    image.toComposeImageBitmap().toAwtImage(), "png", baos
                )

                call.respondBytes(baos.toByteArray(), contentType = ContentType.defaultForFileExtension("png"))
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

                call.respond(ViewModel.checkPhotoserverConnection(queryParameters["inet_address"]!!))
            }
        }

        get("/api/get/fonts") {
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.also { call.respond(it) }
        }

        get("/api/get/cameras") {
            ViewModel.cameraList.value.map { it.cameraName }.also { call.respond(it) }
        }

        get("/api/get/camera/configs") {
            if (call.request.queryParameters["camera_name"] == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val camera = ViewModel.getCameraByName(call.request.queryParameters["camera_name"]!!)

            if (camera == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            camera.gP2Camera.config.filter {
                (it.path.startsWith("/main/imgsettings", ignoreCase = true) ||
                        it.path.startsWith("/main/capturesettings", ignoreCase = true))
                        && it.choices != null && it.choices.size > 1
            }.map {
                CameraConfigEntry(
                    configName = it.label,
                    value = it.value.toString(),
                    choices = it.choices.toList()
                )
            }.also { call.respond(it) }

            camera.gP2Camera.release()
        }

        post("/api/post/settings") {
            val settingsData = try {
                call.receive<SettingsData>()
            } catch (e: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            ViewModel.updateSettings(settingsData)
            call.respond(HttpStatusCode.OK)
        }

        post("/api/post/camera_config") {
            val cameraConfiguration = try {
                call.receive<List<CameraConfigEntry>>()
            } catch (e: ContentTransformationException) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            ViewModel.updateCameraConfiguration(cameraConfiguration)
            call.respond(HttpStatusCode.OK)
        }

        post("/api/post/lock") {
            call.respond(ViewModel.lockMirror())
        }

        post("/api/post/unlock") {
            call.respond(ViewModel.unlockMirror())
        }

        post("/api/post/shutdown") {
            call.respond(true)
            ViewModel.shutdownMirror()
        }

        get("/api/get/events") {
            call.request.queryParameters["last_id"].also { lastId ->
                if (lastId == null || lastId.toIntOrNull() == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                if (ViewModel.events.isEmpty()) {
                    call.respond(emptyList<Event>())
                    return@get
                }
                if (lastId.toInt() !in ViewModel.events.indices) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                call.respond(ViewModel.events.subList(lastId.toInt(), ViewModel.events.size - 1))
            }
        }

        get("/api/get/last_event_id") {
            ViewModel.events.lastIndex.also { call.respond(if (it == -1) 0 else it) }
        }
    }
}


