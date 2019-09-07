package me.yohom.fluttify.common.tmpl.objc.plugin.handler.arg

import me.yohom.fluttify.common.model.Variable

//// 引用参数
//let #__arg_name__# = REF_MAP[args["#__arg_name__#"] as Int] as! #__type_name__#
internal class ArgRefTmpl(private val variable: Variable) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/plugin/handler/arg/arg_ref.stmt.m.tmpl").readText()

    fun objcArgRef(): String {
        return tmpl
            .replace("#__type_name__#", if (variable.isList) "List<${variable.typeName}>" else variable.typeName)
            .replace("#__arg_name__#", variable.name)
    }
}