package com.xiao.base.executor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 *
 * @author lix wang
 */
interface SafeDeferred<T : Any?> {
    suspend fun await(): T

    suspend fun awaitNanos(timeout: Long = 60000, timeUnit: TimeUnit = TimeUnit.MILLISECONDS): T

    fun cancel(cause: CancellationException? = null)

    @ExperimentalCoroutinesApi
    fun getCompleted(): T

    val isCompleted: Boolean
}