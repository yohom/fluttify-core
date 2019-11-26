package me.yohom.fluttify.tmpl.java.common.handler.common.invoke.common.callback.callback_method.callback_arg.callback_arg_ref

import me.yohom.fluttify.model.Parameter

//int arg#__arg_name__# = #__arg_name__#.hashCode();
//getHEAP().put(arg#__arg_name__#, #__arg_name__#);
internal class CallbackArgRefTmpl(private val param: Parameter) {
    private val tmpl = this::class.java.getResource("/tmpl/java/callback_arg_ref.stmt.java.tmpl").readText()

    fun javaCallbackArgRef(): String {
        return tmpl
            .replace("#__arg_name__#", param.variable.name)
    }
}