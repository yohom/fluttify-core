package me.yohom.fluttify.tmpl.java.common.handler.common.invoke.common.callback.callback_method.callback_arg.callback_arg_list

import me.yohom.fluttify.model.Parameter

//List<Integer> arg#__arg_name__# = new ArrayList<>();
//for (#__type_name__# item : #__arg_name__#) {
//    getHEAP().put(item.hashCode(), item);
//    arg#__arg_name__#.add(item.hashCode());
//}
internal class CallbackArgListTmpl(private val param: Parameter) {
    private val tmpl = this::class.java.getResource("/tmpl/java/callback_arg_list.stmt.java.tmpl").readText()

    fun javaCallbackArgList(): String {
        return tmpl
            .replace("#__arg_name__#", param.variable.name)
            .replace("#__type_name__#", param.variable.typeName)

    }
}