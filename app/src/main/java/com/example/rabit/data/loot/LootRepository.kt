package com.example.rabit.data.loot

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LootMessage(
    val sender: String,
    val body: String,
    val timestamp: Long,
    val type: String = "SMS" // SMS, WhatsApp, Telegram
)

@Serializable
data class LootContact(
    val name: String,
    val number: String,
    val email: String? = null
)

@Serializable
data class LootCall(
    val number: String,
    val duration: String,
    val type: String, // Incoming, Outgoing, Missed
    val timestamp: Long
)

object LootRepository {
    private val _messages = MutableStateFlow<List<LootMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _contacts = MutableStateFlow<List<LootContact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _calls = MutableStateFlow<List<LootCall>>(emptyList())
    val calls = _calls.asStateFlow()

    fun addMessage(message: LootMessage) {
        _messages.value = listOf(message) + _messages.value
    }

    fun addContact(contact: LootContact) {
        _contacts.value = listOf(contact) + _contacts.value
    }

    fun addCall(call: LootCall) {
        _calls.value = listOf(call) + _calls.value
    }

    fun clearAll() {
        _messages.value = emptyList()
        _contacts.value = emptyList()
        _calls.value = emptyList()
    }
}
