package com.hc.dat.service.model

data class Result<T>(
    val isError: Boolean = false,
    val errorCode: Int = -1,
    val data: T? = null
)
