package me.yohom.fluttify.common.tmpl.objc.common.handler.arg

import me.yohom.fluttify.common.SYSTEM_TYPEDEF
import me.yohom.fluttify.common.extensions.depointer
import me.yohom.fluttify.common.extensions.enpointer
import me.yohom.fluttify.common.extensions.isObjcValueType
import me.yohom.fluttify.common.extensions.toObjcType
import me.yohom.fluttify.common.model.Variable

//// jsonable参数
//#__type_name__# #__arg_name__# = #__right_value__#;
internal class ArgJsonableTmpl(private val variable: Variable) {
    private val tmpl = this::class.java.getResource("/tmpl/objc/plugin/handler/arg/arg_jsonable.stmt.m.tmpl").readText()

    fun objcArgJsonable(): String {
        val typeName = when {
            variable.isList -> "List<${variable.typeName}>"
            variable.typeName.isObjcValueType() -> variable.typeName.depointer().toObjcType()
            else -> variable.typeName.enpointer()
        }
        val rightValue = if (variable.typeName.isObjcValueType()) {
            var methodPrefix = SYSTEM_TYPEDEF[variable.typeName] ?: variable
                .typeName
                .depointer()
                .toLowerCase()
                .removePrefix("ns")
                .removePrefix("cg")
            if (variable.typeName == "NSUInteger") {
                methodPrefix = "unsignedInteger"
            }
            "[args[@\"${variable.name.depointer()}\"] ${methodPrefix}Value]"
        } else {
            // 理论上, 这里目前应该只有NSString会走到这里
            "(${variable.typeName.enpointer()}) args[@\"${variable.name.depointer()}\"]"
        }
        val argName = variable.name.depointer()

        return tmpl
            .replace("#__type_name__#", typeName)
            .replace("#__right_value__#", rightValue)
            .replace("#__arg_name__#", argName)
    }
}