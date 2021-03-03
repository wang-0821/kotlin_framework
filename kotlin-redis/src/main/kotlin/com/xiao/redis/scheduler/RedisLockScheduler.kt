package com.xiao.redis.scheduler

import com.xiao.base.scheduler.BaseScheduler
import com.xiao.base.scheduler.SafeScheduledFuture
import com.xiao.redis.utils.RedisLock
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock

/**
 * [RedisLockScheduler] 是分布式[ScheduledExecutorService]
 * 在异步任务执行过程中，会一直锁定分布式锁。
 *
 * @author lix wang
 */
class RedisLockScheduler : BaseScheduler {
    private var taskCount: Int = 0
    private val redisLock: RedisLock
    private val taskMaxCount: Int
    private val lockRetryDuration: Duration
    private val waitLockTimeout: Duration?
    private val lock = ReentrantLock()
    private val isFullCondition = lock.newCondition()
    private val lockHolderFutures = mutableMapOf<ScheduledTask<*>, SafeScheduledFuture<*>>()
    private val taskFutures = mutableMapOf<ScheduledTask<*>, SafeScheduledFuture<*>>()

    @JvmOverloads
    constructor(
        name: String,
        redisLock: RedisLock,
        scheduledExecutorService: ScheduledExecutorService,
        redisRetryDuration: Duration = DEFAULT_LOCK_RETRY_DURATION,
        waitLockTimeout: Duration? = DEFAULT_TASK_TIMEOUT,
        taskMaxCount: Int = Int.MAX_VALUE
    ) : super(name, scheduledExecutorService) {
        this.redisLock = redisLock
        this.lockRetryDuration = redisRetryDuration
        this.waitLockTimeout = waitLockTimeout
        this.taskMaxCount = taskMaxCount
    }

    /**
     * [command] task will try lock first, once acquired lock, will hold lock till task finished.
     * [command] task will use [scheduleAtFixedRate] to continually refresh lock expire time.
     * [command] task will wait locked till timeout, will not skip task.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> schedule(delay: Duration, command: () -> T): SafeScheduledFuture<T> {
        return execWithLock {
            val task = ScheduledTask(command, waitLockTimeout, false)
            val future = super.schedule(delay) {
                task.call()
            }

            taskFutures[task] = future
            future.whenComplete { _, _ ->
                taskFutures.remove(task)
            }

            return@execWithLock future
        }
    }

    /**
     * [command] task will try lock with [scheduleAtFixedRate], if not locked, will skip task without waiting lock.
     */
    override fun scheduleAtFixedRate(
        initialDelay: Duration,
        period: Duration,
        command: () -> Unit
    ): SafeScheduledFuture<Unit> {
        return execWithLock {
            val task = ScheduledTask(command, waitLockTimeout, true)
            val future = super.scheduleAtFixedRate(initialDelay, period) {
                task.call()
            }

            taskFutures[task] = future
            future.whenComplete { _, _ ->
                taskFutures.remove(task)
            }

            return@execWithLock future
        }
    }

    /**
     * [command] task will skip task without waiting lock, if task can not acquire lock.
     */
    override fun scheduleWithFixedDelay(
        initialDelay: Duration,
        delay: Duration,
        command: () -> Unit
    ): SafeScheduledFuture<Unit> {
        return execWithLock {
            val task = ScheduledTask(command, waitLockTimeout, true)
            val future = super.scheduleWithFixedDelay(initialDelay, delay) {
                task.call()
            }

            taskFutures[task] = future
            future.whenComplete { _, _ ->
                taskFutures.remove(task)
            }

            return@execWithLock future
        }
    }

    override fun taskCount(): Int {
        return taskCount
    }

    override fun taskCapacity(): Int {
        return Int.MAX_VALUE
    }

    override fun shutdown() {
        cancelAllScheduledTask()
        super.shutdown()
    }

    override fun shutdownNow() {
        cancelAllScheduledTask()
        super.shutdownNow()
    }

    private fun cancelAllScheduledTask() {
        taskFutures.forEach {
            it.value.cancel(false)
        }
        lockHolderFutures.forEach {
            it.value.cancel(false)
        }
        taskFutures.clear()
        lockHolderFutures.clear()
    }

    private fun <T : Any?> execWithLock(block: () -> SafeScheduledFuture<T>): SafeScheduledFuture<T> {
        lock.lock()
        try {
            if (taskCount >= taskMaxCount) {
                isFullCondition.await()
            }
            val future = block()

            taskCount++
            return future
        } finally {
            lock.unlock()
        }
    }

    private fun tryLockAndHold(task: ScheduledTask<*>): SafeScheduledFuture<Unit> {
        var future: SafeScheduledFuture<Unit>? = null
        try {
            // try lock
            val lockExpireTime = lockRetryDuration.multipliedBy(2)
            task.locked = redisLock.tryLock(lockExpireTime)

            // constantly try lock
            future = super.scheduleAtFixedRate(lockRetryDuration, lockRetryDuration) {
                val taskLocked = task.locked
                task.locked = redisLock.tryLock(lockExpireTime)
                if (!taskLocked) {
                    task.thread?.let {
                        LockSupport.unpark(it)
                    }
                }
            }.apply {
                exceptionally {
                    this.cancel(false)
                    throw it
                }
            }

            lockHolderFutures[task] = future
            return future
        } catch (e: Exception) {
            lockHolderFutures.remove(task)
            future?.cancel(false)
            throw e
        }
    }

    private fun releaseLock(task: ScheduledTask<*>, lockHolderFuture: SafeScheduledFuture<Unit>?) {
        lockHolderFuture?.cancel(false)
        lockHolderFutures.remove(task)
        if (task.locked) {
            task.locked = false
            redisLock.unlock()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inner class ScheduledTask<T : Any?> : Callable<T> {
        private val task: () -> T
        private val timeoutNanos: Long
        private val allowSkip: Boolean
        var locked = false
        var thread: Thread? = null

        constructor(
            task: () -> T,
            timeoutDuration: Duration?,
            allowSkip: Boolean
        ) {
            this.task = task
            this.timeoutNanos = timeoutDuration?.toNanos() ?: 0
            this.allowSkip = allowSkip
        }

        override fun call(): T {
            val lockHolderFuture = tryLockAndHold(this)
            try {
                if (!locked) {
                    if (allowSkip) {
                        return null as T
                    } else {
                        thread = Thread.currentThread()
                        if (timeoutNanos > 0) {
                            LockSupport.parkNanos(timeoutNanos)
                        } else {
                            LockSupport.park()
                        }
                    }
                }

                return if (locked) {
                    return task()
                } else {
                    // timeout
                    null as T
                }
            } finally {
                releaseLock(this, lockHolderFuture)
            }
        }
    }

    companion object {
        private val DEFAULT_LOCK_RETRY_DURATION = Duration.ofSeconds(2)
        private val DEFAULT_TASK_TIMEOUT = Duration.ofMinutes(5)
    }
}