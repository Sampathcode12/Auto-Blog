package com.example.autoblog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.autoblog.model.MessageItem

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val createdAtMs: Long,
    val imageUriString: String?
)

fun MessageEntity.toDomain(): MessageItem =
    MessageItem(id, title, body, createdAtMs, imageUriString)

fun MessageItem.toEntity(): MessageEntity =
    MessageEntity(id, title, body, createdAtMs, imageUriString)
