package me.yohom.fluttify.common.tmpl.kotlin.plugin

import me.yohom.fluttify.common.extensions.toDartType
import me.yohom.fluttify.common.model.Method

//"#__class_name__#::#__method_name__#" to ::#__handler__#
class MethodBranchTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/kotlin/branch.stmt.kt.tmpl").readText()

    fun kotlinMethodBranch(): String {
        val className = method.className
        val methodName = method.name
        val handler = "handle${method.className.toDartType()}_${method.name}"

        return tmpl.replace("#__class_name__#", className)
            .replace("#__method_name__#", methodName)
            .replace("#__handler__#", handler)
    }
}