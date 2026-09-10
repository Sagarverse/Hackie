package com.example.rabit.data.voice

import android.content.Context
import android.util.Log
import com.example.rabit.domain.model.gemini.GeminiRequest
import com.example.rabit.domain.repository.GeminiRepository
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class JarvisAction(
    val action: String,
    val arguments: Map<String, String> = emptyMap()
)

class JarvisSmartEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)
    private val geminiRepo: GeminiRepository = GeminiRepositoryImpl()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _llmResponse = MutableStateFlow("")
    val llmResponse = _llmResponse.asStateFlow()

    private val _actionQueue = MutableStateFlow<List<JarvisAction>>(emptyList())
    val actionQueue = _actionQueue.asStateFlow()

    private val systemPrompt = """
        You are Jarvis, an AI assistant for the Hackie app. Your job is to translate the user's natural language command into a sequence of structured actions to execute on the connected device or app.
        
        Available actions:
        - {"action": "connect_device", "address": "XX:XX:XX:XX:XX:XX"}
        - {"action": "connect_recent"}
        - {"action": "disconnect"}
        - {"action": "lock_screen"}
        - {"action": "unlock_screen"}
        - {"action": "send_text", "text": "..."}
        - {"action": "key_combo", "combo": "GUI+C"}
        - {"action": "open_app", "name": "Safari"}
        - {"action": "media_play_pause"}
        - {"action": "volume_up"}
        - {"action": "volume_down"}
        - {"action": "mute"}
        - {"action": "screenshot"}
        - {"action": "type_text", "text": "..."}
        - {"action": "navigate", "screen": "settings"}
        - {"action": "start_advertising"}
        - {"action": "execute_macro", "name": "..."}
        - {"action": "speak", "text": "..."}
        - {"action": "wait", "ms": "1000"}
        
        Respond ONLY with a JSON array of actions. Example:
        [
            {"action": "connect_recent"},
            {"action": "wait", "ms": "2000"},
            {"action": "unlock_screen"},
            {"action": "speak", "text": "I have connected and unlocked your device."}
        ]
    """.trimIndent()

    var onExecuteActions: ((List<JarvisAction>) -> Unit)? = null

    fun processCommand(commandText: String, contextInfo: String = "") {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) {
            Log.e("JarvisSmartEngine", "No Gemini API key found")
            return
        }

        _isProcessing.value = true
        _llmResponse.value = ""
        
        val fullPrompt = "Context: $contextInfo\nCommand: $commandText"

        scope.launch {
            try {
                val request = GeminiRequest(
                    prompt = fullPrompt,
                    systemPrompt = systemPrompt,
                    temperature = 0.2f
                )
                
                val response = geminiRepo.sendPrompt(request, apiKey)
                _llmResponse.value = response.text
                
                if (response.error == null && response.text.isNotBlank()) {
                    val actions = parseJsonToActions(response.text)
                    _actionQueue.value = actions
                    onExecuteActions?.invoke(actions)
                } else {
                     Log.e("JarvisSmartEngine", "LLM Error: ${response.error?.message}")
                }
            } catch (e: Exception) {
                Log.e("JarvisSmartEngine", "Error processing command", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun parseJsonToActions(jsonString: String): List<JarvisAction> {
        val actions = mutableListOf<JarvisAction>()
        try {
            // Extract JSON array from text (handle potential markdown formatting)
            val jsonStart = jsonString.indexOf("[")
            val jsonEnd = jsonString.lastIndexOf("]") + 1
            if (jsonStart != -1 && jsonEnd > jsonStart) {
                val cleanJson = jsonString.substring(jsonStart, jsonEnd)
                val jsonArray = JSONArray(cleanJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val actionType = obj.getString("action")
                    val args = mutableMapOf<String, String>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "action") {
                            args[key] = obj.getString(key)
                        }
                    }
                    actions.add(JarvisAction(actionType, args))
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisSmartEngine", "Error parsing LLM response to JSON", e)
        }
        return actions
    }
}
