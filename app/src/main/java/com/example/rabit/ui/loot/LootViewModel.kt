package com.example.rabit.ui.loot

import androidx.lifecycle.ViewModel
import com.example.rabit.data.loot.LootRepository
import kotlinx.coroutines.flow.StateFlow

class LootViewModel : ViewModel() {
    val messages = LootRepository.messages
    val contacts = LootRepository.contacts
    val calls = LootRepository.calls

    fun clearLoot() {
        LootRepository.clearAll()
    }
    
    // Simulation / Demo data generator
    fun generateDemoLoot() {
        LootRepository.addMessage(com.example.rabit.data.loot.LootMessage("Bank Info", "Your OTP for transaction is 4421. Do not share.", System.currentTimeMillis() - 100000))
        LootRepository.addMessage(com.example.rabit.data.loot.LootMessage("+1 442 5521", "The package is ready for pickup at the warehouse.", System.currentTimeMillis() - 500000))
        LootRepository.addContact(com.example.rabit.data.loot.LootContact("John Doe", "+1 555 0199"))
        LootRepository.addContact(com.example.rabit.data.loot.LootContact("Jane Smith", "+1 555 0102"))
        LootRepository.addCall(com.example.rabit.data.loot.LootCall("+1 555 0199", "2m 14s", "Incoming", System.currentTimeMillis() - 800000))
    }
}
