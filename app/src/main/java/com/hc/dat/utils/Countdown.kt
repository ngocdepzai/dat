package com.hc.dat.utils

import kotlinx.coroutines.*

object Countdown {
    private var currentCountDownTime = 0L
    private var startTime = 0L
    private var endTime = 0L
    private var timeUnit = TimeUnit.MILLIS_SECOND
    lateinit var onTimeUpCallback: OnTimeUpCallback
    private var job: Job? = null
    private var inProgress = false

    fun restartCountdown() {
        currentCountDownTime = startTime
    }

    fun getInactiveTimes(): Long = startTime - currentCountDownTime

    fun startCountDown(start: Long, end: Long = 0, unit: TimeUnit, callback: OnTimeUpCallback) {
        startTime = start
        currentCountDownTime = startTime
        endTime = end
        timeUnit = unit
        onTimeUpCallback = callback
        if (!inProgress) {
            inProgress = true
            job = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
//                    Logger.i("countdown currentCountDownTime= $currentCountDownTime")
                    if (currentCountDownTime <= endTime) {
                        CoroutineScope(Dispatchers.Main).launch {
                            onTimeUpCallback.onTimeUp()
                        }
                        endCountDown()
                    } else {
                        currentCountDownTime--
                        delay(timeUnit.unitPerMillisSecond)
                    }
                }
            }
        }
    }

    fun endCountDown() {
        job?.cancel()
        inProgress = false
    }
}

enum class TimeUnit(val unitPerMillisSecond: Long) {
    MILLIS_SECOND(0),
    SECOND(1000L),
    MINUTES(1000L * 60L),
    HOUR(1000L * 60L * 60L),
    DAY(1000L * 60L * 60L * 24L)
}
