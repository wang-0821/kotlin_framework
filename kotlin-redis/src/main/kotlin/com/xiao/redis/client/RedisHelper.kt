package com.xiao.redis.client

import com.xiao.base.util.NettyUtils
import com.xiao.redis.client.proxy.RedisAsyncServiceProxy
import com.xiao.redis.client.proxy.RedisServiceProxy
import com.xiao.redis.client.service.RedisAsyncService
import com.xiao.redis.client.service.RedisService
import com.xiao.redis.client.testing.TestingRedisAsyncServiceProxy
import com.xiao.redis.client.testing.TestingRedisServiceProxy
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.resource.DefaultClientResources
import io.netty.channel.EventLoopGroup
import java.lang.reflect.Proxy

/**
 *
 * @author lix wang
 */
object RedisHelper {
    @JvmStatic
    fun getRedisService(url: String): RedisService {
        return Proxy.newProxyInstance(
            RedisService::class.java.classLoader,
            arrayOf(RedisService::class.java),
            RedisServiceProxy(RedisClient.create(clientResources, RedisURI.create(url)))
        ) as RedisService
    }

    @JvmStatic
    fun getRedisAsyncService(url: String): RedisAsyncService {
        return Proxy.newProxyInstance(
            RedisAsyncService::class.java.classLoader,
            arrayOf(RedisAsyncService::class.java),
            RedisAsyncServiceProxy(RedisClient.create(clientResources, RedisURI.create(url)))
        ) as RedisAsyncService
    }

    @JvmStatic
    fun getTestingRedisService(): RedisService {
        return Proxy.newProxyInstance(
            RedisService::class.java.classLoader,
            arrayOf(RedisService::class.java),
            TestingRedisServiceProxy()
        ) as RedisService
    }

    @JvmStatic
    fun getTestingRedisAsyncService(): RedisAsyncService {
        return Proxy.newProxyInstance(
            RedisAsyncService::class.java.classLoader,
            arrayOf(RedisAsyncService::class.java),
            TestingRedisAsyncServiceProxy()
        ) as RedisAsyncService
    }

    private val ioThreads = Runtime.getRuntime().availableProcessors()
    private val ioEventLoopGroup: EventLoopGroup = NettyUtils.getIoEventLoopGroup(ioThreads)
    // use shared io eventLoopGroup
    private val clientResources = DefaultClientResources
        .builder()
        .eventExecutorGroup(ioEventLoopGroup)
        .build()
}