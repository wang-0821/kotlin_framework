package com.xiao.rpc

import com.xiao.base.context.ContextScanner
import com.xiao.rpc.factory.SslSocketFactorySelector
import com.xiao.rpc.handler.Chain
import com.xiao.rpc.io.Request
import com.xiao.rpc.io.Response
import com.xiao.rpc.tool.UrlParser
import javax.net.ssl.SSLContext

/**
 *
 * @author lix wang
 */
class Client {
    var connectTimeout = 5000
    private set
    var readTimeout = 5000
    private set
    var writeTimeout = 5000
    private set

    init {
        refreshContext()
    }

    class Call(private val client: Client, private val request: Request) {
        fun execute(): Response {
             return Chain(client, request).execute()
        }
    }

    fun newCall(request: Request): Call {
        return Call(this, request)
    }

    fun connectTimeout(mills: Int): Client {
        this.connectTimeout = mills
        return this
    }

    fun readTimeout(mills: Int): Client {
        this.readTimeout = mills
        return this
    }

    fun writeTimeout(mills: Int): Client {
        this.writeTimeout = mills
        return this
    }

    private fun refreshContext() {
        ContextScanner.scanAndExecute(getPackageName(this::class.java.name))
    }

    companion object  {
        private fun getPackageName(fqClassName: String): String {
            val lastDotIndex: Int = fqClassName.lastIndexOf(".")
            return if (lastDotIndex != -1) fqClassName.substring(0, lastDotIndex) else ""
        }
    }
}

fun main() {
    val response = Client().newCall(UrlParser.parseUrl("https://www.baidu.com")).execute()
    println("Response*********** ${response.contentAsString()} *******")
}