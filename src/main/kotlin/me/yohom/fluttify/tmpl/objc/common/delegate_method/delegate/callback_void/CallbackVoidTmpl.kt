package me.yohom.fluttify.tmpl.objc.common.delegate_method.delegate.callback_void

import me.yohom.fluttify.extensions.findType
import me.yohom.fluttify.model.Method
import me.yohom.fluttify.tmpl.objc.common.nsvalue_struct.struct_to_nsvalue.StructToNSValueTmpl

//[channel invokeMethod:@"#__callback_method__#" arguments:@{#__callback_args__#}];
internal class CallbackVoidTmpl(private val method: Method) {
    private val tmpl =
        this::class.java.getResource("/tmpl/objc/callback_void.stmt.m.tmpl").readText()

    fun objcCallbackVoid(): String {
        return tmpl
            .replace(
                "#__callback_method__#",
                "Callback::${method.className}::${method.name}${method.formalParams.joinToString("") { it.named }.capitalize()}"
            )
            .replace(
                "#__callback_args__#",
                method.formalParams.joinToString {
                    if (it.variable.typeName.findType().isStruct()) {
                        "@\"${it.variable.name}\": @(${StructToNSValueTmpl(
                            it.variable
                        ).objcStructToNSValue()}.hash)"
                    } else if (it.variable.typeName.findType().isRefType()){
                        "@\"${it.variable.name}\": @(${it.variable.name}.hash)"
                    } else {
                        "@\"${it.variable.name}\": @(${it.variable.name})"
                    }
                }
            )
    }
}