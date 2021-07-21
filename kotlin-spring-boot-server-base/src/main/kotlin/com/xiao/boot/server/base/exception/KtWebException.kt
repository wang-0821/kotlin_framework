package com.xiao.boot.server.base.exception

/**
 *
 * @author lix wang
 */
class KtWebException : Exception {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(exception: Exception) : super(exception.message, exception.cause)
    constructor(message: String, throwable: Throwable?) : super(message, throwable)
}