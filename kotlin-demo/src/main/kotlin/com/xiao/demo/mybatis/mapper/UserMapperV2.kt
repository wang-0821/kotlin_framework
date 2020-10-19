package com.xiao.demo.mybatis.mapper

import com.xiao.demo.model.User
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

/**
 *
 * @author lix wang
 */
interface UserMapperV2 {
    @Select("SELECT * FROM users WHERE id = #{id}")
    fun getById(@Param("id") id: Long): User
}