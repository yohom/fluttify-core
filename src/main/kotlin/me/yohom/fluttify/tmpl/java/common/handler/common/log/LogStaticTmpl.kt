package me.yohom.fluttify.tmpl.java.common.handler.common.log

import me.yohom.fluttify.model.Method

//println("fluttify-kotlin: #__class_name__#::#__method_name__#(#__args__#)")
@Suppress("DuplicatedCode")
internal class LogStaticTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/java/log_static.stmt.java.tmpl").readText()

    fun kotlinLogStatic(): String {
        val className = method.className
        val methodName = method.name
        val args = method
            .formalParams
            .filterNot { it.variable.isCallback() }
            .filterNot { it.variable.isLambda() }.joinToString(" + ") { it.variable.name }
        return tmpl
            .replace("#__class_name__#", className)
            .replace("#__method_name__#", methodName)
            .replace("#__args__#", if (args.isEmpty()) "\"\"" else args)

    }
}