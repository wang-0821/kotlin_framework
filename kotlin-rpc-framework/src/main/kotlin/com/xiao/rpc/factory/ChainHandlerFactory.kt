package com.xiao.rpc.factory

import com.xiao.base.context.BeanRegistryAware
import com.xiao.rpc.handler.Chain
import com.xiao.rpc.handler.ConnectionHandler
import com.xiao.rpc.handler.ExchangeHandler
import com.xiao.rpc.handler.Handler
import com.xiao.rpc.handler.RouteHandler

/**
 *
 * @author lix wang
 */
interface ChainHandlerFactory {
    fun create(chain: Chain): List<Handler>
}

object DefaultChainHandlerFactory : ChainHandlerFactory {
    override fun create(chain: Chain): List<Handler> {
        return listOf(RouteHandler(chain), ConnectionHandler(chain), ExchangeHandler(chain))
    }
}

object ChainHandlerFactorySelector : BeanRegistryAware {
    fun select(): ChainHandlerFactory {
        return getByType(ChainHandlerFactory::class.java) ?: DefaultChainHandlerFactory
    }
}