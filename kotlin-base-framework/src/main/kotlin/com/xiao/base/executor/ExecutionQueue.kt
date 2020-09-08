package com.xiao.base.executor

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.locks.ReentrantLock

/**
 *
 * @author lix wang
 */
class ExecutionQueue {
    private val executorService: ExecutorService
    private val executionQueueName: String
    private val lock = ReentrantLock()

    constructor(executionQueueName: String, executorService: ExecutorService) {
        this.executionQueueName = executionQueueName
        this.executorService = executorService
    }

    fun <T> submit(taskName: String, callable: Callable<T>): Future<T> {
        val name = if (taskName.isNullOrBlank()) {
            "Queue-Callable"
        } else {
            taskName
        }
        return submitTask(name) {
            FutureTask(callable)
        }
    }

    fun <T> submit(callable: Callable<T>): Future<T> {
        return submit("", callable)
    }

    fun submit(taskName: String, runnable: Runnable) {
        val name = if (taskName.isNullOrBlank()){
            "Queue-Runnable"
        } else {
            taskName
        }
        submitTask(name) {
            FutureTask<Void>(runnable, null)
        }
    }

    fun submit(runnable: Runnable) {
        submit("", runnable)
    }

    private fun <T> submitTask(taskName: String, futureTaskGenerator: () -> FutureTask<T>): Future<T> {
        lock.lock()
        try {
            val task = futureTaskGenerator()
            val queueItem = SimpleQueueItem(taskName, task)
            executorService.submit(queueItem)
            return task
        } catch (e: Exception) {
            throw IllegalStateException(
                "Submit task $taskName to $executionQueueName ExecutionQueue failed. ${e.message}")
        } finally {
            lock.unlock()
        }
    }
}