package me.yohom.fluttify.tmpl.kotlin.common.handler.common.arg

import me.yohom.fluttify.model.Variable

//// 枚举参数
//val #__arg_name__# = #__type_name__#.values()[args["#__arg_name__#"] as Int]
internal class ArgEnumTmpl(private val variable: Variable) {
    private val tmpl = this::class.java.getResource("/tmpl/kotlin/arg_enum.stmt.kt.tmpl").readText()

    fun kotlinArgEnum(): String {
        return tmpl
            .replace("#__type_name__#", variable.typeName)
            .replace("#__arg_name__#", variable.name)
    }
}