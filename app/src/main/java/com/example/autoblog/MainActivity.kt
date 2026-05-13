package com.example.autoblog

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.autoblog.R
import com.example.autoblog.data.MessagesRepository
import com.example.autoblog.model.MessageItem
import com.example.autoblog.ui.screens.CreateMessageScreen
import com.example.autoblog.ui.screens.DashboardScreen
import com.example.autoblog.ui.screens.LoginScreen
import com.example.autoblog.ui.screens.MessageDetailScreen
import com.example.autoblog.ui.screens.MessageEditorScreen
import com.example.autoblog.ui.screens.PublishScreen
import com.example.autoblog.ui.screens.ShareMessageScreen
import com.example.autoblog.ui.theme.AutoBlogTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed class LoggedInRoute {
    data object MessageList : LoggedInRoute()
    data object Create : LoggedInRoute()
    data class Detail(val id: String) : LoggedInRoute()
    data class Edit(val id: String) : LoggedInRoute()
    data class Share(
        val ids: List<String>,
        val returnToDetailId: String? = null,
        val returnToPublish: Boolean = false,
        val publishReturnDetailId: String? = null
    ) : LoggedInRoute()
    data class Publish(
        val ids: List<String>,
        val returnToDetailId: String? = null
    ) : LoggedInRoute()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AutoBlogTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val context = LocalContext.current
                    val repository = remember {
                        MessagesRepository(context.applicationContext)
                    }
                    var messages by remember { mutableStateOf<List<MessageItem>>(emptyList()) }
                    val scope = rememberCoroutineScope()

                    LaunchedEffect(repository) {
                        repository.ensureSeededIfEmpty()
                        repository.messagesFlow().collect { list ->
                            withContext(Dispatchers.Main) {
                                messages = list
                            }
                        }
                    }

                    AppNavigator(
                        innerPadding = innerPadding,
                        messages = messages,
                        repository = repository,
                        scope = scope
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigator(
    innerPadding: PaddingValues,
    messages: List<MessageItem>,
    repository: MessagesRepository,
    scope: CoroutineScope
) {
    var isLoggedIn by remember { mutableStateOf(false) }
    var route by remember { mutableStateOf<LoggedInRoute>(LoggedInRoute.MessageList) }

    if (!isLoggedIn) {
        LoginScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onLoginSuccess = { isLoggedIn = true }
        )
        return
    }

    fun messageById(id: String): MessageItem? = messages.firstOrNull { it.id == id }

    when (val r = route) {
        is LoggedInRoute.Share -> {
            val items = r.ids.mapNotNull { messageById(it) }
            if (items.isEmpty()) {
                LaunchedEffect(r.ids) {
                    route = LoggedInRoute.MessageList
                }
                Box(Modifier.fillMaxSize())
            } else {
                ShareMessageScreen(
                    modifier = Modifier.fillMaxSize(),
                    messages = items,
                    onBack = {
                        route = when {
                            r.returnToPublish ->
                                LoggedInRoute.Publish(r.ids, r.publishReturnDetailId)
                            r.returnToDetailId != null ->
                                LoggedInRoute.Detail(r.returnToDetailId)
                            else -> LoggedInRoute.MessageList
                        }
                    }
                )
            }
        }

        is LoggedInRoute.Publish -> {
            val items = r.ids.mapNotNull { messageById(it) }
            if (items.isEmpty()) {
                LaunchedEffect(r.ids) {
                    route = LoggedInRoute.MessageList
                }
                Box(Modifier.fillMaxSize())
            } else {
                PublishScreen(
                    modifier = Modifier.fillMaxSize(),
                    messages = items,
                    onBack = {
                        route = r.returnToDetailId?.let { LoggedInRoute.Detail(it) }
                            ?: LoggedInRoute.MessageList
                    },
                    onOpenShareForOtherPlatforms = {
                        route = LoggedInRoute.Share(
                            ids = r.ids,
                            returnToDetailId = null,
                            returnToPublish = true,
                            publishReturnDetailId = r.returnToDetailId
                        )
                    }
                )
            }
        }

        is LoggedInRoute.Edit -> {
            val msg = messageById(r.id)
            if (msg == null) {
                LaunchedEffect(r.id) {
                    route = LoggedInRoute.MessageList
                }
                Box(Modifier.fillMaxSize())
            } else {
                MessageEditorScreen(
                    modifier = Modifier.fillMaxSize(),
                    headerTitleRes = R.string.edit_msg_title,
                    primaryButtonRes = R.string.edit_msg_send_update,
                    sentToastRes = R.string.edit_msg_sent,
                    initialTitle = msg.title,
                    initialBody = msg.body,
                    initialImageUri = msg.imageUriString?.let(Uri::parse),
                    onBack = { route = LoggedInRoute.Detail(r.id) },
                    onSend = { title, body, imageUri ->
                        scope.launch(Dispatchers.IO) {
                            repository.updateMessage(r.id, title, body, imageUri, msg)
                            withContext(Dispatchers.Main) {
                                route = LoggedInRoute.Detail(r.id)
                            }
                        }
                    }
                )
            }
        }

        is LoggedInRoute.Detail -> {
            val msg = messageById(r.id)
            if (msg == null) {
                LaunchedEffect(r.id) {
                    route = LoggedInRoute.MessageList
                }
                Box(Modifier.fillMaxSize())
            } else {
                MessageDetailScreen(
                    modifier = Modifier.fillMaxSize(),
                    message = msg,
                    onBack = { route = LoggedInRoute.MessageList },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            repository.deleteMessage(r.id)
                            withContext(Dispatchers.Main) {
                                route = LoggedInRoute.MessageList
                            }
                        }
                    },
                    onEdit = { route = LoggedInRoute.Edit(r.id) },
                    onShare = { route = LoggedInRoute.Share(listOf(r.id), r.id) },
                    onPublish = {
                        route = LoggedInRoute.Publish(listOf(r.id), returnToDetailId = r.id)
                    },
                    onImageAttached = { uri ->
                        scope.launch(Dispatchers.IO) {
                            repository.attachImage(r.id, uri)
                        }
                    }
                )
            }
        }

        is LoggedInRoute.Create -> {
            CreateMessageScreen(
                modifier = Modifier.fillMaxSize(),
                onBack = { route = LoggedInRoute.MessageList },
                onSend = { title, body, imageUri ->
                    scope.launch(Dispatchers.IO) {
                        repository.insertMessage(title, body, imageUri)
                        withContext(Dispatchers.Main) {
                            route = LoggedInRoute.MessageList
                        }
                    }
                }
            )
        }

        is LoggedInRoute.MessageList -> {
            DashboardScreen(
                modifier = Modifier.fillMaxSize(),
                messages = messages,
                onOpenCreate = { route = LoggedInRoute.Create },
                onMessageClick = { route = LoggedInRoute.Detail(it.id) },
                onEditMessage = { route = LoggedInRoute.Edit(it.id) },
                onDeleteMessage = { id ->
                    scope.launch(Dispatchers.IO) {
                        repository.deleteMessage(id)
                    }
                },
                onBulkDelete = { ids ->
                    scope.launch(Dispatchers.IO) {
                        repository.deleteMessages(ids.toList())
                    }
                },
                onBulkShare = { items ->
                    route = LoggedInRoute.Share(items.map { it.id }, returnToDetailId = null)
                },
                onBulkPublish = { items ->
                    route = LoggedInRoute.Publish(items.map { it.id }, returnToDetailId = null)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    AutoBlogTheme {
        LoginScreen(
            modifier = Modifier.fillMaxSize(),
            onLoginSuccess = {}
        )
    }
}
