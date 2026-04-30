package com.example.rabit.ui.mission

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MissionStatus(
    val subjectId: String = "",
    val c2Status: String = "INACTIVE",
    val phishArmed: Boolean = false,
    val activeExploits: Int = 0,
    val capturedLootCount: Int = 0
)

class MissionCommandViewModel : ViewModel() {
    private val _missionStatus = MutableStateFlow(MissionStatus())
    val missionStatus = _missionStatus.asStateFlow()

    fun updateSubjectId(id: String) {
        _missionStatus.value = _missionStatus.value.copy(subjectId = id)
    }

    fun setC2Status(status: String) {
        _missionStatus.value = _missionStatus.value.copy(c2Status = status)
    }

    fun setPhishArmed(armed: Boolean) {
        _missionStatus.value = _missionStatus.value.copy(phishArmed = armed)
    }
    
    fun refreshStats() {
        // Simulated stats update
        _missionStatus.value = _missionStatus.value.copy(
            activeExploits = (1..5).random(),
            capturedLootCount = (10..50).random()
        )
    }
}
