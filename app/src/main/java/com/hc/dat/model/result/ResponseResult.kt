package com.hc.dat.model.result

data class ResponseResult<T>(
    val isError: Boolean = false,
    val errorCode: Int = -1,
    val errorMessage: String = "",
    val data: T? = null
)
