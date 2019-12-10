package me.yohom.fluttify.tmpl.objc.common.handler.common.ref.ref_ref

import me.yohom.fluttify.extensions.enpointer
import me.yohom.fluttify.extensions.findType
import me.yohom.fluttify.model.Method

//#__type_name__# ref = (#__type_name__#) HEAP[(NSNumber*) args[@"refId"]];
internal class RefRefTmpl(private val method: Method) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/ref_ref.stmt.m.tmpl").readText()

    fun objcRefRef(): String {
        val empty = ""
        val protocol = "id<${method.className}>"
        val `class` = method.className.enpointer()

        return when {
            method.isStatic -> empty
            method.className.findType().isInterface() -> tmpl.replace("#__type_name__#", protocol)
            else -> tmpl.replace("#__type_name__#", `class`)
        }
    }
}