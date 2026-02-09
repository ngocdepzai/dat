package com.lws.type

import java.lang.RuntimeException


open class BaseException constructor(
    message: String,
    val error: Int,
    cause: Throwable? = null
) : RuntimeException (message, cause)