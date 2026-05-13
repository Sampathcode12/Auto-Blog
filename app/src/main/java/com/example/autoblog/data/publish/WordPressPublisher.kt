package com.example.autoblog.data.publish

import android.content.Context
import android.net.Uri
import com.example.autoblog.model.MessageItem
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed class WordPressPublishResult {
    data class Success(val postUrls: List<String>) : WordPressPublishResult()
    data class Failure(val message: String) : WordPressPublishResult()
}

object WordPressPublisher {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    fun normalizeSiteUrl(input: String): String {
        var s = input.trim().trimEnd('/')
        while (s.endsWith("/wp-json", ignoreCase = true)) {
            s = s.removeSuffix("/wp-json").trimEnd('/')
        }
        if (!s.startsWith("http://", ignoreCase = true) &&
            !s.startsWith("https://", ignoreCase = true)
        ) {
            s = "https://$s"
        }
        return s
    }

    suspend fun publishMessages(
        context: Context,
        messages: List<MessageItem>,
        siteUrl: String,
        username: String,
        applicationPassword: String
    ): WordPressPublishResult = withContext(Dispatchers.IO) {
        if (messages.isEmpty()) {
            return@withContext WordPressPublishResult.Failure("No messages to publish")
        }
        val base = normalizeSiteUrl(siteUrl)
        if (base.isBlank() || username.isBlank() || applicationPassword.isBlank()) {
            return@withContext WordPressPublishResult.Failure("Site URL, username, and application password are required")
        }
        val appPasswordNoSpaces = applicationPassword.replace(" ", "")
        val credential = Credentials.basic(username, appPasswordNoSpaces)
        val appContext = context.applicationContext
        val published = mutableListOf<String>()
        for (msg in messages) {
            when (val step = publishOne(appContext, base, credential, msg)) {
                is StepResult.Ok -> published.add(step.postUrl)
                is StepResult.Fail -> return@withContext WordPressPublishResult.Failure(step.message)
            }
        }
        WordPressPublishResult.Success(published)
    }

    private sealed class StepResult {
        data class Ok(val postUrl: String) : StepResult()
        data class Fail(val message: String) : StepResult()
    }

    private fun publishOne(
        context: Context,
        siteBase: String,
        credential: String,
        message: MessageItem
    ): StepResult {
        val restBase = "$siteBase/wp-json/wp/v2"
        val featuredMediaId = message.imageUriString?.let { uriStr ->
            val file = materializeImageForUpload(context, uriStr) ?: return@let null
            try {
                val id = uploadMedia(restBase, credential, file)
                if (!file.delete()) file.deleteOnExit()
                id
            } catch (e: Exception) {
                if (!file.delete()) file.deleteOnExit()
                return StepResult.Fail("Image upload failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }

        val htmlContent = htmlBodyFromPlain(message.body)
        val json = JSONObject().apply {
            put("title", message.title)
            put("content", htmlContent)
            put("status", "publish")
            if (featuredMediaId != null) {
                put("featured_media", featuredMediaId)
            }
        }
        val body = json.toString().toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$restBase/posts")
            .header("Authorization", credential)
            .post(body)
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return StepResult.Fail(parseWpError(responseBody, response.code))
                }
                val obj = JSONObject(responseBody)
                val link = obj.optString("link", "").ifBlank { siteBase }
                StepResult.Ok(link)
            }
        } catch (e: Exception) {
            StepResult.Fail(e.message ?: "Network error")
        }
    }

    private fun uploadMedia(restBase: String, credential: String, file: File): Int {
        val mime = guessMime(file.name)
        val mediaType = mime.toMediaTypeOrNull() ?: "image/jpeg".toMediaType()
        val fileBody = file.asRequestBody(mediaType)
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, fileBody)
            .build()
        val request = Request.Builder()
            .url("$restBase/media")
            .header("Authorization", credential)
            .post(multipart)
            .build()
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException(parseWpError(responseBody, response.code))
            }
            val obj = JSONObject(responseBody)
            return obj.getInt("id")
        }
    }

    private fun parseWpError(body: String, code: Int): String {
        return try {
            val o = JSONObject(body)
            val msg = o.optString("message", body)
            "$msg (HTTP $code)"
        } catch (_: Exception) {
            if (body.isNotBlank()) "$body (HTTP $code)" else "HTTP $code"
        }
    }

    private fun htmlBodyFromPlain(body: String): String {
        val escaped = body
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        val withBreaks = escaped.replace("\n", "<br>\n")
        return "<!-- wp:paragraph --><p>$withBreaks</p><!-- /wp:paragraph -->"
    }

    private fun materializeImageForUpload(context: Context, imageUriString: String): File? {
        val uri = runCatching { Uri.parse(imageUriString) }.getOrNull() ?: return null
        val mime = context.contentResolver.getType(uri) ?: guessMimeFromUri(imageUriString)
        val ext = when {
            mime.contains("png", ignoreCase = true) -> "png"
            mime.contains("gif", ignoreCase = true) -> "gif"
            mime.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val out = File.createTempFile("wp_media_", ".$ext", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (out.exists() && out.length() > 0L) out else null
        } catch (_: Exception) {
            out.delete()
            null
        }
    }

    private fun guessMimeFromUri(uriString: String): String {
        return when {
            uriString.endsWith(".png", true) -> "image/png"
            uriString.endsWith(".gif", true) -> "image/gif"
            uriString.endsWith(".webp", true) -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private fun guessMime(filename: String): String {
        return when {
            filename.endsWith(".png", true) -> "image/png"
            filename.endsWith(".gif", true) -> "image/gif"
            filename.endsWith(".webp", true) -> "image/webp"
            else -> "image/jpeg"
        }
    }

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
}
