package com.xiao.base.executor

import com.xiao.base.logging.Logging
import org.apache.logging.log4j.ThreadContext
import java.util.UUID
import java.util.concurrent.Callable

/**
 *
 * @author lix wang
 */
abstract class QueueItem<T>(val name: String) : Callable<T> {
    private val maxRetryTimes = 3
    private var retryTimes = 0

    fun execute(): T {
        val result: T
        ThreadContext.put("RpcRequestId", UUID.randomUUID().toString())
        val startTime = System.currentTimeMillis()
        result = try {
            call()
        } catch (e: Exception) {
            log.error("Task-$name execute failed. ${e.message}", e)
            retry()
        }
        log.info("Task-$name succeed, retried $retryTimes times, consume ${System.currentTimeMillis() - startTime} ms.")
        ThreadContext.clearMap()
        return result
    }

    private fun retry(): T {
        val startTime = System.currentTimeMillis()
        for (i in 1..maxRetryTimes) {
            retryTimes++
            try {
                return call()
            } catch (e: Exception) {
                log.error("Task-$name retry-$retryTimes failed. ${e.message}", e)
            }
        }
        log.error("Task-$name failed, retried $retryTimes times, consume ${System.currentTimeMillis() - startTime} ms.")
        throw RuntimeException("Task-$name failed.")
    }

    companion object : Logging()
}