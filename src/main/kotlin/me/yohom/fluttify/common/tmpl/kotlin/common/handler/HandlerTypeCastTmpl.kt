package me.yohom.fluttify.common.tmpl.kotlin.common.handler

import me.yohom.fluttify.common.model.Method

// todo 先实现objc的
//val refId = args["refId"] as Int
//val ref = REF_MAP[refId] as #__class_name__#
internal class HandlerTypeCastTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/kotlin/handler_type_cast.stmt.kt.tmpl").readText()

    fun kotlinTypeCast(): String {
        return if (method.isStatic)
            ""
        else
            tmpl.replace("#__class_name__#", method.className)
    }
}