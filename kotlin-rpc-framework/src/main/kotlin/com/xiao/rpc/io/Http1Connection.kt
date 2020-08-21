package com.xiao.rpc.io

import com.xiao.rpc.Route
import com.xiao.rpc.RunningState
import com.xiao.rpc.StateSocket
import com.xiao.rpc.factory.SslSocketFactorySelector
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

/**
 *
 * @author lix wang
 */
class Http1Connection(private val route: Route, private val socket: StateSocket) : AbstractConnection() {
    private val state = RunningState()

    private var realSocket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun connect() {
        if (route.address.isTls) {
            this.realSocket = connectTls(socket, route)
        } else {
            this.realSocket = socket
        }
        this.inputStream = realSocket?.getInputStream()
        this.outputStream = realSocket?.getOutputStream()
    }

    override fun validateAndUse(): Boolean {
        synchronized(state) {
            return state.validateAndUse()
        }
    }

    override fun route(): Route {
        return route
    }

    override fun write(message: ByteArray) {
        outputStream?.write(message)
    }


    override fun finishRequest() {
        outputStream?.flush()
    }

    override fun response(exchange: Exchange): Response {
        return parseToResponse(inputStream!!)
    }

    override fun close() {
        this.inputStream?.close()
        this.outputStream?.close()
    }
}