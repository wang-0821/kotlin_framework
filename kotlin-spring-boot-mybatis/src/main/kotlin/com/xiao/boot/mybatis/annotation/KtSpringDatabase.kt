package com.xiao.boot.mybatis.annotation

import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

/**
 *
 * @author lix wang
 */
@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Import(KtSpringDatabaseRegistrar::class)
annotation class KtSpringDatabase(
    val name: String,
    val mapperBasePackage: String = "",
    val mapperXmlPattern: String = "",
    val dataScriptPattern: String = ""
)