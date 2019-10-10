package me.yohom.fluttify.tmpl.objc.common.callback_method.callback_arg.callback_arg_enum

import me.yohom.fluttify.model.Parameter

//NSNumber* arg#__arg_name__# = @((NSInteger) #__arg_name__#);
internal class CallbackArgEnumTmpl(private val param: Parameter) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/callback_arg_enum.stmt.m.tmpl").readText()

    fun objcCallbackArgEnum(): String {
        return tmpl
            .replace("#__arg_name__#", param.variable.name)
    }
}