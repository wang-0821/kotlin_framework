package com.xiao.metrics

import com.xiao.base.thread.ThreadUnsafe
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.ReferenceCountUtil

/**
 *
 * @author lix wang
 */
class MetricsLatencyBuf {
    var times: Long = 0
        private set
    private var writeableBytes = MAX_CAPACITY
    private var latencies: ByteBuf? = null

    @ThreadUnsafe
    fun writeable(): Boolean {
        return writeableBytes - LATENCY_BYTES >= 0
    }

    @ThreadUnsafe
    fun addLatency(runningTime: Int) {
        val currentWriteableBytes = writeableBytes - LATENCY_BYTES
        if (currentWriteableBytes >= 0) {
            writeableBytes = currentWriteableBytes
            if (latencies == null) {
                latencies = PooledByteBufAllocator.DEFAULT.buffer(MIN_CAPACITY, MAX_CAPACITY)
            }
            latencies!!.writeInt(runningTime)
            times++
        }
    }

    @ThreadUnsafe
    fun resetLatency(target: MutableList<Int>) {
        latencies?.let {
            try {
                while (it.isReadable(LATENCY_BYTES)) {
                    target.add(it.readInt())
                }
                it.clear()
            } catch (e: Exception) {
                ReferenceCountUtil.release(it)
                latencies = null
            }
        }
    }

    companion object {
        private const val MIN_CAPACITY = 256
        private const val MAX_CAPACITY = 8192
        private const val LATENCY_BYTES = 4
    }
}