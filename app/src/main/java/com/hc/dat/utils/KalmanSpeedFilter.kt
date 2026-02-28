package com.hc.dat.utils

class KalmanSpeedFilter(
        private var processNoise: Float = 1f,     // Q
        private var measurementNoise: Float = 3f  // R
) {

    private var estimatedSpeed = 0f
    private var estimationError = 1f

    fun reset(initialSpeed: Float = 0f) {
        estimatedSpeed = initialSpeed
        estimationError = 1f
    }

    fun update(measuredSpeed: Float): Float {

        // Prediction step
        estimationError += processNoise

        // Kalman gain
        val kalmanGain = estimationError / (estimationError + measurementNoise)

        // Update estimate
        estimatedSpeed += kalmanGain * (measuredSpeed - estimatedSpeed)

        // Update error covariance
        estimationError *= (1 - kalmanGain)

        return estimatedSpeed
    }
}