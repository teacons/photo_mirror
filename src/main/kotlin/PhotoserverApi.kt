import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PhotoserverApi {

    @Multipart
    @POST("/mirror/photo")
    fun sendPhoto(@Part photo: MultipartBody.Part): Call<ResponseBody>
}