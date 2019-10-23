package me.yohom.fluttify.tmpl.objc.common.callback.common.callback_arg.callback_arg_ctype

import me.yohom.fluttify.extensions.depointer
import me.yohom.fluttify.model.Variable

//// primitive回调参数
//NSNumber* arg#__arg_name__# = @(#__arg_name__#);
internal class CallbackArgCTypeTmpl(private val variable: Variable) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/callback_arg_ctype.stmt.m.tmpl").readText()

    fun objcCallbackArgCType(): String {
        return tmpl
            .replace("#__arg_name__#", variable.name.depointer())
    }
}