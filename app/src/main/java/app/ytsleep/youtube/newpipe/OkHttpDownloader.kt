package app.ytsleep.youtube.newpipe

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder().url(request.url())

        for ((name, values) in request.headers()) {
            for (value in values) builder.addHeader(name, value)
        }

        val body = request.dataToSend()
        val method = request.httpMethod()
        val okBody = body?.toRequestBody("application/json".toMediaTypeOrNull())

        builder.method(method, if (method == "GET" || method == "HEAD") null else okBody)

        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw IOException("Network error: ${e.message}", e)
        }

        if (response.code == 429) {
            response.body?.close()
            throw ReCaptchaException("Rate limited (429)", request.url())
        }

        val responseBody = response.body?.string() ?: ""
        val headers = response.headers.toMultimap()

        return Response(response.code, response.message, headers, responseBody, request.url())
    }
}
