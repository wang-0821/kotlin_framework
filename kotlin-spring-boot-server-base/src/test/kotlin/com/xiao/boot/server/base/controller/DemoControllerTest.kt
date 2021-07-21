package com.xiao.boot.server.base.controller

import com.xiao.boot.base.env.EnvInfoProvider
import com.xiao.boot.base.testing.KtSpringTestBase
import com.xiao.boot.server.base.ServerBaseAutoConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate

/**
 * @author lix wang
 */
@SpringBootTest(
    classes = [ServerBaseAutoConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class DemoControllerTest : KtSpringTestBase() {
    @Autowired
    lateinit var envInfoProvider: EnvInfoProvider

    @Test
    fun `test request hello world`() {
        val result = RestTemplate().exchange(
            "http://localhost:${envInfoProvider.port()}/api/v1/demo/helloWorld",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            String::class.java,
            mapOf<String, String>()
        )
        Assertions.assertEquals(result.body, "hello world")
    }

    @Test
    fun `test request post method`() {
        val result = RestTemplate().exchange(
            "http://localhost:${envInfoProvider.port()}/api/v1/demo/printInput",
            HttpMethod.POST,
            HttpEntity("hello world"),
            String::class.java,
            mapOf<String, String>()
        )
        Assertions.assertEquals(result.body, "hello world")
    }

    @Test
    fun `test throw exception`() {
        val result = RestTemplate().exchange(
            "http://localhost:${envInfoProvider.port()}/api/v1/demo/throwException",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            Unit::class.java,
            mapOf<String, String>()
        ).body
    }
}