package com.niksoriginals.melo.listentogether

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ListenTogetherServer(
    val name: String,
    val url: String,
    val location: String,
    val operator: String
)

object ListenTogetherServers {
    private const val SERVER_JSON_URL = "https://raw.githubusercontent.com/niksoriginals/Melo/refs/heads/main/app/server.json"

    private val _servers = MutableStateFlow(
        listOf(
            ListenTogetherServer(
                name = "Melo Server",
                url = "wss://iad1tya-echomusic.hf.space/ws",
                location = "Global - Melo",
                operator = "Melo"
            )
        )
    )
    
    val serversFlow: StateFlow<List<ListenTogetherServer>> = _servers

    val servers: List<ListenTogetherServer>
        get() = _servers.value

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(SERVER_JSON_URL).build()
                val response = client.newCall(request).execute()
                response.body.string().let { jsonString ->
                    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
                    val name = jsonObject["name"]?.jsonPrimitive?.content ?: "Melo Server"
                    val url = jsonObject["serverUrl"]?.jsonPrimitive?.content ?: "wss://iad1tya-echomusic.hf.space/ws"
                    val region = jsonObject["region"]?.jsonPrimitive?.content ?: "Global - Melo"
                    
                    if (url.startsWith("wss://") || url.startsWith("ws://")) {
                        _servers.value = listOf(
                            ListenTogetherServer(
                                name = name,
                                url = url,
                                location = region,
                                operator = "Melo"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Fallback implicitly retained
            }
        }
    }

    val defaultServerUrl: String
        get() = servers.first().url

    fun findByUrl(url: String): ListenTogetherServer? = servers.firstOrNull { it.url == url }
}
