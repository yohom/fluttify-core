package me.yohom.fluttify.common.tmpl.dart.enumeration

import me.yohom.fluttify.common.extensions.replaceParagraph
import me.yohom.fluttify.common.extensions.toDartType
import me.yohom.fluttify.common.model.Type

//enum #__enum_name__# {
//  #__enum_constant__#
//}
/**
 * 生成普通类的dart接口
 */
class EnumTmpl(private val type: Type) {
    private val tmpl = this::class.java.getResource("/tmpl/dart/enum.dart.tmpl").readText()

    fun dartEnum(): String {
        return tmpl
            .replace("#__enum_name__#", type.name.toDartType())
            .replaceParagraph("#__enum_constant__#", type.constants.joinToString(",\n"))
    }
}