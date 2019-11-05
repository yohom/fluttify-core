package me.yohom.fluttify.tmpl.java.common.handler.common.ref

import me.yohom.fluttify.model.Method

//val refId = args["refId"] as Int
//val ref = HEAP[refId] as #__class_name__#
internal class RefTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/java/ref.stmt.java.tmpl").readText()

    fun kotlinRef(): String {
        return if (method.isStatic)
            ""
        else
            tmpl.replace("#__class_name__#", method.className)

    }
}