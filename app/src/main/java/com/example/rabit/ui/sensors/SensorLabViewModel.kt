package com.example.rabit.ui.sensors

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rabit.data.gemini.GeminiRepositoryImpl
import com.example.rabit.domain.model.gemini.GeminiRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SensorReading(val x: Float, val y: Float, val z: Float)

class SensorLabViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _accelReading = MutableStateFlow(SensorReading(0f, 0f, 0f))
    val accelReading = _accelReading.asStateFlow()

    private val _magReading = MutableStateFlow(SensorReading(0f, 0f, 0f))
    val magReading = _magReading.asStateFlow()

    private val _aiInterpretation = MutableStateFlow("Awaiting sensor data stabilization...")
    val aiInterpretation = _aiInterpretation.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val geminiRepo = GeminiRepositoryImpl()
    private val prefs = application.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    private val accelDataBuffer = mutableListOf<SensorReading>()
    private val magDataBuffer = mutableListOf<SensorReading>()

    fun startSurveillance() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_UI)
        
        runAiAnalysisLoop()
    }

    fun stopSurveillance() {
        sensorManager.unregisterListener(this)
        _isAnalyzing.value = false
    }

    private fun runAiAnalysisLoop() {
        _isAnalyzing.value = true
        viewModelScope.launch {
            while (_isAnalyzing.value) {
                delay(10000) // Analyze every 10 seconds
                performNeuralInterpretation()
            }
        }
    }

    private suspend fun performNeuralInterpretation() {
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isBlank()) return

        val accelSnap = accelDataBuffer.takeLast(50).joinToString { "(${it.x},${it.y},${it.z})" }
        val magSnap = magDataBuffer.takeLast(50).joinToString { "(${it.x},${it.y},${it.z})" }

        val prompt = """
            You are a Side-Channel Signal Intelligence (SIGINT) Expert.
            Analyze the following raw sensor data from a mobile device placed near a target system.
            
            Accelerometer Data (Vibrations):
            $accelSnap
            
            Magnetometer Data (EMF):
            $magSnap
            
            Objective: Identify if there is a mechanical keyboard being typed on, or if a high-power electronic device (like a monitor or server) is nearby based on EMF spikes.
            Provide a tactical summary of the environment.
        """.trimIndent()

        try {
            val response = geminiRepo.sendPrompt(GeminiRequest(prompt), apiKey)
            _aiInterpretation.value = response.text
        } catch (e: Exception) {
            _aiInterpretation.value = "Neural Link Error: ${e.message}"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val reading = SensorReading(event.values[0], event.values[1], event.values[2])
                _accelReading.value = reading
                accelDataBuffer.add(reading)
                if (accelDataBuffer.size > 200) accelDataBuffer.removeAt(0)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                val reading = SensorReading(event.values[0], event.values[1], event.values[2])
                _magReading.value = reading
                magDataBuffer.add(reading)
                if (magDataBuffer.size > 200) magDataBuffer.removeAt(0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
