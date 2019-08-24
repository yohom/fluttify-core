package me.yohom.fluttify.common.tmpl.dart.clazz

import me.yohom.fluttify.common.extensions.depointer
import me.yohom.fluttify.common.extensions.toDartType
import me.yohom.fluttify.common.model.Field

//Future<void> set_#__name__#(#__type__# #__name__#) async {
//  await _channel.invokeMethod('#__setter_method__#', {'refId': refId, "#__name__#": #__name__#});
//}
/**
 * 生成普通类的dart接口
 */
class SetterTmpl(private val field: Field) {
    private val tmpl = this::class.java.getResource("/tmpl/dart/setter.mtd.dart.tmpl").readText()

    fun dartSetter(): String {
        return field.variable.run {
            tmpl
                .replace("#__type__#", typeName.toDartType())
                .replace("#__name__#", name.depointer())
                .replace("#__setter_method__#", field.setterMethodName())
        }
    }
}