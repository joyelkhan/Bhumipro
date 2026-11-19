package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.BengaliFormatters
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalCompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var azimuthDeg by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sm?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sm == null || accel == null || magnet == null) {
            sensorAvailable = false
            onDispose { }
        } else {
            val gravity = FloatArray(3)
            val geomag = FloatArray(3)
            val rMat = FloatArray(9)
            val iMat = FloatArray(9)
            val orient = FloatArray(3)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, 3)
                        Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomag, 0, 3)
                    }
                    if (SensorManager.getRotationMatrix(rMat, iMat, gravity, geomag)) {
                        SensorManager.getOrientation(rMat, orient)
                        val deg = ((Math.toDegrees(orient[0].toDouble()) + 360.0) % 360.0).toFloat()
                        azimuthDeg = deg
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magnet, SensorManager.SENSOR_DELAY_UI)
            onDispose { sm.unregisterListener(listener) }
        }
    }

    val cardinalBn = when (azimuthDeg.roundToInt()) {
        in 338..360, in 0..22 -> "উত্তর (উ)"
        in 23..67 -> "উত্তর-পূর্ব"
        in 68..112 -> "পূর্ব (পূ)"
        in 113..157 -> "দক্ষিণ-পূর্ব"
        in 158..202 -> "দক্ষিণ (দ)"
        in 203..247 -> "দক্ষিণ-পশ্চিম"
        in 248..292 -> "পশ্চিম (প)"
        else -> "উত্তর-পশ্চিম"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                title = { Text("ডিজিটাল কম্পাস") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${BengaliFormatters.toBengaliDigits(azimuthDeg.roundToInt())}° • $cardinalBn",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Canvas(modifier = Modifier.size(220.dp)) {
                val radius = size.minDimension * 0.44f
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(color = Color(0xFF0D4A38), radius = radius, center = center, style = Stroke(width = 6f))
                rotate(-azimuthDeg, pivot = center) {
                    drawLine(
                        color = Color(0xFFDC2626),
                        start = center,
                        end = Offset(center.x, center.y - radius * 0.8f),
                        strokeWidth = 10f
                    )
                    drawLine(
                        color = Color(0xFF64748B),
                        start = center,
                        end = Offset(center.x, center.y + radius * 0.65f),
                        strokeWidth = 8f
                    )
                }
            }
            if (!sensorAvailable) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "এই ডিভাইসে ম্যাগনেটোমিটার সেন্সর পাওয়া যায়নি।",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
