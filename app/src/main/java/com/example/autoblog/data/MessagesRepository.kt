package com.example.autoblog.data

import android.content.Context
import android.net.Uri
import com.example.autoblog.data.local.AppDatabase
import com.example.autoblog.data.local.toDomain
import com.example.autoblog.data.local.toEntity
import com.example.autoblog.model.MessageItem
import com.example.autoblog.model.defaultMessages
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MessagesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(appContext).messageDao()

    fun messagesFlow(): Flow<List<MessageItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun ensureSeededIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            dao.insertAll(defaultMessages().map { it.toEntity() })
        }
    }

    suspend fun insertMessage(title: String, body: String, imageUri: Uri?) = withContext(Dispatchers.IO) {
        val imageStr = normalizePersistedImage(imageUri, previousStored = null)
        val item = MessageItem(
            id = UUID.randomUUID().toString(),
            title = title,
            body = body,
            createdAtMs = System.currentTimeMillis(),
            imageUriString = imageStr
        )
        dao.insert(item.toEntity())
    }

    suspend fun updateMessage(
        id: String,
        title: String,
        body: String,
        imageUri: Uri?,
        previous: MessageItem
    ) = withContext(Dispatchers.IO) {
        val imageStr = if (imageUri == null) {
            previous.imageUriString
        } else {
            normalizePersistedImage(imageUri, previous.imageUriString)
        }
        val updated = MessageItem(
            id = id,
            title = title,
            body = body,
            createdAtMs = previous.createdAtMs,
            imageUriString = imageStr
        )
        dao.insert(updated.toEntity())
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id)
        entity?.imageUriString?.let { deleteFileIfOurs(it) }
        dao.deleteById(id)
    }

    suspend fun deleteMessages(ids: List<String>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            dao.getById(id)?.imageUriString?.let { deleteFileIfOurs(it) }
        }
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    suspend fun attachImage(id: String, uri: Uri?) = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: return@withContext
        val newStr = normalizePersistedImage(uri, entity.imageUriString)
        if (entity.imageUriString != null &&
            entity.imageUriString != newStr
        ) {
            deleteFileIfOurs(entity.imageUriString)
        }
        dao.insert(entity.copy(imageUriString = newStr))
    }

    private fun normalizePersistedImage(uri: Uri?, previousStored: String?): String? {
        if (uri == null) return null
        if (uri.toString() == previousStored) return previousStored
        val filesPath = appContext.filesDir.absolutePath
        val path = uri.path
        if (uri.scheme == "file" && path != null && path.startsWith(filesPath)) {
            return uri.toString()
        }
        return copyImageToInternal(uri) ?: uri.toString()
    }

    private fun copyImageToInternal(source: Uri): String? {
        val dir = File(appContext.filesDir, "message_images").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        return try {
            appContext.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(dest).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun deleteFileIfOurs(uriString: String) {
        runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme != "file") return@runCatching
            val p = uri.path ?: return@runCatching
            val f = File(p)
            if (f.absolutePath.startsWith(appContext.filesDir.absolutePath)) {
                f.delete()
            }
        }
    }
}
