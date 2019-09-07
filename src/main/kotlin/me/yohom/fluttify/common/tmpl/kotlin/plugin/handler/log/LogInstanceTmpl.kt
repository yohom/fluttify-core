package me.yohom.fluttify.common.tmpl.kotlin.plugin.handler.log

import me.yohom.fluttify.common.model.Method

//val refId = args["refId"] as Int
//val ref = REF_MAP[refId] as #__class_name__#
internal class LogInstanceTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("val refId = args[\"refId\"] as Int\nval ref = REF_MAP[refId] as #__class_name__#").readText()

    fun kotlinTypeCast(): String {
        return if (method.isStatic)
            ""
        else
            tmpl.replace("#__class_name__#", method.className)
    }
}