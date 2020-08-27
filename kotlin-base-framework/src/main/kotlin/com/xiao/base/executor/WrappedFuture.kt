package com.xiao.base.executor

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 *
 * @author lix wang
 */
class WrappedFuture<T>(private val future: Future<T>) : Future<T> {
    var submitTime: Long = -1
    var timeTrace: ExecuteTimeTrace? = null
    var retryTimeTraces: List<ExecuteTimeTrace>? = null

    override fun isDone(): Boolean {
        return future.isDone
    }

    override fun get(): T {
        return future.get()
    }

    override fun get(timeout: Long, unit: TimeUnit): T {
        return future.get(timeout, unit)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return future.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return future.isCancelled
    }
}