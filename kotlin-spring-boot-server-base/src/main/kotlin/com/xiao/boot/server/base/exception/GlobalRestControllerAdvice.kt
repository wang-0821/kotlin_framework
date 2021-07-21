package com.xiao.boot.server.base.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 *
 * @author lix wang
 */
@RestControllerAdvice
class GlobalRestControllerAdvice {
    @ExceptionHandler(value = [Exception::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleException(exception: Exception) {
        throw KtWebException(exception)
    }
}